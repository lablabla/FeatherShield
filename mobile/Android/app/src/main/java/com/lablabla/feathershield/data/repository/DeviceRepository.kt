package com.lablabla.feathershield.data.repository

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.lablabla.feathershield.data.model.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import io.github.g00fy2.versioncompare.Version
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeviceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {

    fun getDevices(): Flow<List<Device>> {
        val userId = auth.currentUser?.uid ?: return emptyFlow()

        val userDevicesFlow = firestore.collection("users").document(userId)
            .collection("devices")
            .snapshots()
            .map { it.documents }

        val globalDevicesFlow = firestore.collection("devices")
            .snapshots()
            .map { it.documents }

        val latestFirmwareFlow = getLatestFirmwareVersion()

        return combine(userDevicesFlow, globalDevicesFlow, latestFirmwareFlow) { userDevices, globalDevices, latestFirmware ->
            userDevices.mapNotNull { userDoc ->
                val deviceId = userDoc.id
                val userDevice = userDoc.toObject(Device::class.java)
                val globalDevice = globalDevices.find { it.id == deviceId }?.toObject(Device::class.java)

                if (userDevice != null && globalDevice != null) {
                    val isUpdateAvailable = if (latestFirmware != null && globalDevice.fwVersion.isNotEmpty()) {
                        Version(globalDevice.fwVersion).isLowerThan(latestFirmware)
                    } else {
                        false
                    }
                    userDevice.copy(
                        id = deviceId,
                        fwVersion = globalDevice.fwVersion,
                        isUpdateAvailable = isUpdateAvailable,
                        name = userDevice.name,
                        batteryLevel = globalDevice.batteryLevel,
                        lastImageUrl = globalDevice.lastImageUrl,
                        lastUpdated = globalDevice.lastUpdated,
                        liveStreamUrl = globalDevice.liveStreamUrl
                    )
                } else null
            }
        }
    }

    fun addDevice(id: String, name: String) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated.")
        val device = Device(id = id)
        firestore.collection("devices")
            .document(id)
            .set(device)
            .addOnCompleteListener { task ->
                var msg =  if (task.isSuccessful) "Success" else  "Failure"
                Log.d("DEVICES", msg)

            }

        firestore.collection("users").document(userId)
            .collection("devices")
            .document(id)
            .set(
                hashMapOf(
                    "name" to name
                )
            )
    }

    fun getDevice(deviceId: String) : Flow<Device?> {
        return getDevices().map { deviceList ->
            // Find the specific device in the list
            deviceList.firstOrNull { device -> device?.id == deviceId }
        }
    }

    fun getLatestFirmwareVersion(): Flow<String?> {
        return firestore.collection("firmwares").document("esp32")
            .snapshots()
            .mapNotNull { it.getString("latest") }
    }
}