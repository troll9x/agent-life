package vn.lifeos.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.profile.LocalProfileSnapshot
import vn.lifeos.data.profile.LocalProfileStore
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.domain.foundation.Outcome
import vn.lifeos.app.planner.PlannerFeature
import vn.lifeos.app.planner.PlannerUiState
import vn.lifeos.app.planner.PlannerActions
import vn.lifeos.app.planner.EventDraft
import vn.lifeos.domain.common.Uuid
import vn.lifeos.domain.planner.CalendarEvent
import vn.lifeos.domain.planner.PlannedTask
import vn.lifeos.domain.planner.TaskChecklistItem
import vn.lifeos.domain.planner.TaskState
import java.time.LocalDate

internal sealed interface ProfileStatus {
    data object Loading : ProfileStatus
    data object NeedsOnboarding : ProfileStatus
    data class Ready(val snapshot: LocalProfileSnapshot) : ProfileStatus
    data object Error : ProfileStatus
}

internal enum class ProfileAction { Idle, Saving, Error }

internal enum class StartupStatus { Loading, Ready, UnlockDevice, Recovery, StorageError }

internal data class ShellUiState(
    val startup: StartupStatus = StartupStatus.Loading,
    val darkTheme: Boolean = false,
    val online: Boolean = true,
    val profile: ProfileStatus = ProfileStatus.Loading,
    val profileAction: ProfileAction = ProfileAction.Idle,
    val now: Instant = Instant.EPOCH,
    val planner: PlannerUiState = PlannerUiState(),
)

