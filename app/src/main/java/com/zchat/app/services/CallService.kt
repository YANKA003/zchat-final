package com.zchat.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zchat.app.R
import com.zchat.app.ui.calls.CallActivity

class CallService : Service() {

    companion object {
        const val EXTRA_CALL_ID = "callId"
        const val EXTRA_CALLER_NAME = "callerName"
        const val EXTRA_IS_CALLER = "isCaller"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "call_service"
    }

    private var callId: String? = null
    private var callerName: String? = null
    private var isCaller: Boolean = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callId = intent?.getStringExtra(EXTRA_CALL_ID)
        callerName = intent?.getStringExtra(EXTRA_CALLER_NAME)
        isCaller = intent?.getBooleanExtra(EXTRA_IS_CALLER, false) ?: false

        Log.d("CallService", "Call service started: $callId, caller: $isCaller")

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CallService", "Call service destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Активный звонок",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Уведомление о активном звонке"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("callId", callId)
            putExtra("callerName", callerName)
            putExtra("isCaller", isCaller)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isCaller) {
            "Звонок..."
        } else {
            "Звонок от $callerName"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("Нажмите, чтобы вернуться к звонку")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }
}
