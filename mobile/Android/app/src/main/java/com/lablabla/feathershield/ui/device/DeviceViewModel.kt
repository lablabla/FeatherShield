package com.lablabla.feathershield.ui.device

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
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
class DeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])
    private val _uiState = MutableStateFlow(DeviceUiState(isLoading = true, name = deviceId))
    val uiState: StateFlow<DeviceUiState> = _uiState.asStateFlow()

    init {
        getDevice()
    }

    fun handleAction(action: DeviceAction) {
        when (action) {
            is DeviceAction.OnStartStreamClick -> updateCommand("start_stream")
            is DeviceAction.OnStopStreamClick -> updateCommand("stop_stream")
            is DeviceAction.OnUpdateFirmwareClick -> updateCommand("update_firmware")
            is DeviceAction.OnSprayClick -> updateCommand("spray") // Assuming "spray" is a valid command
            is DeviceAction.OnBackClick -> { /* No-op, handled by the Route */ }
        }
    }

    private fun getDevice() {
        viewModelScope.launch {
            deviceRepository.getDevice(deviceId)
                .catch {
                    // Handle potential errors, e.g., device not found or permission issues
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { device ->
                    // Map the domain model 'Device' to the 'DeviceUiState'
                    _uiState.update {
                        if (device != null) {
                            it.copy(
                                isLoading = false,
                                batteryLevel = device.batteryLevel,
                                lastImageUrl = device.lastImageUrl,
                                isUpdateAvailable = device.isUpdateAvailable,
                            )
                        } else {
                            // Handle case where device is null (e.g., deleted)
                            it.copy(isLoading = false)
                        }
                    }
                }
        }
    }

    private fun updateCommand(command: String, args: HashMap<String, Any>? = null) {
        viewModelScope.launch {
            // Use a map of String to Any for broader compatibility with Firestore
            val commandMap = args ?: hashMapOf("command" to command)
            firestore.collection("commands")
                .document(deviceId)
                .set(commandMap)
            // Optionally add .await() and try/catch for error handling
        }
    }
}
