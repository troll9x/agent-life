package vn.lifeos.app.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import vn.lifeos.app.R

private const val PREFS_NAME = "notification_permission"
private const val PROMPT_HANDLED = "start_prompt_handled"
private const val REQUEST_ATTEMPTED = "request_attempted"

internal fun notificationPromptNeeded(sdk: Int, granted: Boolean, handled: Boolean, attempted: Boolean): Boolean =
    sdk >= 33 && !granted && !handled && !attempted

internal fun notificationPermissionGranted(context: Context): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

internal fun notificationRequestAttempted(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(REQUEST_ATTEMPTED, false)

internal fun markNotificationRequestAttempted(context: Context) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(REQUEST_ATTEMPTED, true).apply()
}

@Composable
internal fun NotificationStartPrompt() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var visible by remember { mutableStateOf(notificationPromptNeeded(Build.VERSION.SDK_INT,
        notificationPermissionGranted(context), preferences.getBoolean(PROMPT_HANDLED, false),
        notificationRequestAttempted(context))) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    if (visible) AlertDialog(
        onDismissRequest = { preferences.edit().putBoolean(PROMPT_HANDLED, true).apply(); visible = false },
        title = { Text(stringResource(R.string.notifications_optional)) },
        text = { Text(stringResource(R.string.notifications_start_explain)) },
        confirmButton = { TextButton(onClick = {
            preferences.edit().putBoolean(PROMPT_HANDLED, true).apply()
            markNotificationRequestAttempted(context)
            visible = false
            request.launch(Manifest.permission.POST_NOTIFICATIONS)
        }) { Text(stringResource(R.string.enable_notifications)) } },
        dismissButton = { TextButton(onClick = {
            preferences.edit().putBoolean(PROMPT_HANDLED, true).apply()
            visible = false
        }) { Text(stringResource(R.string.later)) } },
    )
}
