package vn.lifeos.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable internal data object TodayRoute
@Serializable internal data object WorkRoute
@Serializable internal data object FinanceRoute
@Serializable internal data object HealthRoute
@Serializable internal data object NewsRoute
@Serializable internal data object AssistantRoute
@Serializable internal data object SettingsRoute

private enum class Area(val label: Int, val number: String) {
    Today(R.string.area_today, "01"),
    Work(R.string.area_work, "02"),
    Finance(R.string.area_finance, "03"),
    Health(R.string.area_health, "04"),
    News(R.string.area_news, "05"),
    Assistant(R.string.area_assistant, "06"),
    Settings(R.string.area_settings, "07"),
}

@Composable
internal fun AgentLifeScreen(state: ShellUiState, onRetry: () -> Unit, onThemeChange: (Boolean) -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        when (state.startup) {
            StartupStatus.Loading -> LoadingState()
            StartupStatus.UnlockDevice -> ErrorState(
                title = stringResource(R.string.unlock_title),
                body = stringResource(R.string.unlock_body),
                action = stringResource(R.string.retry),
                onAction = onRetry,
            )
            StartupStatus.Recovery -> ErrorState(
                title = stringResource(R.string.recovery_title),
                body = stringResource(R.string.recovery_body),
                action = stringResource(R.string.retry),
                onAction = onRetry,
            )
            StartupStatus.StorageError -> ErrorState(
                title = stringResource(R.string.storage_title),
                body = stringResource(R.string.storage_body),
                action = stringResource(R.string.retry),
                onAction = onRetry,
            )
            StartupStatus.Ready -> AppShell(state.online, state.darkTheme, onThemeChange)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppShell(online: Boolean, darkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val selected = when {
        destination?.hasRoute<WorkRoute>() == true -> Area.Work
        destination?.hasRoute<FinanceRoute>() == true -> Area.Finance
        destination?.hasRoute<HealthRoute>() == true -> Area.Health
        destination?.hasRoute<NewsRoute>() == true -> Area.News
        destination?.hasRoute<AssistantRoute>() == true -> Area.Assistant
        destination?.hasRoute<SettingsRoute>() == true -> Area.Settings
        else -> Area.Today
    }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val menuLabel = stringResource(R.string.open_menu)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(24.dp),
                )
                HorizontalDivider()
                Area.entries.forEach { area ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(area.label)) },
                        selected = selected == area,
                        onClick = {
                            if (selected != area) {
                                when (area) {
                                    Area.Today -> navController.navigate(TodayRoute)
                                    Area.Work -> navController.navigate(WorkRoute)
                                    Area.Finance -> navController.navigate(FinanceRoute)
                                    Area.Health -> navController.navigate(HealthRoute)
                                    Area.News -> navController.navigate(NewsRoute)
                                    Area.Assistant -> navController.navigate(AssistantRoute)
                                    Area.Settings -> navController.navigate(SettingsRoute)
                                }
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(selected.label)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.semantics { contentDescription = menuLabel },
                        ) {
                            Text(
                                text = "☰",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (!online) OfflineState()
                NavHost(navController, startDestination = TodayRoute, modifier = Modifier.weight(1f)) {
                    composable<TodayRoute> { AreaScreen(Area.Today) }
                    composable<WorkRoute> { AreaScreen(Area.Work) }
                    composable<FinanceRoute> { AreaScreen(Area.Finance) }
                    composable<HealthRoute> { AreaScreen(Area.Health) }
                    composable<NewsRoute> { AreaScreen(Area.News) }
                    composable<AssistantRoute> { AreaScreen(Area.Assistant) }
                    composable<SettingsRoute> { SettingsScreen(darkTheme, onThemeChange) }
                }
            }
        }
    }
}

@Composable
private fun AreaScreen(area: Area) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(area.number, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(area.label), style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
        EmptyState()
    }
}

@Composable
private fun SettingsScreen(darkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(stringResource(R.string.area_settings), style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleLarge)
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.dark_theme), style = MaterialTheme.typography.bodyLarge)
                    val darkLabel = stringResource(R.string.dark_theme)
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = onThemeChange,
                        modifier = Modifier.testTag("dark_theme").semantics { contentDescription = darkLabel },
                    )
                }
            }
        }
        Text(stringResource(R.string.settings_later), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun LoadingState() {
    StatePanel(stringResource(R.string.loading_title), stringResource(R.string.loading_body))
}

@Composable
internal fun EmptyState() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.not_implemented), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.not_implemented_body), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
internal fun ErrorState(title: String, body: String, action: String, onAction: () -> Unit) {
    StatePanel(title, body, action, onAction)
}

@Composable
private fun StatePanel(title: String, body: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            Text(body, style = MaterialTheme.typography.bodyLarge)
            if (action != null && onAction != null) Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
internal fun OfflineState() {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.offline_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.offline_body), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
