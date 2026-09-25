package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NetSentryApplication
import com.example.R
import com.example.data.model.AlertLevel
import com.example.data.model.NetworkConnection
import com.example.data.model.TrafficSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NetworkMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_STATUS_ID = "netsentry_service_channel"
        const val CHANNEL_ALERTS_ID = "netsentry_alerts_channel"
        const val NOTIFICATION_STATUS_ID = 1001

        const val ACTION_START = "ACTION_START_MONITORING"
        const val ACTION_STOP = "ACTION_STOP_MONITORING"

        fun startService(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NetworkMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundServiceNotification()
        observeEngineEvents()

        return START_STICKY
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Status channel (Low priority, silent)
            val statusChannel = NotificationChannel(
                CHANNEL_STATUS_ID,
                "NetSentry Background Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time connection audit status and traffic throughput"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(statusChannel)

            // High priority alerts channel for real-time new connection warnings
            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                "New Connection Security Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts immediately when a new outbound or suspicious connection is established"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notification = buildStatusNotification(
            statusText = "Monitoring active connections & traffic...",
            activeCount = 0
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_STATUS_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_STATUS_ID, notification)
        }
    }

    private fun buildStatusNotification(statusText: String, activeCount: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, NetworkMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_STATUS_ID)
            .setContentTitle("NetSentry Network Monitor: Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun observeEngineEvents() {
        val app = application as? NetSentryApplication ?: return
        val engine = app.detectorEngine
        val repo = app.repository

        // Listen for new connection alerts in real-time
        serviceScope.launch {
            engine.newConnectionAlerts.collectLatest { connection ->
                if (repo.notificationsEnabled.value) {
                    showNewConnectionNotification(connection)
                }
            }
        }

        // Update foreground service notification with live stats
        serviceScope.launch {
            engine.trafficSnapshot.collectLatest { snapshot ->
                val activeCount = repo.liveConnections.value.size
                val status = "$activeCount active • ${snapshot.activeTransport} (${TrafficSnapshot.formatSpeed(snapshot.totalRxRate)})"
                notificationManager.notify(
                    NOTIFICATION_STATUS_ID,
                    buildStatusNotification(status, activeCount)
                )
            }
        }
    }

    private fun showNewConnectionNotification(conn: NetworkConnection) {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            conn.remotePort,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (conn.alertLevel) {
            AlertLevel.HIGH_RISK -> "⚠️ High Risk Connection: ${conn.appName}"
            AlertLevel.SUSPICIOUS -> "🚨 Suspicious Connection: ${conn.appName}"
            AlertLevel.NOTICE -> "⚡ New Connection: ${conn.appName}"
            AlertLevel.NORMAL -> "📡 New Outbound Connection"
        }

        val text = "${conn.protocol} to ${conn.displayDestination}:${conn.remotePort} (${conn.serviceName}) via ${conn.networkType}"

        val alertNotification = NotificationCompat.Builder(this, CHANNEL_ALERTS_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\nDetails: ${conn.riskDetails}"))
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), alertNotification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
