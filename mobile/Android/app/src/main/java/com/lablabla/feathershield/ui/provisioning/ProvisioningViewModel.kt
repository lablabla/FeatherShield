package com.lablabla.feathershield.ui.provisioning

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lablabla.feathershield.data.model.WifiCredentials
import com.lablabla.feathershield.data.repository.BleProvisioningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@SuppressLint("MissingPermission")
class ProvisioningViewModel @Inject constructor(
    private val bleProvisioningRepository: BleProvisioningRepository
) : ViewModel() {

    private val _provisioningState = MutableStateFlow<ProvisioningState>(ProvisioningState.Idle)
    val provisioningState: StateFlow<ProvisioningState> = _provisioningState.asStateFlow()

    fun startScan() {
        _provisioningState.value = ProvisioningState.Scanning
        viewModelScope.launch {
            bleProvisioningRepository.startScan()
            bleProvisioningRepository.foundDevices.collect { devices ->
                if (devices.isNotEmpty()) {
                    _provisioningState.value = ProvisioningState.DevicesFound(devices)
                }
            }
        }
    }

    fun connectDevice(device: BluetoothDevice) {
        _provisioningState.value = ProvisioningState.Connecting(device)
        bleProvisioningRepository.connectDevice(device)
    }

    fun sendWifiCredentials(credentials: WifiCredentials) {
        viewModelScope.launch {
            bleProvisioningRepository.sendWifiCredentials(credentials)
        }
    }

    fun resetState() {
        _provisioningState.value = ProvisioningState.Idle
    }
}

sealed class ProvisioningState {
    data object Idle : ProvisioningState()
    data object Scanning : ProvisioningState()
    data class DevicesFound(val devices: List<BluetoothDevice>) : ProvisioningState()
    data class Connecting(val device: BluetoothDevice) : ProvisioningState()
    data object ProvisioningSuccess : ProvisioningState()
    data class Error(val message: String) : ProvisioningState()
}