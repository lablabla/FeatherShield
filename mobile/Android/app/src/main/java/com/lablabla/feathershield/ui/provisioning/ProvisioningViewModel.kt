package com.lablabla.feathershield.ui.provisioning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.lablabla.feathershield.data.repository.EspProvisioningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

// TODO: Move to data layer
data class StepStatus(
    val inProgress: Boolean = false,
    val isDone: Boolean = false
)
enum class UiStepStatus { PENDING, IN_PROGRESS, DONE, ERROR }

sealed class ProvisioningState {
    object Scanning : ProvisioningState()
    object ConnectingToDevice : ProvisioningState()
    object NetworkScan : ProvisioningState()
    data class NetworkSelection(val networks: List<String>) : ProvisioningState()
    data class Provisioning(
        val sendingCredentialsStatus: StepStatus,
        val applyingWifiConnectionStatus: StepStatus,
        val checkingProvisioningStatus: StepStatus
    ) : ProvisioningState()
    object Success : ProvisioningState()
    data class Error(val message: String) : ProvisioningState()
}
@OptIn(ExperimentalPermissionsApi::class)
@HiltViewModel
class ProvisioningViewModel @Inject constructor(
    private val espProvisioningRepository: EspProvisioningRepository
) : ViewModel() {

    @OptIn(ExperimentalPermissionsApi::class)
    private val _uiState = MutableStateFlow(ProvisioningUiState())
    val uiState = _uiState.asStateFlow()

    private var pop: String? = null
    private var deviceName: String? = null

    init {
        observeProvisioningState()
    }

    private fun observeProvisioningState() {
        viewModelScope.launch {
            espProvisioningRepository.provisioningState.collect { repoState ->
                val newFlowState = mapRepoStateToUiState(repoState)
                _uiState.update { it.copy(flowState = newFlowState) }
            }
        }
    }

    fun handleAction(action: ProvisioningAction) {
        when (action) {
            is ProvisioningAction.OnQrCodeScanned -> onQrCodeScanned(action.data)
            is ProvisioningAction.OnNetworkSelected -> _uiState.update { it.copy(selectedSsid = action.ssid) }
            is ProvisioningAction.OnPasswordChange -> _uiState.update { it.copy(password = action.password) }
            is ProvisioningAction.OnTogglePasswordVisibility -> _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            is ProvisioningAction.OnConnectClick -> connectToNetwork()
            is ProvisioningAction.OnTryAgainClick -> {
                // Reset the UI flow to the beginning
                _uiState.update { it.copy(flowState = ProvisioningFlowState.CameraPermission) }
            }
            is ProvisioningAction.OnBackClick -> {
                espProvisioningRepository.stopBleScan()
            }
            // Other actions are handled by the Route or don't require VM logic here.
            else -> { }
        }
    }

    private fun onQrCodeScanned(qrCodeData: String) {
        viewModelScope.launch {
            try {
                val qrCodeJson = JSONObject(qrCodeData)
                pop = qrCodeJson.getString("pop")
                deviceName = qrCodeJson.getString("name")
                // The repository's state flow will automatically update to ConnectingToDevice
                espProvisioningRepository.startBleScan(deviceName!!, pop!!)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(flowState = ProvisioningFlowState.Error("Invalid QR Code: ${e.message}"))
                }
            }
        }
    }

    private fun connectToNetwork() {
        val currentState = _uiState.value
        if (currentState.selectedSsid.isNotBlank()) {
            espProvisioningRepository.provisionDevice(currentState.selectedSsid, currentState.password)
        }
    }

    private fun mapRepoStateToUiState(repoState:  ProvisioningState): ProvisioningFlowState {
        return when (repoState) {
            is  ProvisioningState.Scanning -> ProvisioningFlowState.Scanning
            is  ProvisioningState.ConnectingToDevice -> ProvisioningFlowState.Provisioning(
                steps = listOf(ProvisioningStep("Connecting to device...", UiStepStatus.IN_PROGRESS))
            )
            is  ProvisioningState.NetworkScan -> ProvisioningFlowState.Provisioning(
                steps = listOf(
                    ProvisioningStep("Connecting to device...", UiStepStatus.DONE),
                    ProvisioningStep("Scanning for networks...", UiStepStatus.IN_PROGRESS)
                )
            )
            is  ProvisioningState.NetworkSelection -> ProvisioningFlowState.NetworkSelection(repoState.networks)
            is  ProvisioningState.Provisioning -> ProvisioningFlowState.Provisioning(
                steps = listOf(
                    ProvisioningStep("Sending Credentials", mapRepoStepStatus(repoState.sendingCredentialsStatus)),
                    ProvisioningStep("Applying Wi-Fi connection", mapRepoStepStatus(repoState.applyingWifiConnectionStatus)),
                    ProvisioningStep("Verifying status", mapRepoStepStatus(repoState.checkingProvisioningStatus))
                )
            )
            is  ProvisioningState.Success -> ProvisioningFlowState.Success
            is  ProvisioningState.Error -> ProvisioningFlowState.Error(repoState.message)
        }
    }

    private fun mapRepoStepStatus(repoStatus: StepStatus): UiStepStatus {
        return when {
            repoStatus.isDone -> UiStepStatus.DONE
            repoStatus.inProgress -> UiStepStatus.IN_PROGRESS
            else -> UiStepStatus.PENDING
        }
    }
}

