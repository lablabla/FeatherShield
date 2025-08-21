package com.lablabla.feathershield.data.repository

import android.util.Log
import com.lablabla.feathershield.data.model.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeviceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
) {
    fun getDevices(): Flow<List<Device>> {
        val userId = auth.currentUser?.uid ?: return emptyFlow()

        // Get a real-time flow of all devices in the user's subcollection
        val userDevicesFlow = firestore.collection("users").document(userId)
            .collection("devices")
            .snapshots()
            .map { it.documents }

        // Get a real-time flow of all devices in the global 'devices' collection
        val globalDevicesFlow = firestore.collection("devices")
            .snapshots()
            .map { it.documents }

        // Combine the two flows
        return userDevicesFlow.combine(globalDevicesFlow) { userDevices, globalDevices ->
            userDevices.mapNotNull { userDoc ->
                val deviceId = userDoc.id
                val userDevice = userDoc.toObject(Device::class.java)

                // Find the corresponding global device document
                val globalDevice = globalDevices.find { it.id == deviceId }?.toObject(Device::class.java)

                // Combine data from both documents
                if (userDevice != null && globalDevice != null) {
                    userDevice.copy(
                        id = deviceId,
                        name = userDevice.name, // The user-defined name
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

    suspend fun getDevice(deviceId: String) : Flow<Device?> {
        val userId = auth.currentUser?.uid ?: return emptyFlow()
        val userDeviceFlow = firestore.collection("users").document(userId)
            .collection("devices")
            .document(deviceId)
            .snapshots()
            .map { it.toObject(Device::class.java) }
        val globalDeviceFlow = firestore.collection("devices").document(deviceId)
            .snapshots()
            .map { it.toObject(Device::class.java) }


        return userDeviceFlow.combine(globalDeviceFlow) { userDevice, globalDevice ->
            if (userDevice != null && globalDevice != null) {
                val storageFileRef = storage.reference.child(globalDevice.lastImageUrl)

                val downloadUrl = storageFileRef.downloadUrl.await().toString()

                userDevice.copy(
                    id = deviceId,
                    name = userDevice.name,
                    batteryLevel = globalDevice.batteryLevel,
                    lastImageUrl = downloadUrl,
                    lastUpdated = globalDevice.lastUpdated,
                    liveStreamUrl = globalDevice.liveStreamUrl
                )
            } else null
        }
    }
}