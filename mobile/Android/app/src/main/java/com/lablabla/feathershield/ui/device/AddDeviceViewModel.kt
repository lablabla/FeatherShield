package com.lablabla.feathershield.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lablabla.feathershield.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val _addDeviceState = MutableStateFlow<AddDeviceState>(AddDeviceState.Idle)
    val addDeviceState: StateFlow<AddDeviceState> = _addDeviceState.asStateFlow()

    fun addDevice(id: String, name: String) {
        _addDeviceState.value = AddDeviceState.Loading
        viewModelScope.launch {
            try {
                deviceRepository.addDevice(id, name)
                _addDeviceState.value = AddDeviceState.Success
            } catch (e: Exception) {
                _addDeviceState.value = AddDeviceState.Error(e.message ?: "Failed to add device")
            }
        }
    }

    fun resetState() {
        _addDeviceState.value = AddDeviceState.Idle
    }
}

sealed class AddDeviceState {
    data object Idle : AddDeviceState()
    data object Loading : AddDeviceState()
    data object Success : AddDeviceState()
    data class Error(val message: String) : AddDeviceState()
}
