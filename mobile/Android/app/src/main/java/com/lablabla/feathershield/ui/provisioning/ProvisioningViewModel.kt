package com.lablabla.feathershield.ui.provisioning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lablabla.feathershield.data.model.WifiCredentials
import com.lablabla.feathershield.data.repository.EspProvisioningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ProvisioningViewModel @Inject constructor(
    private val espProvisioningRepository: EspProvisioningRepository
) : ViewModel() {

    val provisioningState = espProvisioningRepository.provisioningState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProvisioningState.Scanning)

    private var pop: String? = null
    private var deviceName: String? = null

    fun onQrCodeScanned(qrCodeData: String) {
        viewModelScope.launch {
            try {
                val qrCodeJson = JSONObject(qrCodeData)
                pop = qrCodeJson.getString("pop")
                deviceName = qrCodeJson.getString("name")
                espProvisioningRepository.startBleScan(deviceName!!, pop!!)
            } catch (e: Exception) {
                // TODO: Handle error
            }
        }
    }

    fun onNetworkSelected(ssid: String, password: String) {
        espProvisioningRepository.provisionDevice(ssid, password)
    }

    fun sendWifiCredentials(credentials: WifiCredentials) {
    }
}

data class StepStatus(
    val inProgress: Boolean = false,
    val isDone: Boolean = false
)

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
