package vn.lifeos.app.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.ZoneId
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import vn.lifeos.app.AgentLifeApplication
import vn.lifeos.app.MainActivity
import vn.lifeos.app.R
import vn.lifeos.data.planner.ReminderEntity
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.domain.foundation.Outcome

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val action = intent.action?.takeIf { it in setOf(ACTION_FIRE, ACTION_SNOOZE, ACTION_COMPLETE) } ?: return
        val trigger = intent.getLongExtra(EXTRA_TRIGGER, -1)
        val input = Data.Builder().putString(EXTRA_REMINDER_ID, id).putString("action", action)
            .putLong(EXTRA_TRIGGER, trigger).build()
        WorkManager.getInstance(context).enqueueUniqueWork("reminder:$action:$id:$trigger", ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<ReminderActionWorker>().setInputData(input).build())
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_USER_UNLOCKED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED,
                android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
            ReminderWorkSchedule.enqueue(context)
        }
    }
}

internal object ReminderWorkSchedule {
    fun register(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("reminder-horizon", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderRescheduleWorker>(1, TimeUnit.DAYS).build())
        enqueue(context)
    }
    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork("reminder-reschedule", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderRescheduleWorker>().build())
    }
}

class ReminderRescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as AgentLifeApplication
        val access = app.databaseProvider.open()
        if (access is DatabaseAccess.UnlockDeviceFirst) return Result.retry()
        val database = (access as? DatabaseAccess.Ready)?.database ?: return Result.failure()
        val profileStore = app.appContainer.profileStore(database)
        val profile = (profileStore.observe().first() as? Outcome.Success)?.value?.profile
            ?: return Result.success()
        TimeZone.setDefault(null)
        val zone = ZoneId.systemDefault()
        if (profile.zoneId != zone.id) {
            val updated = profileStore.update(profile, ProfileInput(profile.displayName, zone.id, profile.baseCurrency, profile.budgetStartDay))
            if (updated !is Outcome.Success) return Result.retry()
        }
        return if (app.appContainer.reminderEngine(database).reconcile(profile.workspaceId, zone)) Result.success()
            else Result.retry()
    }
}

class ReminderActionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(EXTRA_REMINDER_ID) ?: return Result.failure()
        val action = inputData.getString("action") ?: return Result.failure()
        val app = applicationContext as AgentLifeApplication
        val access = app.databaseProvider.open()
        if (access is DatabaseAccess.UnlockDeviceFirst) return Result.retry()
        val database = (access as? DatabaseAccess.Ready)?.database ?: return Result.failure()
        val engine = app.appContainer.reminderEngine(database)
        val store = engine.store()
        val now = app.appContainer.clock.instant()
        when (action) {
            ACTION_FIRE -> {
                val row = store.claim(id, inputData.getLong(EXTRA_TRIGGER, -1), now) ?: return Result.success()
                if (notificationAllowed(applicationContext)) {
                    val title = store.title(row) ?: return Result.success()
                    showReminder(applicationContext, row, title)
                }
            }
            ACTION_SNOOZE -> {
                store.snooze(id, now)?.let(engine.alarms()::schedule)
                applicationContext.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
            }
            ACTION_COMPLETE -> {
                val result = store.complete(id, now) ?: return Result.success()
                result.cancelledIds.forEach(engine.alarms()::cancel)
                applicationContext.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
            }
            else -> return Result.failure()
        }
        return Result.success()
    }
}

private fun showReminder(context: Context, row: ReminderEntity, title: String) {
    val open = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse("agentlife://reminder/${row.id}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra("reminder_target_type", row.targetType)
        putExtra("reminder_target_id", row.targetId)
        putExtra("reminder_occurrence_key", row.occurrenceKey)
        putExtra("reminder_trigger_at", row.triggerAt)
    }
    val content = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val public = NotificationCompat.Builder(context, REMINDER_CHANNEL).setSmallIcon(R.drawable.ic_today)
        .setContentTitle("Agent Life").setContentText("Có lời nhắc").build()
    val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL).setSmallIcon(R.drawable.ic_today)
        .setContentTitle(title).setContentText(if (row.targetType == "TASK") "Đến hạn công việc" else "Sắp tới lịch hẹn")
        .setContentIntent(content).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPublicVersion(public).setCategory(NotificationCompat.CATEGORY_REMINDER)
    if (row.slot == "DUE") builder.addAction(0, "Hoãn 10 phút", actionIntent(context, row, ACTION_SNOOZE))
    if (row.targetType == "TASK") builder.addAction(0, "Hoàn tất", actionIntent(context, row, ACTION_COMPLETE))
    context.getSystemService(NotificationManager::class.java).notify(row.id.hashCode(), builder.build())
}

private fun actionIntent(context: Context, row: ReminderEntity, action: String): PendingIntent {
    val intent = Intent(context, ReminderActionReceiver::class.java).apply {
        this.action = action
        data = Uri.parse("agentlife://reminder-action/$action/${row.id}")
        putExtra(EXTRA_REMINDER_ID, row.id)
        putExtra(EXTRA_TRIGGER, row.triggerAt)
    }
    return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}
