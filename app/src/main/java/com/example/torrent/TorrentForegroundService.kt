package com.example.torrent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.VoltTorrentApp
import com.example.ui.components.formatSpeed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TorrentForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null

    private val engineManager: TorrentEngineManager
        get() = (application as VoltTorrentApp).container.engineManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_PAUSE_ALL -> {
                serviceScope.launch { engineManager.pauseAll() }
            }
            ACTION_RESUME_ALL -> {
                serviceScope.launch { engineManager.resumeAll() }
            }
            else -> {
                engineManager.ensureSessionStarted()
                startForegroundWithNotification(buildStatusNotification(0L, 0L, 0))
                observeTelemetry()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeTelemetry() {
        if (observerJob != null) return
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        observerJob = serviceScope.launch {
            engineManager.globalTelemetry.collectLatest { telemetry ->
                val notification = buildStatusNotification(
                    downloadBps = telemetry.totalDownloadBps,
                    uploadBps = telemetry.totalUploadBps,
                    activeCount = telemetry.activeCount
                )
                nm.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun buildStatusNotification(
        downloadBps: Long,
        uploadBps: Long,
        activeCount: Int
    ): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TorrentForegroundService::class.java).apply { action = ACTION_PAUSE_ALL },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val resumeIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, TorrentForegroundService::class.java).apply { action = ACTION_RESUME_ALL },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (activeCount > 0) {
            "VoltTorrent • Активно: $activeCount"
        } else {
            "VoltTorrent • Движок готов"
        }
        val text = "↓ ${formatSpeed(downloadBps)}   ↑ ${formatSpeed(uploadBps)}"

        return NotificationCompat.Builder(this, CHANNEL_TRANSFERS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.action_pause), pauseIntent)
            .addAction(0, getString(R.string.action_resume), resumeIntent)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val transfersChannel = NotificationChannel(
                CHANNEL_TRANSFERS,
                getString(R.string.notification_channel_transfers_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_transfers_desc)
                setShowBadge(false)
            }
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                getString(R.string.notification_channel_alerts_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_alerts_desc)
            }
            nm.createNotificationChannel(transfersChannel)
            nm.createNotificationChannel(alertsChannel)
        }
    }

    override fun onDestroy() {
        observerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.torrent.START"
        const val ACTION_STOP = "com.example.torrent.STOP"
        const val ACTION_PAUSE_ALL = "com.example.torrent.PAUSE_ALL"
        const val ACTION_RESUME_ALL = "com.example.torrent.RESUME_ALL"
        private const val CHANNEL_TRANSFERS = "volt_transfers"
        private const val CHANNEL_ALERTS = "volt_alerts"
        private const val NOTIFICATION_ID = 4091
    }
}
