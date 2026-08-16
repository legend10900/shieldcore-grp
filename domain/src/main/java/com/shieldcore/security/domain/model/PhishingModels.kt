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
