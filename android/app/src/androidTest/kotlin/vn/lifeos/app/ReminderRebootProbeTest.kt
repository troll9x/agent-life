package vn.lifeos.app

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.ZoneId
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import vn.lifeos.app.reminders.ACTION_FIRE
import vn.lifeos.app.reminders.ACTION_SNOOZE
import vn.lifeos.app.reminders.ACTION_COMPLETE
import vn.lifeos.app.reminders.EXTRA_REMINDER_ID
import vn.lifeos.app.reminders.EXTRA_TRIGGER
import vn.lifeos.app.reminders.ReminderActionReceiver
import vn.lifeos.data.planner.RoomPlannerRepository
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.domain.common.RandomIdGenerator
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.domain.planner.CreateTask
import vn.lifeos.domain.planner.TaskState
import vn.lifeos.domain.planner.Frequency
import vn.lifeos.domain.planner.RecurrenceSpec

/** Run prepare and verify explicitly around an AVD reboot; default suite skips these probes. */
@RunWith(AndroidJUnit4::class)
class ReminderRebootProbeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as AgentLifeApplication
    private val preferences = app.getSharedPreferences("reminder_reboot_probe", Context.MODE_PRIVATE)

    @Test fun prepare() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("phase") == "prepare")
        val database = (app.databaseProvider.open() as DatabaseAccess.Ready).database
        val store = app.appContainer.profileStore(database)
        if ((store.observe().first() as Outcome.Success).value == null) {
            assertTrue(store.complete(ProfileInput("Fixture Reboot", ZoneId.systemDefault().id, "VND", 1)) is Outcome.Success)
        }
        val profile = (store.observe().first() as Outcome.Success).value!!.profile
        val planner = RoomPlannerRepository(database, app.appContainer.clock)
        val taskId = RandomIdGenerator.newId()
        val due = app.appContainer.clock.instant().plusSeconds(7200)
        assertTrue(planner.createTask(CreateTask(taskId, profile.workspaceId, planner.workspaceDeviceId(profile.workspaceId)!!,
            null, "Fixture reboot", dueAt = due)) is Outcome.Success)
        assertTrue(app.appContainer.reminderEngine(database).reconcile(profile.workspaceId, ZoneId.of(profile.zoneId)))
        val reminder = database.reminderDao().all(profile.workspaceId.value).single { it.targetId == taskId.value && it.slot == "DUE" }
        preferences.edit().putString("task", taskId.value).putString("workspace", profile.workspaceId.value)
            .putString("reminder", reminder.id).commit()
        println("REMINDER_REBOOT_ID=${reminder.id}")
        assertNotNull(alarm(reminder.id))
    }

    @Test fun verifyAndCleanUp() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("phase") == "verify")
        val id = preferences.getString("reminder", null) ?: error("Probe was not prepared")
        assertNotNull(alarm(id))
        val database = (app.databaseProvider.open() as DatabaseAccess.Ready).database
        val workspace = Uuid.parse(preferences.getString("workspace", null)!!)
        val taskId = Uuid.parse(preferences.getString("task", null)!!)
        val planner = RoomPlannerRepository(database, app.appContainer.clock)
        val task = (planner.observeTasks(workspace).first() as Outcome.Success).value.single { it.id == taskId }
        assertTrue(planner.updateTaskState(workspace, taskId, task.version, TaskState.DONE) is Outcome.Success)
        val profile = (app.appContainer.profileStore(database).observe().first() as Outcome.Success).value!!.profile
        assertTrue(app.appContainer.reminderEngine(database).reconcile(workspace, ZoneId.of(profile.zoneId)))
        preferences.edit().clear().commit()
        Unit
    }

    @Test fun prepareTimezone() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("phase") == "timezone_prepare")
        val database = (app.databaseProvider.open() as DatabaseAccess.Ready).database
        val profile = (app.appContainer.profileStore(database).observe().first() as Outcome.Success).value!!.profile
        val zone = ZoneId.systemDefault()
        val planner = RoomPlannerRepository(database, app.appContainer.clock)
        val taskId = RandomIdGenerator.newId()
        val due = LocalDate.now(zone).plusDays(1).atTime(9, 0).atZone(zone).toInstant()
        assertTrue(planner.createTask(CreateTask(taskId, profile.workspaceId, planner.workspaceDeviceId(profile.workspaceId)!!,
            null, "Fixture timezone", dueAt = due, recurrence = RecurrenceSpec(Frequency.DAILY, count = 3).encode())) is Outcome.Success)
        assertTrue(app.appContainer.reminderEngine(database).reconcile(profile.workspaceId, zone))
        val first = database.reminderDao().all(profile.workspaceId.value).first { it.targetId == taskId.value && it.state == "PENDING" }
        preferences.edit().putString("zoneTask", taskId.value).putString("zoneWorkspace", profile.workspaceId.value)
            .putString("zoneReminder", first.id).putLong("zoneTrigger", first.triggerAt).commit()
        assertNotNull(alarm(first.id))
        Unit
    }

    @Test fun verifyTimezoneAndCleanUp() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("phase") == "timezone_verify")
        val database = (app.databaseProvider.open() as DatabaseAccess.Ready).database
        val workspace = Uuid.parse(preferences.getString("zoneWorkspace", null)!!)
        val taskId = Uuid.parse(preferences.getString("zoneTask", null)!!)
        val firstId = preferences.getString("zoneReminder", null)!!
        val profile = (app.appContainer.profileStore(database).observe().first() as Outcome.Success).value!!.profile
        assertTrue(profile.zoneId == ZoneId.systemDefault().id)
        val active = database.reminderDao().all(workspace.value).filter { it.targetId == taskId.value && it.state == "PENDING" }
        assertTrue(active.isNotEmpty())
        assertTrue(active.any { it.triggerAt != preferences.getLong("zoneTrigger", -1) })
        assertTrue(active.all { alarm(it.id) != null })
        assertTrue(alarm(firstId) == null || active.any { it.id == firstId })
        val planner = RoomPlannerRepository(database, app.appContainer.clock)
        val task = (planner.observeTasks(workspace).first() as Outcome.Success).value.single { it.id == taskId }
        assertTrue(planner.updateTaskState(workspace, taskId, task.version, TaskState.DONE) is Outcome.Success)
        assertTrue(app.appContainer.reminderEngine(database).reconcile(workspace, ZoneId.of(profile.zoneId)))
        preferences.edit().remove("zoneTask").remove("zoneWorkspace").remove("zoneReminder").remove("zoneTrigger").commit()
        Unit
    }

    @Test fun prepareDelivery() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("phase") == "delivery_prepare")
        val database = (app.databaseProvider.open() as DatabaseAccess.Ready).database
        val profile = (app.appContainer.profileStore(database).observe().first() as Outcome.Success).value!!.profile
        val planner = RoomPlannerRepository(database, app.appContainer.clock)
        val id = RandomIdGenerator.newId()
        assertTrue(planner.createTask(CreateTask(id, profile.workspaceId, planner.workspaceDeviceId(profile.workspaceId)!!,
            null, "Fixture delivery", dueAt = app.appContainer.clock.instant().plusSeconds(25))) is Outcome.Success)
        assertTrue(app.appContainer.reminderEngine(database).reconcile(profile.workspaceId, ZoneId.of(profile.zoneId)))
        val row = database.reminderDao().all(profile.workspaceId.value).single { it.targetId == id.value && it.slot == "DUE" }
        preferences.edit().putString("deliveryReminder", row.id).putString("deliveryTask", id.value)
            .putString("deliveryWorkspace", profile.workspaceId.value).commit()
        assertNotNull(alarm(row.id))
        Unit
    }

    @Test fun verifyDeliveryActionsAndCleanUp() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("phase") == "delivery_verify")
        val database = (app.databaseProvider.open() as DatabaseAccess.Ready).database
        val id = preferences.getString("deliveryReminder", null)!!
        var row = database.reminderDao().byId(id)!!
        repeat(50) {
            if (row.state != "FIRED") { delay(200); row = database.reminderDao().byId(id)!! }
        }
        assertTrue("state=${row.state}, trigger=${row.triggerAt}", row.state == "FIRED")
        val notifications = app.getSystemService(NotificationManager::class.java)
        repeat(50) {
            if (notifications.activeNotifications.none { it.id == id.hashCode() }) delay(200)
        }
        assertTrue(notifications.activeNotifications.any { it.id == id.hashCode() })
        ReminderActionReceiver().onReceive(app, Intent(app, ReminderActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, id)
            putExtra(EXTRA_TRIGGER, row.triggerAt)
        })
        val workspace = Uuid.parse(preferences.getString("deliveryWorkspace", null)!!)
        val taskId = preferences.getString("deliveryTask", null)!!
        var snooze = database.reminderDao().all(workspace.value).firstOrNull { it.targetId == taskId && it.slot == "SNOOZE" }
        repeat(30) {
            if (snooze == null) { delay(200); snooze = database.reminderDao().all(workspace.value).firstOrNull { it.targetId == taskId && it.slot == "SNOOZE" } }
        }
        assertTrue(snooze?.state == "PENDING")
        assertNotNull(alarm(snooze!!.id))
        ReminderActionReceiver().onReceive(app, Intent(app, ReminderActionReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_REMINDER_ID, id)
            putExtra(EXTRA_TRIGGER, row.triggerAt)
        })
        repeat(30) {
            if (database.reminderDao().byId(snooze!!.id)?.state != "CANCELLED") delay(200)
        }
        assertTrue(database.reminderDao().byId(snooze!!.id)?.state == "CANCELLED")
        assertTrue(alarm(snooze!!.id) == null)
        preferences.edit().remove("deliveryReminder").remove("deliveryTask").remove("deliveryWorkspace").commit()
        Unit
    }

    private fun alarm(id: String): PendingIntent? = PendingIntent.getBroadcast(app, 0,
        Intent(app, ReminderActionReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("agentlife://alarm/$id")
        }, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
}
