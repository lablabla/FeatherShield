package com.lablabla.feathershield.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Device(
    val id: String = "",
    val name: String = "",
    val batteryLevel: Int = 0,
    val lastImageUrl: String = "",
    val liveStreamUrl: String = "",
    @ServerTimestamp
    val lastUpdated: Date = Date()
)
