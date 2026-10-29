package vn.lifeos.app.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import vn.lifeos.data.planner.ReminderEntity

internal const val REMINDER_CHANNEL = "agent_life_reminders"
internal const val ACTION_FIRE = "vn.lifeos.app.reminders.FIRE"
internal const val ACTION_SNOOZE = "vn.lifeos.app.reminders.SNOOZE"
internal const val ACTION_COMPLETE = "vn.lifeos.app.reminders.COMPLETE"
internal const val EXTRA_REMINDER_ID = "reminder_id"
internal const val EXTRA_TRIGGER = "trigger_at"

internal fun notificationAllowed(context: Context): Boolean {
    val manager = context.getSystemService(NotificationManager::class.java)
    return (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        manager.areNotificationsEnabled()
}

internal class ReminderAlarms(private val context: Context) {
    private val manager = context.getSystemService(AlarmManager::class.java)

    fun exactAllowed(): Boolean = Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms()

    fun schedule(row: ReminderEntity) {
        val pending = alarmIntent(row.id, row.triggerAt)
        manager.cancel(pending)
        if (exactAllowed()) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, row.triggerAt, pending)
                return
            } catch (_: SecurityException) { /* Access may have changed between check and set. */ }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, row.triggerAt, pending)
    }

    fun cancel(id: String) {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("agentlife://alarm/$id")
        }
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            manager.cancel(it)
            it.cancel()
        }
    }

    private fun alarmIntent(id: String, trigger: Long): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ACTION_FIRE
            data = Uri.parse("agentlife://alarm/$id")
            putExtra(EXTRA_REMINDER_ID, id)
            putExtra(EXTRA_TRIGGER, trigger)
        }
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

internal fun createReminderChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "Nhắc việc và lịch", NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Lời nhắc công việc và sự kiện đã lưu trên thiết bị"
        lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
    })
}
