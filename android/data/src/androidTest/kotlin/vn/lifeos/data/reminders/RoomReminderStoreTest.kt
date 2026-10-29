package vn.lifeos.data.reminders

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.db.AgentLifeDatabase
import vn.lifeos.data.db.RoomFoundationRepository
import vn.lifeos.data.planner.RoomPlannerRepository
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.CreatePersonalWorkspace
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.domain.planner.CreateTask
import vn.lifeos.domain.planner.ReminderTarget
import vn.lifeos.domain.planner.UpdateTask

@RunWith(AndroidJUnit4::class)
class RoomReminderStoreTest {
    private fun id(n: Int) = Uuid.parse("00000000-0000-4000-8000-${n.toString().padStart(12, '0')}")

    @Test fun editingDueTimeReplacesPendingAndCompletionIsIdempotent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "reminder-transitions.db"
        context.deleteDatabase(name)
        val database = Room.databaseBuilder(context, AgentLifeDatabase::class.java, name).build()
        val now = Instant.parse("2026-10-20T00:00:00Z")
        val clock = Clock.fixed(now, ZoneOffset.UTC)
        val workspace = id(901)
        val device = id(903)
        assertTrue(RoomFoundationRepository(database, clock, RandomIdGenerator).createPersonalWorkspace(
            CreatePersonalWorkspace(workspace, id(902), device, id(904), "Personal", "Fixture", "UTC", "VND", 1, "ANDROID", "test")) is Outcome.Success)
        val planner = RoomPlannerRepository(database, clock)
        val dueNine = now.plusSeconds(9 * 3600)
        val dueTen = now.plusSeconds(10 * 3600)
        val task = (planner.createTask(CreateTask(id(905), workspace, device, null, "Fixture task", dueAt = dueNine)) as Outcome.Success).value
        val store = RoomReminderStore(database)
        val nine = store.reconcile(workspace, listOf(ReminderCandidate(ReminderTarget.TASK, task.id, task.id.value, dueNine)), now)
        assertEquals(1, nine.schedule.size)
        val reminderId = nine.schedule.single().id
        val updated = planner.updateTask(UpdateTask(workspace, task.id, task.version, null, task.title, null, 3, dueTen, null))
        assertTrue(updated is Outcome.Success)
        val ten = store.reconcile(workspace, listOf(ReminderCandidate(ReminderTarget.TASK, task.id, task.id.value, dueTen)), now)
        assertEquals(reminderId, ten.schedule.single().id)
        assertEquals(dueTen.toEpochMilli(), ten.schedule.single().triggerAt)
        assertNull(store.claim(reminderId, dueNine.toEpochMilli(), dueNine))
        assertNotNull(store.claim(reminderId, dueTen.toEpochMilli(), dueTen))
        val snooze = store.snooze(reminderId, dueTen)!!
        assertEquals(snooze.id, store.snooze(reminderId, dueTen.plusSeconds(1))?.id)
        assertEquals(dueTen.plusSeconds(600).toEpochMilli(), snooze.triggerAt)
        assertNotNull(store.complete(reminderId, dueTen.plusSeconds(2)))
        assertNotNull(store.complete(reminderId, dueTen.plusSeconds(3)))
        assertEquals("DONE", database.reminderDao().task(workspace.value, task.id.value)?.state)
        assertEquals(3L, database.reminderDao().task(workspace.value, task.id.value)?.version)
        assertEquals("CANCELLED", database.reminderDao().byId(snooze.id)?.state)
        assertNull(store.claim(snooze.id, snooze.triggerAt, dueTen.plusSeconds(600)))
        database.close()
    }
}
