package com.shieldcore.security.domain.model

data class PhishingUrl(
    val url: String,
    val isMalicious: Boolean,
    val threatType: String? = null,
    val detectionSource: String? = null
)

enum class LinkSafetyStatus {
    SAFE,
    PHISHING,
    MALWARE,
    SUSPICIOUS,
    UNKNOWN
}

enum class ScamCategory(val displayName: String, val severityLevel: String) {
    SAFE("Legitimate Message", "SAFE"),
    FAKE_DELIVERY("Fake Courier / Delivery Scam", "HIGH"),
    UPI_PAYMENT_TRAP("UPI PIN & Payment Trap", "CRITICAL"),
    BANK_IMPERSONATION("Bank KYC / Account Freeze Scam", "CRITICAL"),
    UTILITY_BILL_SCAM("Electricity / Utility Disconnection Threat", "HIGH"),
    LOTTERY_PRIZE("Fake Lottery / Prize Winner Scam", "HIGH"),
    PART_TIME_JOB("Work-From-Home / Task Scam", "MEDIUM"),
    MALICIOUS_APK("Malware / Remote Access App Link", "CRITICAL"),
    SUSPICIOUS_LINK("Suspicious Web Domain", "MEDIUM")
}

data class UpiVerificationResult(
    val rawInput: String,
    val payeeAddress: String?,
    val payeeName: String?,
    val amount: String?,
    val note: String?,
    val isDangerousTrap: Boolean,
    val warningMessage: String?,
    val explanation: String
)

data class FraudAnalysisReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rawText: String,
    val sender: String? = null,
    val isScam: Boolean,
    val riskScore: Int, // 0 (100% safe) to 100 (high danger)
    val category: ScamCategory,
    val highlightedKeywords: List<String> = emptyList(),
    val extractedUrls: List<String> = emptyList(),
    val extractedUpiHandles: List<String> = emptyList(),
    val urlReports: List<PhishingUrl> = emptyList(),
    val upiReports: List<UpiVerificationResult> = emptyList(),
    val seniorAdvice: String,
    val technicalDetails: List<String> = emptyList()
)
