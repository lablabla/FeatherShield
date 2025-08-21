package com.lablabla.feathershield.ui.device

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.lablabla.feathershield.data.model.Device
import com.lablabla.feathershield.data.repository.AuthRepository
import com.lablabla.feathershield.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle["deviceId"])
    private val _device = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _device.asStateFlow()

    init {
        getDevice()
    }

    private fun getDevice() {
        viewModelScope.launch {
            deviceRepository.getDevice(deviceId).collect {
                _device.value = it
            }
        }
    }

    fun startStream() {
        updateCommand("start_stream")
    }

    fun stopStream() {
        updateCommand("stop_stream")
    }

    private fun updateCommand(command: String) {
        viewModelScope.launch {
            firestore.collection("commands")
                .document(deviceId)
                .set(
                    hashMapOf(
                        "command" to command
                    )
                )
        }
    }
}
