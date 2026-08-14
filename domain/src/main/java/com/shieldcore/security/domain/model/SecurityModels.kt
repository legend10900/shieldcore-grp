package com.shieldcore.security.domain.model

import java.util.Date

/**
 * Represents the risk level of a scanned item.
 */
enum class RiskLevel {
    SAFE,
    SUSPICIOUS,
    MALICIOUS,
    UNKNOWN
}

/**
 * Result of an antivirus scan on a file or package.
 */
data class ScanResult(
    val id: String,
    val label: String,
    val packageName: String?,
    val filePath: String,
    val riskLevel: RiskLevel,
    val threatName: String? = null,
    val hash: String? = null,
    val scanTime: Long = System.currentTimeMillis()
)

/**
 * Rule/Signature for matching threats.
 */
data class SecuritySignature(
    val id: String,
    val name: String,
    val pattern: String,
    val riskLevel: RiskLevel = RiskLevel.MALICIOUS
)

/**
 * Summary of a full device scan.
 */
data class ScanSummary(
    val totalFilesScanned: Int,
    val threatsFound: Int,
    val startTime: Long,
    val endTime: Long,
    val status: ScanStatus = ScanStatus.COMPLETED
)

enum class ScanStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    CANCELLED
}
