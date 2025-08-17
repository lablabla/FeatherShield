package com.lablabla.feathershield.ui.device

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.lablabla.feathershield.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val messaging: FirebaseMessaging
) : ViewModel() {

    private val _addDeviceState = MutableStateFlow<AddDeviceState>(AddDeviceState.Idle)
    val addDeviceState: StateFlow<AddDeviceState> = _addDeviceState.asStateFlow()

    fun addDevice(id: String, name: String) {
        _addDeviceState.value = AddDeviceState.Loading
        viewModelScope.launch {
            try {
                val topic = "alerts_$id"
                Log.d("ADD_DEVICE", "Subscribing to $topic")
                messaging.subscribeToTopic(topic)
                    .addOnCompleteListener { task ->
                        var message = "Device added"
                        if (task.isSuccessful)
                        {
                            Log.d("ADD_DEVICE", "Subscribing succeeded")
                            deviceRepository.addDevice(id, name)
                            _addDeviceState.value = AddDeviceState.Success
                        }
                        else
                        {
                            message = "Adding device failed"
                            _addDeviceState.value = AddDeviceState.Error(message)
                            Log.d("ADD_DEVICE", "Subscribing failed")
                        }
//                        Toast.makeText(baseContext)
                    }
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
