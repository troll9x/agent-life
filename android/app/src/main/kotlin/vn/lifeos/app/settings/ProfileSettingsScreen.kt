package vn.lifeos.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import vn.lifeos.app.R
import vn.lifeos.app.UserFormatters
import vn.lifeos.app.onboarding.NotificationPermissionOption
import vn.lifeos.app.onboarding.parseInput
import vn.lifeos.data.profile.ConnectionSummary
import vn.lifeos.data.profile.LocalProfileSnapshot
import vn.lifeos.data.profile.ProfileInput

@Composable
internal fun ProfileSettingsScreen(
    snapshot: LocalProfileSnapshot,
    now: Instant,
    saving: Boolean,
    failed: Boolean,
    darkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onSave: (ProfileInput) -> Unit,
) {
    val profile = snapshot.profile
    var name by rememberSaveable(profile.id.value, profile.version) { mutableStateOf(profile.displayName) }
    var startDay by rememberSaveable(profile.id.value, profile.version) { mutableStateOf(profile.budgetStartDay.toString()) }
    val deviceZone = ZoneId.systemDefault().id
    val input = parseInput(name, deviceZone, profile.baseCurrency, startDay)
    val preview = input?.let {
        budgetPeriodContaining(now.atZone(ZoneId.of(it.zoneId)).toLocalDate(), it.budgetStartDay)
    }
    val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.display_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.device_time_zone, deviceZone), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(startDay, { startDay = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.budget_start_day)) }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("budget_day"))
        if (preview != null) Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.period_preview), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.period_preview_value, dateFormat.format(preview.start), dateFormat.format(preview.endExclusive)), modifier = Modifier.testTag("period_preview"))
                Text(stringResource(R.string.period_impact), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (failed) Text(stringResource(R.string.save_error), color = MaterialTheme.colorScheme.error)
        Button(onClick = { parseInput(name, ZoneId.systemDefault().id, profile.baseCurrency, startDay)?.let(onSave) },
            enabled = input != null && !saving) {
            Text(stringResource(if (saving) R.string.saving else R.string.save_profile))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.dark_theme))
            val darkLabel = stringResource(R.string.dark_theme)
            Switch(darkTheme, onThemeChange, modifier = Modifier.testTag("dark_theme").semantics { contentDescription = darkLabel })
        }
        NotificationPermissionOption()
        IntegrationsScreen(snapshot.connections, profile.zoneId, profile.baseCurrency)
    }
}

@Composable
private fun IntegrationsScreen(connections: List<ConnectionSummary>, zone: String, currency: String) {
    var gate by rememberSaveable { mutableStateOf(false) }
    val formatters = UserFormatters(zone, currency)
    Text(stringResource(R.string.integrations_title), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(R.string.integrations_gate), style = MaterialTheme.typography.bodyMedium)
    val providers = (listOf("MICROSOFT", "GOOGLE", "HEALTH_CONNECT") + connections.map { it.provider }).distinct()
    providers.forEach { provider ->
        val matching = connections.filter { it.provider == provider }
        if (matching.isEmpty()) ConnectionCard(provider, null, formatters) { gate = true }
        else matching.forEach { ConnectionCard(provider, it, formatters) { gate = true } }
    }
    if (gate) Text(stringResource(R.string.connector_unavailable), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun ConnectionCard(provider: String, connection: ConnectionSummary?, formatters: UserFormatters, onAction: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(provider, style = MaterialTheme.typography.titleMedium)
            Text(connection?.state ?: stringResource(R.string.not_connected))
            Text(connection?.lastSuccessfulAt?.let { stringResource(R.string.last_success, formatters.dateTime(Instant.ofEpochMilli(it))) }
                ?: stringResource(R.string.never_synced))
            Text(connection?.scopes?.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.selected_scopes, it.joinToString(", ")) }
                ?: stringResource(R.string.no_scopes))
            TextButton(onClick = onAction) { Text(stringResource(if (connection == null) R.string.connect else R.string.disconnect)) }
        }
    }
}
