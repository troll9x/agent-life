package vn.lifeos.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.lifeos.data.security.DatabaseAccess

internal enum class StartupStatus { Loading, Ready, UnlockDevice, Recovery, StorageError }

internal data class ShellUiState(
    val startup: StartupStatus = StartupStatus.Loading,
    val darkTheme: Boolean = false,
    val online: Boolean = true,
)

internal class ShellViewModel(
    private val startupRepository: StartupRepository,
    private val appearanceRepository: AppearanceRepository,
    networkStatus: kotlinx.coroutines.flow.Flow<Boolean>,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ShellUiState(darkTheme = appearanceRepository.isDarkTheme()))
    val state: StateFlow<ShellUiState> = mutableState.asStateFlow()
    private var openJob: Job? = null

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
                when (startupRepository.open()) {
                    is DatabaseAccess.Ready -> StartupStatus.Ready
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

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass == ShellViewModel::class.java)
                return ShellViewModel(
                    container.startupRepository,
                    container.appearanceRepository,
                    container.networkStatus,
                ) as T
            }
        }
    }
}
