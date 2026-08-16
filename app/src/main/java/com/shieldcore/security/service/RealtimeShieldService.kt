package com.shieldcore.security.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shieldcore.security.domain.model.RiskLevel
import com.shieldcore.security.domain.repository.ScannerRepository
import com.shieldcore.security.presentation.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RealtimeShieldService : Service() {

    @Inject
    lateinit var scannerRepository: ScannerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadObserver: FileObserver? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_PACKAGE_ADDED) {
                val pkgName = intent.data?.schemeSpecificPart ?: return
                serviceScope.launch {
                    val result = scannerRepository.scanPackage(pkgName)
                    if (result.riskLevel == RiskLevel.MALICIOUS) {
                        notifyThreatDetected(result.label, result.packageName ?: pkgName)
                    }
                }
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = 4001
        const val CHANNEL_ID = "shieldcore_realtime_channel"
        const val ACTION_START = "com.shieldcore.security.action.START_REALTIME"
        const val ACTION_STOP = "com.shieldcore.security.action.STOP_REALTIME"
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
        startWatchingDownloads()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createPersistentNotification())
        return START_STICKY
    }

    private fun startWatchingDownloads() {
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null && downloadDir.exists()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    downloadObserver = object : FileObserver(downloadDir, CLOSE_WRITE or MOVED_TO) {
                        override fun onEvent(event: Int, path: String?) {
                            if (path != null && (path.endsWith(".apk", ignoreCase = true) || path.endsWith(".tmp", ignoreCase = true))) {
                                val targetFile = File(downloadDir, path)
                                serviceScope.launch {
                                    val result = scannerRepository.scanFile(targetFile.absolutePath, emptyList())
                                    if (result.riskLevel == RiskLevel.MALICIOUS) {
                                        notifyThreatDetected(result.label, targetFile.absolutePath)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    downloadObserver = object : FileObserver(downloadDir.absolutePath, CLOSE_WRITE or MOVED_TO) {
                        override fun onEvent(event: Int, path: String?) {
                            if (path != null && path.endsWith(".apk", ignoreCase = true)) {
                                val targetFile = File(downloadDir, path)
                                serviceScope.launch {
                                    val result = scannerRepository.scanFile(targetFile.absolutePath, emptyList())
                                    if (result.riskLevel == RiskLevel.MALICIOUS) {
                                        notifyThreatDetected(result.label, targetFile.absolutePath)
                                    }
                                }
                            }
                        }
                    }
                }
                downloadObserver?.startWatching()
            }
        } catch (_: Exception) {}
    }

    private fun createPersistentNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Real-Time Antivirus Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors newly installed packages and file downloads continuously"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ShieldCore Real-Time Shield Active")
            .setContentText("Continuous protection against malware, viruses, and phishing")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun notifyThreatDetected(label: String, target: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ Malware Threat Detected!")
            .setContentText("$label is malicious and poses a security hazard to your device.")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(packageReceiver)
        } catch (_: Exception) {}
        try {
            downloadObserver?.stopWatching()
        } catch (_: Exception) {}
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
