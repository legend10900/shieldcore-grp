package com.shieldcore.security.domain.repository

import com.shieldcore.security.domain.model.CleanSummary
import com.shieldcore.security.domain.model.JunkItem
import kotlinx.coroutines.flow.Flow

interface JunkCleanerRepository {
    /**
     * Scans the device for junk files.
     */
    fun scanForJunk(): Flow<JunkScanProgress>

    /**
     * Cleans selected junk items.
     */
    suspend fun cleanJunk(items: List<JunkItem>): CleanSummary

    /**
     * Triggers the automated cache cleaning via Accessibility Service.
     */
    suspend fun startAutomatedCacheClean()
}

sealed class JunkScanProgress {
    object Idle : JunkScanProgress()
    data class Scanning(val currentDir: String, val currentSize: Long) : JunkScanProgress()
    data class Completed(val items: List<JunkItem>) : JunkScanProgress()
}
