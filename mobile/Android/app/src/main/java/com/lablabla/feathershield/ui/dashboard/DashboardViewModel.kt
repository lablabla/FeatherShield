package com.lablabla.feathershield.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lablabla.feathershield.data.repository.AuthRepository
import com.lablabla.feathershield.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceRepository: DeviceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        fetchDevices()
    }

    fun handleAction(action: DashboardAction) {
        when (action) {
            // Navigation is handled in the Route; the ViewModel just performs the sign-out logic.
            is DashboardAction.OnSignOutClick -> signOut()
            // These are pure navigation events handled by the Route, so no action is needed here.
            is DashboardAction.OnAddDeviceClick -> { /* No-op */ }
            is DashboardAction.OnDeviceClick -> { /* No-op */ }
        }
    }

    fun signOut() {
        // Firebase's signOut is not a suspend function, so a coroutine is not strictly needed.
        authRepository.signOut()
    }

    private fun fetchDevices() {
        viewModelScope.launch {
            // Start with a loading state.
            _uiState.update { it.copy(isLoading = true) }

            // Collect devices from the repository in real-time.
            deviceRepository.getDevices()
                .catch {
                    // In case of an error, stop loading.
                    // A more robust implementation could add an error message to the UiState.
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { devices ->
                    // Once data is received, update the state with the new list and stop loading.
                    _uiState.update {
                        it.copy(
                            devices = devices,
                            isLoading = false
                        )
                    }
                }
        }
    }
}
