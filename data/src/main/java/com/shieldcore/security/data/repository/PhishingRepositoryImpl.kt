package com.shieldcore.security.data.repository

import com.shieldcore.security.domain.model.LinkSafetyStatus
import com.shieldcore.security.domain.model.PhishingUrl
import com.shieldcore.security.domain.repository.PhishingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhishingRepositoryImpl @Inject constructor() : PhishingRepository {

    private val userMarkedStatus = ConcurrentHashMap<String, LinkSafetyStatus>()

    private val staticBlacklist = setOf(
        "malicious-site.com",
        "phishing-example.net",
        "steal-credentials.org",
        "free-crypto-giveaway.xyz",
        "secure-login-update.top",
        "account-verification-alert.online"
    )

    private val suspiciousTlds = setOf(
        "xyz", "top", "work", "tk", "ml", "ga", "cf", "gq", "fit", "buzz", "click", "country", "kim", "science"
    )

    private val brandImpersonations = listOf(
        "paypal-login", "apple-id-verify", "bankofamerica-secure", "netflix-update",
        "chase-verify", "wellsfargo-secure", "google-security-verify", "metamask-support",
        "binance-withdraw", "coinbase-login", "microsoft-security-alert"
    )

    override suspend fun checkUrl(url: String): PhishingUrl = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        val domain = cleanUrl.lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .split("/")
            .first()
            .split(":")
            .first()

        // 1. Check user override
        if (userMarkedStatus[domain] == LinkSafetyStatus.SAFE) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = false, detectionSource = "User Whitelist")
        }
        if (userMarkedStatus[domain] == LinkSafetyStatus.PHISHING || userMarkedStatus[domain] == LinkSafetyStatus.MALWARE) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = true, threatType = "User Blacklisted", detectionSource = "User Custom Rules")
        }

        // 2. Blacklist check
        if (staticBlacklist.any { domain.contains(it) }) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = true, threatType = "Known Phishing Domain", detectionSource = "ShieldCore Threat Intelligence")
        }

        // 3. Heuristic: Direct IP address host
        val isDirectIp = domain.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$".toRegex())
        if (isDirectIp && !domain.startsWith("192.168.") && !domain.startsWith("10.") && !domain.startsWith("127.")) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = true, threatType = "Suspicious Direct-IP URL", detectionSource = "Heuristic Analyzer")
        }

        // 4. Heuristic: Brand impersonation keywords
        for (pattern in brandImpersonations) {
            if (domain.contains(pattern)) {
                return@withContext PhishingUrl(cleanUrl, isMalicious = true, threatType = "Brand Impersonation / Typosquatting", detectionSource = "Heuristic Analyzer")
            }
        }

        // 5. Heuristic: Suspicious TLD combined with sensitive terms
        val tld = domain.substringAfterLast(".", "")
        if (suspiciousTlds.contains(tld) && (domain.contains("login") || domain.contains("verify") || domain.contains("account") || domain.contains("bank") || domain.contains("wallet"))) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = true, threatType = "Suspicious TLD Credential Harvester", detectionSource = "Heuristic Analyzer")
        }

        // 6. Suspicious credential embedding
        if (cleanUrl.contains("@") && cleanUrl.startsWith("http")) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = true, threatType = "Credential Injection URL Obfuscation", detectionSource = "Heuristic Analyzer")
        }

        PhishingUrl(cleanUrl, isMalicious = false, detectionSource = "ShieldCore Engine (Safe)")
    }

    override suspend fun markUrlSafety(url: String, status: LinkSafetyStatus) = withContext(Dispatchers.IO) {
        val domain = url.trim().lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .split("/")
            .first()
            .split(":")
            .first()
        userMarkedStatus[domain] = status
    }
}