internal class ShellViewModel(
    private val startupRepository: StartupRepository,
    private val appearanceRepository: AppearanceRepository,
    networkStatus: kotlinx.coroutines.flow.Flow<Boolean>,
    private val container: AppContainer,
) : ViewModel(), PlannerActions {
    private val mutableState = MutableStateFlow(ShellUiState(darkTheme = appearanceRepository.isDarkTheme(), now = container.clock.instant()))
    val state: StateFlow<ShellUiState> = mutableState.asStateFlow()
    private var openJob: Job? = null
    private var profileJob: Job? = null
    private var saveJob: Job? = null
    private val zoneSyncMutex = Mutex()
    private var profileStore: LocalProfileStore? = null
    private var plannerFeature: PlannerFeature? = null
    private var plannerJob: Job? = null
    private var plannerDatabase: vn.lifeos.data.db.AgentLifeDatabase? = null

    init {
        retry()
        viewModelScope.launch {
            networkStatus.collect { online -> mutableState.value = mutableState.value.copy(online = online) }
        }
    }

    fun retry() {
        if (openJob?.isActive == true) return
        openJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(startup = StartupStatus.Loading)
            val status = try {
                when (val access = startupRepository.open()) {
                    is DatabaseAccess.Ready -> {
                        plannerDatabase = access.database
                        if (profileStore == null) profileStore = container.profileStore(access.database)
                        if (profileJob?.isActive != true) {
                            mutableState.value = mutableState.value.copy(profile = ProfileStatus.Loading)
                            profileJob = viewModelScope.launch {
                                profileStore!!.observe().collect { result ->
                                    mutableState.value = mutableState.value.copy(profile = when (result) {
                                        is Outcome.Success -> result.value?.let(ProfileStatus::Ready) ?: ProfileStatus.NeedsOnboarding
                                        is Outcome.Failure -> ProfileStatus.Error
                                    })
                                    val ready = (mutableState.value.profile as? ProfileStatus.Ready)?.snapshot
                                    if (ready != null && plannerFeature == null) {
                                        startPlanner(access.database, ready.profile.workspaceId)
                                    }
                                }
                            }
                        }
                        StartupStatus.Ready
                    }
                    DatabaseAccess.UnlockDeviceFirst -> StartupStatus.UnlockDevice
                    DatabaseAccess.RecoveryRequired -> StartupStatus.Recovery
                    DatabaseAccess.StorageUnavailable -> StartupStatus.StorageError
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                StartupStatus.StorageError
            }
            mutableState.value = mutableState.value.copy(startup = status)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        appearanceRepository.setDarkTheme(enabled)
        mutableState.value = mutableState.value.copy(darkTheme = enabled)
    }

    fun completeOnboarding(input: ProfileInput) = saveProfile {
        profileStore?.complete(input)
    }

    fun updateProfile(input: ProfileInput) = saveProfile {
        val profile = (mutableState.value.profile as? ProfileStatus.Ready)?.snapshot?.profile
        if (profile == null) null else profileStore?.update(profile, input)
    }

    fun syncDeviceTimeZone(zoneId: String) {
        if (runCatching { ZoneId.of(zoneId) }.isFailure || saveJob?.isActive == true) return
        if (mutableState.value.profile !is ProfileStatus.Ready) return
        val store = profileStore ?: return
        viewModelScope.launch {
            zoneSyncMutex.withLock {
                val profile = (store.observe().first() as? Outcome.Success)?.value?.profile ?: return@withLock
                if (profile.zoneId == zoneId) return@withLock
                val result = try {
                    store.update(profile, ProfileInput(profile.displayName, zoneId, profile.baseCurrency, profile.budgetStartDay))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { null }
                if (result !is Outcome.Success) mutableState.value = mutableState.value.copy(profileAction = ProfileAction.Error)
            }
        }
    }

    private fun saveProfile(operation: suspend () -> Outcome<*>?) {
        if (saveJob?.isActive == true) return
        saveJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(profileAction = ProfileAction.Saving)
            val result = try { operation() } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
            mutableState.value = mutableState.value.copy(
                profileAction = if (result is Outcome.Success) ProfileAction.Idle else ProfileAction.Error,
            )
        }
    }

    override fun createProject(title: String) { plannerFeature?.createProject(title) }
    override fun createTask(title: String, note: String?, projectId: Uuid?, priority: Int, dueAt: Instant?, recurrence: String?) {
        plannerFeature?.createTask(title, note, projectId, priority, dueAt, recurrence)
    }
    override fun updateTask(task: PlannedTask, title: String, note: String?, projectId: Uuid?, priority: Int, dueAt: Instant?, recurrence: String?) {
        plannerFeature?.updateTask(task, title, note, projectId, priority, dueAt, recurrence)
    }
    override fun setTaskState(task: PlannedTask, state: TaskState) { plannerFeature?.setTaskState(task, state) }
    override fun selectChecklist(taskId: Uuid?) { plannerFeature?.selectChecklist(taskId) }
    override fun addChecklist(taskId: Uuid, title: String, order: Int) { plannerFeature?.addChecklist(taskId, title, order) }
    override fun setChecklistDone(item: TaskChecklistItem, done: Boolean) { plannerFeature?.setChecklistDone(item, done) }
    override fun createEvent(input: EventDraft) { plannerFeature?.createEvent(input) }
    override fun updateEvent(event: CalendarEvent, input: EventDraft) { plannerFeature?.updateEvent(event, input) }
    override fun replaceOccurrence(event: CalendarEvent, originalDate: LocalDate, input: EventDraft) { plannerFeature?.replaceOccurrence(event, originalDate, input) }
    override fun clearPlannerError() { plannerFeature?.clearError() }
    override fun retryPlanner() {
        plannerFeature?.let { it.retry(); return }
        val database = plannerDatabase ?: return retry()
        val workspaceId = (mutableState.value.profile as? ProfileStatus.Ready)?.snapshot?.profile?.workspaceId ?: return
        viewModelScope.launch { startPlanner(database, workspaceId) }
    }

    private suspend fun startPlanner(database: vn.lifeos.data.db.AgentLifeDatabase, workspaceId: Uuid) {
        plannerFeature = try { container.plannerFeature(database, workspaceId, viewModelScope) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { null }
        if (plannerFeature == null) mutableState.value = mutableState.value.copy(
            planner = PlannerUiState(loading = false, loadFailed = true, error = "Không mở được dữ liệu công việc/lịch."))
        plannerJob?.cancel()
        plannerJob = plannerFeature?.let { feature -> viewModelScope.launch {
            feature.state.collect { planner -> mutableState.value = mutableState.value.copy(planner = planner) }
        } }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == ShellViewModel::class.java)
                return ShellViewModel(
                    container.startupRepository,
                    container.appearanceRepository,
                    container.networkStatus,
                    container,
                ) as T
            }
        }
    }
}
