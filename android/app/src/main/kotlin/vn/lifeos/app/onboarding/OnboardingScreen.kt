package vn.lifeos.app.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import vn.lifeos.app.R
import vn.lifeos.data.profile.ProfileInput

@Composable
internal fun OnboardingScreen(saving: Boolean, failed: Boolean, onComplete: (ProfileInput) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var zone by rememberSaveable { mutableStateOf("Asia/Ho_Chi_Minh") }
    var currency by rememberSaveable { mutableStateOf("VND") }
    var startDay by rememberSaveable { mutableStateOf("1") }
    val input = parseInput(name, zone, currency, startDay)
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.onboarding_intro), style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.display_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(zone, { zone = it }, label = { Text(stringResource(R.string.time_zone)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text(stringResource(R.string.currency)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            startDay, { startDay = it.filter(Char::isDigit).take(2) },
            label = { Text(stringResource(R.string.budget_start_day)) },
            supportingText = { Text(stringResource(R.string.day_range)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        NotificationPermissionOption()
        if (failed) Text(stringResource(R.string.save_error), color = MaterialTheme.colorScheme.error)
        Button(onClick = { input?.let(onComplete) }, enabled = input != null && !saving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (saving) R.string.saving else R.string.finish_onboarding))
        }
    }
}

@Composable
internal fun NotificationPermissionOption() {
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    var granted by rememberSaveable { mutableStateOf(
        Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { accepted ->
        granted = accepted
        denied = !accepted
    }
    NotificationPermissionContent(granted, denied) { request.launch(Manifest.permission.POST_NOTIFICATIONS) }
}

@Composable
internal fun NotificationPermissionContent(granted: Boolean, denied: Boolean, onRequest: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.notifications_optional), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.notifications_explain), style = MaterialTheme.typography.bodyMedium)
        when {
            granted -> Text(stringResource(R.string.permission_granted))
            denied -> Text(stringResource(R.string.permission_denied))
            else -> TextButton(onClick = onRequest) {
                Text(stringResource(R.string.enable_notifications))
            }
        }
    }
}

internal fun parseInput(name: String, zone: String, currency: String, startDay: String): ProfileInput? {
    val day = startDay.toIntOrNull() ?: return null
    if (name.isBlank() || day !in 1..28 || !Regex("[A-Z]{3}").matches(currency)) return null
    try { java.time.ZoneId.of(zone) } catch (_: Exception) { return null }
    try { java.util.Currency.getInstance(currency) } catch (_: Exception) { return null }
    return ProfileInput(name.trim(), zone, currency, day)
}
