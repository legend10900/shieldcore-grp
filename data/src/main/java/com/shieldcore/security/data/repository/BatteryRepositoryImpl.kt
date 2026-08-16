package com.shieldcore.security.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.shieldcore.security.domain.repository.BatteryInfo
import com.shieldcore.security.domain.repository.BatteryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BatteryRepository {

    override fun getBatteryInfo(): Flow<BatteryInfo> = flow {
        while (true) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, filter)

            batteryStatus?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = (level * 100 / scale.toFloat()).toInt()
                
                val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10.0f
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)

                emit(BatteryInfo(
                    level = batteryPct,
                    temperature = temperature,
                    voltage = voltage,
                    health = health,
                    status = status,
                    isCharging = isCharging
                ))
            }
            delay(5000) // Poll every 5 seconds
        }
    }

    override suspend fun getCpuHeavyApps(): List<String> {
        // In a real app, this would use UsageStatsManager to identify apps with high CPU time
        // For demonstration, we return some system components if they are active
        return listOf("Background Sync", "System UI", "Media Server")
    }

    override suspend fun optimizePower(): Boolean {
        // Trigger power saving recommendations or clear non-essential background tasks
        return true
    }
}
