package com.shieldcore.security.domain.repository

import com.shieldcore.security.domain.model.BatteryAudit
import com.shieldcore.security.domain.model.BreachRecord
import kotlinx.coroutines.flow.Flow

interface SecurityAuditRepository {
    /**
     * Checks if an email has been involved in a data breach using k-Anonymity.
     */
    suspend fun checkDataBreach(email: String): List<BreachRecord>

    /**
     * Gets current battery health and optimization status.
     */
    fun getBatteryAudit(): Flow<BatteryAudit>

    /**
     * Performs battery optimization steps.
     */
    suspend fun optimizeBattery()
}
