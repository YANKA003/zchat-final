package com.zchat.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zchat.app.R
import com.zchat.app.ui.MainActivity

class GoodokMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save token to Firebase Database for this user
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: getString(R.string.app_name)
            val body = remoteMessage.data["body"] ?: ""
            val senderId = remoteMessage.data["senderId"] ?: ""
            val senderName = remoteMessage.data["senderName"] ?: ""
            val type = remoteMessage.data["type"] ?: "message"

            sendNotification(title, body, senderId, senderName, type)
        }

        remoteMessage.notification?.let {
            sendNotification(
                it.title ?: getString(R.string.app_name),
                it.body ?: "",
                "",
                "",
                "message"
            )
        }
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        senderId: String,
        senderName: String,
        type: String
    ) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("senderId", senderId)
        intent.putExtra("senderName", senderName)
        intent.putExtra("type", type)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "goodok_messages"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
