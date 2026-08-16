package com.shieldcore.security.domain.repository

import kotlinx.coroutines.flow.Flow

data class BatteryInfo(
    val level: Int,
    val temperature: Float,
    val voltage: Int,
    val health: Int,
    val status: Int,
    val isCharging: Boolean
)

interface BatteryRepository {
    fun getBatteryInfo(): Flow<BatteryInfo>
    suspend fun getCpuHeavyApps(): List<String>
    suspend fun optimizePower(): Boolean
}
