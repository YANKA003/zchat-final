package com.zchat.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zchat.app.R
import com.zchat.app.ui.MainActivity

class GoodokMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM token: $token")
        // Token will be saved when user logs in
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM", "Message received from: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Message data payload: ${remoteMessage.data}")

            val type = remoteMessage.data["type"]
            val title = remoteMessage.data["title"] ?: "GOODOK"
            val body = remoteMessage.data["body"] ?: "Новое сообщение"
            val senderId = remoteMessage.data["senderId"]
            val senderName = remoteMessage.data["senderName"]

            when (type) {
                "message" -> showMessageNotification(title, body, senderId, senderName)
                "call" -> showCallNotification(title, body, senderId, senderName)
                else -> showDefaultNotification(title, body)
            }
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d("FCM", "Message notification body: ${it.body}")
            showDefaultNotification(it.title ?: "GOODOK", it.body ?: "")
        }
    }

    private fun showMessageNotification(title: String, body: String, senderId: String?, senderName: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("openChat", true)
            putExtra("userId", senderId)
            putExtra("username", senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, (senderId?.hashCode() ?: 0),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "messages"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Сообщения",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Уведомления о новых сообщениях"
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(senderId?.hashCode() ?: 0, notificationBuilder.build())
    }

    private fun showCallNotification(title: String, body: String, senderId: String?, senderName: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("incomingCall", true)
            putExtra("callerId", senderId)
            putExtra("callerName", senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 1001,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "calls"
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Входящий звонок: $title")
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(ringtoneUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Звонки",
                NotificationManager.IMPORTANCE_MAX
            )
            channel.description = "Уведомления о звонках"
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(1001, notificationBuilder.build())
    }

    private fun showDefaultNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "general"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Общие",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
