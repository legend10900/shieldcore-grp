package com.shieldcore.security

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.shieldcore.security.service.ScannerWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ShieldCoreApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleBackgroundScan()
        startRealtimeProtection()
    }

    private fun startRealtimeProtection() {
        try {
            val intent = android.content.Intent(this, com.shieldcore.security.service.RealtimeShieldService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) {}
    }

    private fun scheduleBackgroundScan() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicScanRequest = PeriodicWorkRequestBuilder<ScannerWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ShieldCoreDailyScan",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicScanRequest
        )
    }
}
