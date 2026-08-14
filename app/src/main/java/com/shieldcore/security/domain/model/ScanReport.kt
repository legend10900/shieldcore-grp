package com.shieldcore.security.domain.model

data class ThreatInfo(
    val packageName: String,
    val appName: String,
    val apkPath: String,
    val threatName: String,
    val threatLevel: ThreatLevel,
    val sha256Digest: String,
    val detectionSource: String
)

enum class ThreatLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class ScanReport(
    val id: Long = 0,
    val timestamp: Long,
    val totalAppsScanned: Int,
    val threatsDetected: List<ThreatInfo>,
    val durationMs: Long,
    val isClean: Boolean = threatsDetected.isEmpty()
)

data class InstalledAppDetails(
    val packageName: String,
    val appName: String,
    val apkPath: String,
    val versionName: String,
    val isSystemApp: Boolean,
    val installedTimestamp: Long
)
