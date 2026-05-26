package com.mechpravdy.neo

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
import java.io.File

class ModelLoadService : Service() {

    companion object {
        const val CHANNEL_ID = "model_load_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.mechpravdy.neo.STOP_SERVICE"
        const val BROADCAST_MODEL_LOADED = "com.mechpravdy.neo.MODEL_LOADED"
        const val EXTRA_SUCCESS = "success"
    }

    private var llamaBridge: LlamaBridge? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        llamaBridge = LlamaBridge()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val modelFile = File(getExternalFilesDir("models"), "mistral-7b-instruct-v0.2.Q4_K_M.gguf")

        if (!modelFile.exists()) {
            broadcastResult(false)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification("Загружаю Mistral 7B..."))

        Thread {
            llamaBridge?.loadModelFromPath(
                path = modelFile.absolutePath,
                onProgress = { msg ->
                    updateNotification("Загрузка: $msg")
                    Log.d("ModelLoadService", msg)
                },
                onDone = { success ->
                    broadcastResult(success)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            )
        }.start()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        llamaBridge = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Загрузка модели",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о загрузке ИИ-модели"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        val stopIntent = Intent(this, ModelLoadService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Меч Правды")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Отмена", stopPendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun broadcastResult(success: Boolean) {
        val intent = Intent(BROADCAST_MODEL_LOADED).apply {
            putExtra(EXTRA_SUCCESS, success)
        }
        sendBroadcast(intent)
    }
}
