package com.shieldcore.security.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_reports")
data class ScanReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val totalAppsScanned: Int,
    val threatCount: Int,
    val threatsJson: String, // JSON representation of detected threats
    val durationMs: Long,
    val isClean: Boolean
)
