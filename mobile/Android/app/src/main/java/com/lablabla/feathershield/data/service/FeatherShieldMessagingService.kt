package com.lablabla.feathershield.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.lablabla.feathershield.R

class FeatherShieldMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle messages with both data and notification payloads.
        remoteMessage.notification?.let {
            // Display the notification to the user.
            showNotification(it.title, it.body, remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        // This is called whenever a new token is generated.
        // You would typically send this token to your backend.
        // For this project, we are using topics, so this is less critical.
    }

    private fun showNotification(title: String?, body: String?, data: Map<String, String>) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create a notification channel for Android 8.0 and higher.
        val channelId = "feathershield_channel_id"
        val channel = NotificationChannel(
            channelId,
            "FeatherShield Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)

        // Build the notification
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(0, builder.build())
    }
}
