package com.shieldcore.security.domain.repository

import com.shieldcore.security.domain.model.InstalledAppDetails
import com.shieldcore.security.domain.model.ScanReport
import com.shieldcore.security.domain.model.ThreatInfo
import kotlinx.coroutines.flow.Flow

interface AntivirusRepository {
    fun scanAllInstalledAppsProgress(): Flow<ScanProgressState>
    suspend fun scanSinglePackage(packageName: String): ThreatInfo?
    suspend fun getInstalledApps(): List<InstalledAppDetails>
    fun getScanHistory(): Flow<List<ScanReport>>
    suspend fun saveScanReport(report: ScanReport)
    suspend fun removeThreat(packageName: String): Boolean
}

sealed class ScanProgressState {
    object Idle : ScanProgressState()
    data class Scanning(
        val currentApp: String,
        val scannedCount: Int,
        val totalCount: Int,
        val currentThreats: List<ThreatInfo>
    ) : ScanProgressState()
    data class Completed(val report: ScanReport) : ScanProgressState()
    data class Error(val message: String) : ScanProgressState()
}
