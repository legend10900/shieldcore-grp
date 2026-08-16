package com.shieldcore.security.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.shieldcore.security.domain.repository.ScanProgress
import com.shieldcore.security.domain.repository.ScannerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ScannerWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ScannerRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "shieldcore_antivirus_channel"
        const val NOTIFICATION_ID_RUNNING = 3001
        const val NOTIFICATION_ID_COMPLETED = 3002
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ShieldCore Antivirus Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background antivirus scanning notifications and threat alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val initialNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("ShieldCore Background Antivirus")
                .setContentText("Scanning installed applications for malware...")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .build()

            try {
                setForeground(ForegroundInfo(NOTIFICATION_ID_RUNNING, initialNotification))
            } catch (_: Exception) {}

            repository.scanAllInstalledApps().collect { progress ->
                when (progress) {
                    is ScanProgress.Running -> {
                        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setContentTitle("ShieldCore Antivirus Scan")
                            .setContentText("Scanning ${progress.currentApp} (${progress.scannedCount}/${progress.totalCount})")
                            .setSmallIcon(android.R.drawable.ic_popup_sync)
                            .setOngoing(true)
                            .setProgress(progress.totalCount, progress.scannedCount, false)
                            .build()
                        notificationManager.notify(NOTIFICATION_ID_RUNNING, notification)
                    }
                    is ScanProgress.Completed -> {
                        notificationManager.cancel(NOTIFICATION_ID_RUNNING)
                        val threats = progress.summary.threatsFound
                        val completedNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                            .setContentTitle(if (threats > 0) "⚠️ Malware Alert: $threats Threats Found" else "✅ ShieldCore Scan Complete")
                            .setContentText(
                                if (threats > 0)
                                    "Identified $threats potentially malicious package(s). Tap to review."
                                else
                                    "Scanned ${progress.summary.totalFilesScanned} apps. Device is secure."
                            )
                            .setSmallIcon(if (threats > 0) android.R.drawable.stat_sys_warning else android.R.drawable.ic_lock_idle_lock)
                            .setAutoCancel(true)
                            .setPriority(if (threats > 0) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                            .build()
                        notificationManager.notify(NOTIFICATION_ID_COMPLETED, completedNotification)
                    }
                    else -> {}
                }
            }
            Result.success()
        } catch (e: Exception) {
            notificationManager.cancel(NOTIFICATION_ID_RUNNING)
            Result.retry()
        }
    }
}
