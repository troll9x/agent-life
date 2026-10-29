package vn.lifeos.app

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.data.planner.RoomPlannerRepository
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.domain.planner.CreateTask
import vn.lifeos.domain.planner.TaskState
import vn.lifeos.domain.planner.UpdateTask
import vn.lifeos.app.reminders.ACTION_FIRE
import vn.lifeos.app.reminders.ReminderActionReceiver

@RunWith(AndroidJUnit4::class)
class ReminderEngineTest {
    @Test fun appReconcilesAlarmAfterEditAndCompletion() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as AgentLifeApplication
        val access = app.databaseProvider.open() as DatabaseAccess.Ready
        val database = access.database
        val profileStore = app.appContainer.profileStore(database)
        if ((profileStore.observe().first() as Outcome.Success).value == null) {
            assertTrue(profileStore.complete(ProfileInput("Fixture Reminder", ZoneId.systemDefault().id, "VND", 1)) is Outcome.Success)
        }
        val profile = (profileStore.observe().first() as Outcome.Success).value!!.profile
        val planner = RoomPlannerRepository(database, app.appContainer.clock)
        val device = planner.workspaceDeviceId(profile.workspaceId)!!
        val id = RandomIdGenerator.newId()
        val now = app.appContainer.clock.instant()
        val firstDue = now.plusSeconds(3600)
        val secondDue = now.plusSeconds(7200)
        val task = (planner.createTask(CreateTask(id, profile.workspaceId, device, null, "Fixture reminder", dueAt = firstDue)) as Outcome.Success).value
        val engine = app.appContainer.reminderEngine(database)
        assertTrue(engine.reconcile(profile.workspaceId, ZoneId.of(profile.zoneId)))
        val first = database.reminderDao().all(profile.workspaceId.value).single { it.targetId == id.value && it.slot == "DUE" }
        assertEquals(firstDue.toEpochMilli(), first.triggerAt)
        val alarmIntent = Intent(app, ReminderActionReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("agentlife://alarm/${first.id}")
        }
        assertNotNull(PendingIntent.getBroadcast(app, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
        val edited = (planner.updateTask(UpdateTask(profile.workspaceId, id, task.version, null, task.title, null, 3, secondDue, null)) as Outcome.Success).value
        assertTrue(engine.reconcile(profile.workspaceId, ZoneId.of(profile.zoneId)))
        val second = database.reminderDao().byId(first.id)!!
        assertEquals(first.id, second.id)
        assertEquals(secondDue.toEpochMilli(), second.triggerAt)
        assertTrue(planner.updateTaskState(profile.workspaceId, id, edited.version, TaskState.DONE) is Outcome.Success)
        assertTrue(engine.reconcile(profile.workspaceId, ZoneId.of(profile.zoneId)))
        assertEquals("CANCELLED", database.reminderDao().byId(first.id)?.state)
        assertNull(PendingIntent.getBroadcast(app, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
    }
}
