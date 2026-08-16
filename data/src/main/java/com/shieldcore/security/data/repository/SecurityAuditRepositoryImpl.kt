package com.shieldcore.security.data.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.shieldcore.security.domain.model.BatteryAudit
import com.shieldcore.security.domain.model.BreachRecord
import com.shieldcore.security.domain.repository.SecurityAuditRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityAuditRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SecurityAuditRepository {

    override suspend fun checkDataBreach(email: String): List<BreachRecord> = withContext(Dispatchers.IO) {
        val sha1 = MessageDigest.getInstance("SHA-1").digest(email.toByteArray())
            .joinToString("") { "%02x".format(it) }
        val prefix = sha1.take(5)
        
        // Mocking API call to HIBP with k-Anonymity
        if (email.contains("leak")) {
            listOf(BreachRecord("Mock Breach", "2024-01-01", "A mock breach for testing.", listOf("Email", "Passwords")))
        } else {
            emptyList()
        }
    }

    override fun getBatteryAudit(): Flow<BatteryAudit> = callbackFlow {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
                val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    else -> "Check Required"
                }

                trySend(BatteryAudit(level, temp, voltage, health, "Normal", level > 80))
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    override suspend fun optimizeBattery() {
        // Implementation for dimming brightness or stopping background polling
    }
}
