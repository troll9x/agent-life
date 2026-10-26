package vn.lifeos.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.lifeos.data.security.DatabaseAccess
import vn.lifeos.data.profile.LocalProfileSnapshot
import vn.lifeos.data.profile.LocalProfileStore
import vn.lifeos.data.profile.ProfileInput
import vn.lifeos.domain.foundation.Outcome

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
)

internal class ShellViewModel(
    private val startupRepository: StartupRepository,
    private val appearanceRepository: AppearanceRepository,
    networkStatus: kotlinx.coroutines.flow.Flow<Boolean>,
    private val container: AppContainer,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ShellUiState(darkTheme = appearanceRepository.isDarkTheme(), now = container.clock.instant()))
    val state: StateFlow<ShellUiState> = mutableState.asStateFlow()
    private var openJob: Job? = null
    private var profileJob: Job? = null
    private var saveJob: Job? = null
    private var profileStore: LocalProfileStore? = null

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
                        if (profileStore == null) profileStore = container.profileStore(access.database)
                        if (profileJob?.isActive != true) {
                            mutableState.value = mutableState.value.copy(profile = ProfileStatus.Loading)
                            profileJob = viewModelScope.launch {
                                profileStore!!.observe().collect { result ->
                                    mutableState.value = mutableState.value.copy(profile = when (result) {
                                        is Outcome.Success -> result.value?.let(ProfileStatus::Ready) ?: ProfileStatus.NeedsOnboarding
                                        is Outcome.Failure -> ProfileStatus.Error
                                    })
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
