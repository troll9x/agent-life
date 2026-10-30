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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable
import vn.lifeos.app.onboarding.OnboardingScreen
import vn.lifeos.app.onboarding.NotificationStartPrompt
import vn.lifeos.app.settings.ProfileSettingsScreen
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.app.planner.PlannerActions
import vn.lifeos.app.planner.PlannerDashboard
import vn.lifeos.app.planner.PlannerScreen
import vn.lifeos.app.reminders.ReminderStatusPanel
import vn.lifeos.app.finance.entry.FinanceActions
import vn.lifeos.app.finance.entry.FinanceEntryScreen
import androidx.compose.runtime.LaunchedEffect
import java.time.ZoneId

@Serializable internal data object TodayRoute
@Serializable internal data object WorkRoute
@Serializable internal data object FinanceRoute
@Serializable internal data object HealthRoute
@Serializable internal data object NewsRoute
@Serializable internal data object AssistantRoute
@Serializable internal data object SettingsRoute

private enum class Area(val label: Int, val number: String, val icon: Int) {
    Today(R.string.area_today, "01", R.drawable.ic_today),
    Work(R.string.area_work, "02", R.drawable.ic_work),
    Finance(R.string.area_finance, "03", R.drawable.ic_finance),
    Health(R.string.area_health, "04", R.drawable.ic_health),
    News(R.string.area_news, "05", R.drawable.ic_news),
    Assistant(R.string.area_assistant, "06", R.drawable.ic_assistant),
    Settings(R.string.area_settings, "07", R.drawable.ic_settings),
}

@Composable
internal fun AgentLifeScreen(
    state: ShellUiState,
    onRetry: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onCompleteOnboarding: (ProfileInput) -> Unit = {},
    onUpdateProfile: (ProfileInput) -> Unit = {},
    plannerActions: PlannerActions? = null,
    financeActions: FinanceActions? = null,
    reminderDestination: ReminderDestination? = null,
    onRetryReminders: () -> Unit = {},
) {
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
            StartupStatus.Ready -> when (val profile = state.profile) {
                ProfileStatus.Loading -> LoadingState()
                ProfileStatus.NeedsOnboarding -> OnboardingScreen(
                    saving = state.profileAction == ProfileAction.Saving,
                    failed = state.profileAction == ProfileAction.Error,
                    onComplete = onCompleteOnboarding,
                )
                ProfileStatus.Error -> ErrorState(
                    title = stringResource(R.string.profile_load_error),
                    body = stringResource(R.string.storage_body),
                    action = stringResource(R.string.retry),
                    onAction = onRetry,
                )
                is ProfileStatus.Ready -> AppShell(
                    state.online, state.darkTheme, profile.snapshot, state.now,
                    state.profileAction, state.planner, plannerActions, onThemeChange, onUpdateProfile,
                    state.reminderScheduleError, reminderDestination, onRetryReminders, state.finance, financeActions,
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppShell(
    online: Boolean,
    darkTheme: Boolean,
    snapshot: vn.lifeos.data.profile.LocalProfileSnapshot,
    now: java.time.Instant,
    action: ProfileAction,
    planner: vn.lifeos.app.planner.PlannerUiState,
    plannerActions: PlannerActions?,
    onThemeChange: (Boolean) -> Unit,
    onUpdateProfile: (ProfileInput) -> Unit,
    reminderScheduleError: Boolean,
    reminderDestination: ReminderDestination?,
    onRetryReminders: () -> Unit,
    finance: vn.lifeos.app.finance.entry.FinanceUiState,
    financeActions: FinanceActions?,
) {
    NotificationStartPrompt()
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
    var showMore by remember { mutableStateOf(false) }
    LaunchedEffect(reminderDestination) {
        if (reminderDestination != null && selected != Area.Work) navController.navigate(WorkRoute) { launchSingleTop = true }
    }
    fun navigateTo(area: Area) {
        if (selected == area) return
        when (area) {
            Area.Today -> navController.navigate(TodayRoute) { launchSingleTop = true }
            Area.Work -> navController.navigate(WorkRoute) { launchSingleTop = true }
            Area.Finance -> navController.navigate(FinanceRoute) { launchSingleTop = true }
            Area.Health -> navController.navigate(HealthRoute) { launchSingleTop = true }
            Area.News -> navController.navigate(NewsRoute) { launchSingleTop = true }
            Area.Assistant -> navController.navigate(AssistantRoute) { launchSingleTop = true }
            Area.Settings -> navController.navigate(SettingsRoute) { launchSingleTop = true }
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(if (selected == Area.Today) R.string.app_name else selected.label)) }) },
        bottomBar = {
            NavigationBar {
                listOf(Area.Today, Area.Work, Area.Finance, Area.Assistant).forEach { area ->
                    NavigationBarItem(
                        modifier = Modifier.testTag("nav_${area.name.lowercase()}"),
                        selected = selected == area,
                        onClick = { navigateTo(area) },
                        icon = { Icon(painterResource(area.icon), contentDescription = null) },
                        label = { Text(stringResource(area.label)) },
                    )
                }
                NavigationBarItem(
                    modifier = Modifier.testTag("nav_more"),
                    selected = selected in listOf(Area.Health, Area.News, Area.Settings),
                    onClick = { showMore = true },
                    icon = { Icon(painterResource(R.drawable.ic_more), contentDescription = null) },
                    label = { Text(stringResource(R.string.area_more)) },
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!online) OfflineState()
            if (selected == Area.Today || selected == Area.Work) ReminderStatusPanel(reminderScheduleError, onRetryReminders)
            NavHost(navController, startDestination = TodayRoute, modifier = Modifier.weight(1f)) {
                composable<TodayRoute> {
                    PlannerDashboard(planner, now, ZoneId.of(snapshot.profile.zoneId), plannerActions) { navigateTo(Area.Work) }
                }
                composable<WorkRoute> { PlannerScreen(planner, now, ZoneId.of(snapshot.profile.zoneId), plannerActions,
                    reminderDestination?.type, reminderDestination?.id, reminderDestination?.triggerAt) }
                composable<FinanceRoute> { FinanceEntryScreen(finance, snapshot.profile.workspaceId,
                    ZoneId.of(snapshot.profile.zoneId), now, financeActions) }
                composable<HealthRoute> { AreaScreen(Area.Health) }
                composable<NewsRoute> { AreaScreen(Area.News) }
                composable<AssistantRoute> { AreaScreen(Area.Assistant) }
                composable<SettingsRoute> {
                    ProfileSettingsScreen(snapshot, now, action == ProfileAction.Saving,
                        action == ProfileAction.Error, darkTheme, onThemeChange, onUpdateProfile)
                }
            }
        }
    }
    if (showMore) ModalBottomSheet(onDismissRequest = { showMore = false }) {
        Text(stringResource(R.string.area_more), style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(3) { index ->
                val area = listOf(Area.Health, Area.News, Area.Settings)[index]
                ListItem(
                    headlineContent = { Text(stringResource(area.label)) },
                    leadingContent = { Icon(painterResource(area.icon), contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("more_${area.name.lowercase()}")
                        .clickable { showMore = false; navigateTo(area) },
                )
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
