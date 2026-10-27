package vn.lifeos.data.planner

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.AppError
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.RegisterConnection
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.domain.planner.*

@RunWith(AndroidJUnit4::class)
class RoomPlannerRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val now = Instant.parse("2026-10-20T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private fun id(suffix: Int) = Uuid.parse("00000000-0000-4000-8000-${suffix.toString().padStart(12, '0')}")
    private val w = id(101)
    private val d = id(103)

    private suspend fun fixtureDb(name: String): AgentLifeDatabase {
        context.deleteDatabase(name)
        val db = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        val result = RoomFoundationRepository(db, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(w, id(102), d, id(104), "Personal", "Fixture User", "Asia/Ho_Chi_Minh", "VND", 1, "ANDROID", "test"))
        assertTrue(result is Outcome.Success)
        return db
    }

    @Test fun storesOfflineGraphAndQueriesDueAndCompletedTasks() = runBlocking {
        val db = fixtureDb("planner-graph.db")
        val repo = RoomPlannerRepository(db, clock)
        val project = repo.createProject(CreateProject(id(201), w, d, "Fixture Project"))
        assertTrue(project is Outcome.Success)
        val undated = repo.createTask(CreateTask(id(202), w, d, id(201), "No deadline"))
        val due = repo.createTask(CreateTask(id(203), w, d, id(201), "Due", dueAt = now.minusSeconds(60), recurrence = "DAILY"))
        assertTrue(undated is Outcome.Success && due is Outcome.Success)
        assertTrue(repo.addChecklistItem(CreateChecklistItem(id(204), w, d, id(203), "Step", 0)) is Outcome.Success)
        assertEquals(1, db.plannerDao().checklist(w.value, id(203).value).size)
        assertTrue(repo.setChecklistDone(w, id(204), 1, true) is Outcome.Success)
        assertTrue(db.plannerDao().checklist(w.value, id(203).value).single().done)
        val key = occurrenceKey(id(203), LocalDate.parse("2026-10-19"))
        assertTrue(repo.addRecurrenceException(CreateRecurrenceException(id(205), w, d, id(203), key, cancelled = true)) is Outcome.Success)
        assertTrue(repo.scheduleReminder(CreateReminder(id(206), w, d, ReminderTarget.TASK, id(203), key, "default", now, "fixture-request")) is Outcome.Success)
        assertEquals(AppError.Conflict, (repo.scheduleReminder(CreateReminder(id(207), w, d, ReminderTarget.TASK, id(203), key, "default", now, "another-request")) as Outcome.Failure).error)
        assertEquals(listOf(id(203)), (repo.tasksDueBefore(w, now) as Outcome.Success).value.map { it.id })
        val archived = repo.archiveProject(w, id(201), 1)
        assertEquals(2L, (archived as Outcome.Success).value.version)
        assertEquals(2, (repo.observeTasks(w).first() as Outcome.Success).value.size)
        val completed = repo.updateTaskState(w, id(203), 1, TaskState.DONE)
        assertTrue(completed is Outcome.Success)
        assertEquals(2L, (completed as Outcome.Success).value.version)
        assertTrue((repo.tasksDueBefore(w, now) as Outcome.Success).value.isEmpty())
        assertEquals(2, (repo.observeTasks(w).first() as Outcome.Success).value.size)
        assertTrue(repo.deleteTask(w, id(203), 2) is Outcome.Success)
        assertTrue(db.plannerDao().checklist(w.value, id(203).value).isEmpty())
        assertEquals("CANCELLED", db.plannerDao().remindersForTask(w.value, id(203).value).single().state)
        db.close()
        val reopened = Room.databaseBuilder(context, AgentLifeDatabase::class.java, "planner-graph.db").build()
        assertEquals(listOf(id(202)), (RoomPlannerRepository(reopened, clock).observeTasks(w).first() as Outcome.Success).value.map { it.id })
        reopened.close()
    }

    @Test fun allDayDateStaysTheSameAcrossZonesAndInvalidRangesAreRejected() = runBlocking {
        val db = fixtureDb("planner-calendar.db")
        val repo = RoomPlannerRepository(db, clock)
        val date = LocalDate.parse("2026-10-19")
        val created = repo.createEvent(CreateCalendarEvent(id(301), w, d, "All day", EventTimeKind.ALL_DAY,
            ZoneId.of("Asia/Ho_Chi_Minh"), startDate = date, endDateExclusive = date.plusDays(1)))
        assertTrue(created is Outcome.Success)
        for (zone in listOf("Asia/Ho_Chi_Minh", "America/Los_Angeles", "Pacific/Auckland")) {
            val events = (repo.eventsOnDate(w, date, ZoneId.of(zone)) as Outcome.Success).value
            assertEquals(1, events.size)
            assertEquals(date, events.single().startDate)
            assertEquals(date.plusDays(1), events.single().endDateExclusive)
            assertEquals(null, events.single().startAt)
            assertTrue((repo.eventsOnDate(w, date.minusDays(1), ZoneId.of(zone)) as Outcome.Success).value.isEmpty())
        }
        val invalid = repo.createEvent(CreateCalendarEvent(id(302), w, d, "Invalid", EventTimeKind.ALL_DAY,
            ZoneId.of("UTC"), startDate = date, endDateExclusive = date))
        assertEquals(AppError.InvalidInput("eventTime"), (invalid as Outcome.Failure).error)
        val timed = repo.createEvent(CreateCalendarEvent(id(304), w, d, "Timed", EventTimeKind.TIMED,
            ZoneId.of("Asia/Ho_Chi_Minh"), startAt = Instant.parse("2026-10-19T16:30:00Z"),
            endAt = Instant.parse("2026-10-19T17:30:00Z")))
        assertTrue(timed is Outcome.Success)
        assertEquals(2, (repo.eventsOnDate(w, date, ZoneId.of("Asia/Ho_Chi_Minh")) as Outcome.Success).value.size)
        assertEquals(1, (repo.eventsOnDate(w, date.plusDays(1), ZoneId.of("Asia/Ho_Chi_Minh")) as Outcome.Success).value.size)
        assertEquals(AppError.NotFound, (repo.createTask(CreateTask(id(303), w, d, id(999), "Wrong project")) as Outcome.Failure).error)
        assertEquals(AppError.NotFound, (repo.addChecklistItem(CreateChecklistItem(id(305), id(999), d, id(301), "Cross workspace", 0)) as Outcome.Failure).error)
        db.close()
    }

    @Test fun editTaskAndOneOccurrencePersistAtomicallyWithoutDuplicates() = runBlocking {
        val db = fixtureDb("planner-edit.db")
        val repo = RoomPlannerRepository(db, clock)
        val task = repo.createTask(CreateTask(id(401), w, d, null, "Draft")) as Outcome.Success
        val edited = repo.updateTask(UpdateTask(w, task.value.id, task.value.version, null, "Edited", null, 2,
            now.plusSeconds(3600), null)) as Outcome.Success
        assertEquals("Edited", edited.value.title)
        assertEquals(2L, edited.value.version)
        val start = LocalDate.parse("2026-10-19")
        val series = repo.createEvent(CreateCalendarEvent(id(402), w, d, "Series", EventTimeKind.ALL_DAY,
            ZoneId.of("Asia/Ho_Chi_Minh"), startDate = start, endDateExclusive = start.plusDays(1),
            recurrence = RecurrenceSpec(Frequency.DAILY, count = 3).encode())) as Outcome.Success
        val replacement = CreateCalendarEvent(id(404), w, d, "Changed once", EventTimeKind.ALL_DAY,
            ZoneId.of("Asia/Ho_Chi_Minh"), startDate = start.plusDays(1), endDateExclusive = start.plusDays(2))
        val command = ReplaceOccurrence(w, d, id(403), series.value.id, start.plusDays(1), replacement)
        assertTrue(repo.replaceOccurrence(command) is Outcome.Success)
        assertEquals(AppError.Conflict, (repo.replaceOccurrence(command.copy(replacement = replacement.copy(id = id(405)))) as Outcome.Failure).error)
        assertEquals(2, (repo.observeEvents(w).first() as Outcome.Success).value.size)
        assertEquals(1, (repo.observeExceptions(w).first() as Outcome.Success).value.size)
        db.close()
        val reopened = Room.databaseBuilder(context, AgentLifeDatabase::class.java, "planner-edit.db").build()
        val readBack = RoomPlannerRepository(reopened, clock)
        assertEquals("Changed once", (readBack.observeEvents(w).first() as Outcome.Success).value.single { it.id == id(404) }.title)
        assertEquals("Edited", (readBack.observeTasks(w).first() as Outcome.Success).value.single().title)
        reopened.close()
    }

    @Test fun remoteEventCannotBeEditedLocally() = runBlocking {
        val db = fixtureDb("planner-readonly.db")
        val foundation = RoomFoundationRepository(db, clock, RandomIdGenerator)
        assertTrue(foundation.registerConnection(RegisterConnection(id(501), w, id(502), d,
            "CALENDAR", "fixture-account", null, setOf("read"), 1, "CONNECTED")) is Outcome.Success)
        val repo = RoomPlannerRepository(db, clock)
        val day = LocalDate.parse("2026-10-19")
        val remote = repo.createEvent(CreateCalendarEvent(id(503), w, d, "Provider", EventTimeKind.ALL_DAY,
            ZoneId.of("UTC"), startDate = day, endDateExclusive = day.plusDays(1),
            sourceConnectionId = id(501), externalId = "fixture-event", readonly = true)) as Outcome.Success
        val attempt = repo.updateEvent(UpdateCalendarEvent(w, remote.value.id, 1, "Changed", EventTimeKind.ALL_DAY,
            ZoneId.of("UTC"), startDate = day, endDateExclusive = day.plusDays(1)))
        assertEquals(AppError.Conflict, (attempt as Outcome.Failure).error)
        db.close()
    }
}
