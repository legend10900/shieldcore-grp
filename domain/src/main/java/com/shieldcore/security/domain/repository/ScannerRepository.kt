package com.shieldcore.security.domain.repository

import com.shieldcore.security.domain.model.ScanResult
import com.shieldcore.security.domain.model.SecuritySignature
import com.shieldcore.security.domain.model.ScanSummary
import kotlinx.coroutines.flow.Flow

interface ScannerRepository {
    suspend fun scanFile(filePath: String, signatures: List<SecuritySignature>): ScanResult
    suspend fun scanPackage(packageName: String): ScanResult
    fun scanAllInstalledApps(): Flow<ScanProgress>
    fun getScanHistory(): Flow<List<ScanSummary>>
    suspend fun removeThreat(packageName: String): Boolean
}

sealed class ScanProgress {
    object Idle : ScanProgress()
    data class Running(
        val currentApp: String,
        val scannedCount: Int,
        val totalCount: Int,
        val threatsFound: List<ScanResult>
    ) : ScanProgress()
    data class Completed(val summary: ScanSummary, val threats: List<ScanResult>) : ScanProgress()
    data class Error(val message: String) : ScanProgress()
}
