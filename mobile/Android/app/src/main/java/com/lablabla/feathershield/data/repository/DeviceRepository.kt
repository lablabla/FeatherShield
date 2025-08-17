package com.lablabla.feathershield.data.repository

import com.lablabla.feathershield.data.model.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeviceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    fun getDevices(): Flow<List<Device>> {
        val userId = auth.currentUser?.uid ?: return emptyFlow()
        return firestore.collection("users").document(userId)
            .collection("devices")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toObject(Device::class.java) }
            }
    }

    suspend fun addDevice(id: String, name: String) {
        val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated.")
        val device = Device(id = id, name = name)
        firestore.collection("users").document(userId)
            .collection("devices")
            .document(id)
            .set(device)
            .await()
    }
}