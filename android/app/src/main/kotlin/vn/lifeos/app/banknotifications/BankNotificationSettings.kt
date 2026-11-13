package vn.lifeos.app.banknotifications

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import vn.lifeos.app.AgentLifeApplication
import vn.lifeos.app.R

@Composable
internal fun BankNotificationSettings() {
    val context = LocalContext.current
    val runtime = (context.applicationContext as AgentLifeApplication).appContainer.bankNotifications
    val state by runtime.state.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var settingsUnavailable by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(lifecycle, runtime) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) runtime.refreshAccess() }
        lifecycle.addObserver(observer)
        runtime.refreshAccess()
        onDispose { lifecycle.removeObserver(observer) }
    }
    BankNotificationContent(state, runtime::select, {
        val component = ComponentName(context, BankNotificationListenerService::class.java)
        val detail = if (Build.VERSION.SDK_INT >= 30) Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
            else Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        try { context.startActivity(detail); settingsUnavailable = false }
        catch (_: ActivityNotFoundException) {
            try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); settingsUnavailable = false }
            catch (_: ActivityNotFoundException) { settingsUnavailable = true }
        }
    }, runtime::reconnect)
    if (settingsUnavailable) Text(stringResource(R.string.bank_settings_unavailable), color = MaterialTheme.colorScheme.error)
}

@Composable
internal fun BankNotificationContent(state: BankNotificationState, onSelect: (Set<String>) -> Unit,
    onSettings: () -> Unit, onReconnect: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    Card(Modifier.fillMaxWidth().testTag("bank_notification_settings")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.bank_notification_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.bank_notification_explanation))
            Text(stringResource(if (state.permission) R.string.bank_access_granted else R.string.bank_access_denied),
                modifier = Modifier.testTag("bank_access_state"))
            Text(stringResource(if (state.connected) R.string.bank_listener_connected else R.string.bank_listener_disconnected))
            Text(stringResource(if (state.sourceState in setOf("PARTIAL", "CONNECTED", "OK")) R.string.bank_capture_enabled else R.string.bank_capture_stopped))
            Text(stringResource(R.string.bank_last_received, state.lastReceivedAt?.let {
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))
            } ?: stringResource(R.string.bank_never_received)))
            Text(stringResource(R.string.bank_capture_count, state.captureCount))
            Text(stringResource(R.string.bank_capture_limitations))
            Button(onClick = onSettings) { Text(stringResource(R.string.bank_open_access)) }
            if (state.permission && !state.connected) TextButton(onClick = onReconnect) { Text(stringResource(R.string.bank_reconnect)) }
            if (state.error) Text(stringResource(R.string.bank_capture_error), color = MaterialTheme.colorScheme.error)
            if (state.loading) Text(stringResource(R.string.bank_loading))
            else {
                OutlinedTextField(search, { search = it }, label = { Text(stringResource(R.string.bank_search_app)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("bank_app_search"))
                Text(stringResource(R.string.bank_selected_count, state.selected.size))
                val apps = (state.apps + state.selected.filter { selected -> state.apps.none { it.packageName == selected } }
                    .map { NotificationAppChoice(it, it) }).filter {
                    it.packageName in state.selected || search.isNotBlank() &&
                        (it.label.contains(search, true) || it.packageName.contains(search, true))
                }
                if (state.selected.isEmpty()) Text(stringResource(R.string.bank_nothing_selected))
                if (search.isNotBlank() && apps.isEmpty()) Text(stringResource(R.string.bank_no_app))
                apps.forEach { app ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(app.packageName in state.selected, { checked ->
                            onSelect(if (checked) state.selected + app.packageName else state.selected - app.packageName)
                        }, modifier = Modifier.testTag("bank_app_${app.packageName}").semantics { contentDescription = app.label })
                        Column { Text(app.label); Text(app.packageName, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                if (state.selected.isNotEmpty()) TextButton(onClick = { onSelect(emptySet()) }) { Text(stringResource(R.string.bank_stop_capture)) }
            }
        }
    }
}
