package vn.lifeos.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.ZoneId
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    private val reminderDestination = mutableStateOf<ReminderDestination?>(null)
    private val timeZoneReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            shellViewModel.syncDeviceTimeZone(currentDeviceZoneId())
        }
    }
    private val shellViewModel: ShellViewModel by viewModels {
        ShellViewModel.factory((application as AgentLifeApplication).appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderDestination.value = intent?.reminderDestination()
        ContextCompat.registerReceiver(this, timeZoneReceiver, IntentFilter(Intent.ACTION_TIMEZONE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent {
            val state by shellViewModel.state.collectAsStateWithLifecycle()
            val profileZone = (state.profile as? ProfileStatus.Ready)?.snapshot?.profile?.zoneId
            LaunchedEffect(profileZone) {
                if (profileZone != null) shellViewModel.syncDeviceTimeZone(currentDeviceZoneId())
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !state.darkTheme
                    isAppearanceLightNavigationBars = !state.darkTheme
                }
            }
            AgentLifeTheme(darkTheme = state.darkTheme) {
                AgentLifeScreen(
                    state = state,
                    onRetry = shellViewModel::retry,
                    onThemeChange = shellViewModel::setDarkTheme,
                    onCompleteOnboarding = shellViewModel::completeOnboarding,
                    onUpdateProfile = shellViewModel::updateProfile,
                    plannerActions = shellViewModel,
                    reminderDestination = reminderDestination.value,
                    onRetryReminders = { shellViewModel.refreshReminders(force = true) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shellViewModel.syncDeviceTimeZone(currentDeviceZoneId())
        shellViewModel.refreshReminders(force = true)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reminderDestination.value = intent.reminderDestination()
    }

    override fun onDestroy() {
        unregisterReceiver(timeZoneReceiver)
        super.onDestroy()
    }

    private fun currentDeviceZoneId(): String {
        TimeZone.setDefault(null)
        return ZoneId.systemDefault().id
    }
}

internal data class ReminderDestination(val type: String, val id: String, val occurrenceKey: String, val triggerAt: Long)

private fun Intent.reminderDestination(): ReminderDestination? {
    val type = getStringExtra("reminder_target_type")?.takeIf { it == "TASK" || it == "EVENT" } ?: return null
    val id = getStringExtra("reminder_target_id")?.takeIf { it.isNotBlank() } ?: return null
    val occurrenceKey = getStringExtra("reminder_occurrence_key") ?: id
    return ReminderDestination(type, id, occurrenceKey, getLongExtra("reminder_trigger_at", -1))
}
