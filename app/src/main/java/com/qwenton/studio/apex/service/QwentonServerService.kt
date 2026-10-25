package com.qwenton.studio.apex.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.qwenton.studio.apex.MainActivity
import com.qwenton.studio.apex.R
import com.qwenton.studio.apex.terminal.QwentonTerminalBridge
import com.qwenton.studio.apex.terminal.ShellScriptFactory

class QwentonServerService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopServerInternal()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val modelName = intent?.getStringExtra(EXTRA_MODEL_NAME) ?: "Local GGUF Model"
        val endpoint = intent?.getStringExtra(EXTRA_ENDPOINT) ?: "http://127.0.0.1:8080/v1"

        val notification = buildNotification(modelName, endpoint)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "QwentonStudio::ServerWakeLock"
            )?.apply {
                acquire(12 * 60 * 60 * 1000L) // 12-hour safe ceiling
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager?.createWifiLock(
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY,
                    "QwentonStudio::WifiLock"
                )
            } else {
                @Suppress("DEPRECATION")
                wifiManager?.createWifiLock(
                    WifiManager.WIFI_MODE_FULL,
                    "QwentonStudio::WifiLock"
                )
            }?.apply {
                acquire()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock / WifiLock", e)
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing locks", e)
        }
    }

    private fun stopServerInternal() {
        val bridge = QwentonTerminalBridge.getInstance(applicationContext)
        val stopCmd = ShellScriptFactory.buildStopServerCommand()
        bridge.dispatchCommand(stopCmd)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Qwenton Server Running",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of the local AI server and active endpoint"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(modelName: String, endpoint: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, QwentonServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Qwenton AI Server: $modelName")
            .setContentText("Listening on $endpoint")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(launchPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Server", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseLocks()
        stopServerInternal()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "QwentonServerService"
        const val CHANNEL_ID = "qwenton_server_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.qwenton.studio.apex.START_SERVER"
        const val ACTION_STOP = "com.qwenton.studio.apex.STOP_SERVER"

        const val EXTRA_MODEL_NAME = "extra_model_name"
        const val EXTRA_ENDPOINT = "extra_endpoint"

        fun start(context: Context, modelName: String, endpoint: String) {
            val intent = Intent(context, QwentonServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_MODEL_NAME, modelName)
                putExtra(EXTRA_ENDPOINT, endpoint)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, QwentonServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
