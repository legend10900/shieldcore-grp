package com.shieldcore.security.data.repository

import com.shieldcore.security.domain.model.LinkSafetyStatus
import com.shieldcore.security.domain.model.PhishingUrl
import com.shieldcore.security.domain.repository.PhishingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhishingRepositoryImpl @Inject constructor() : PhishingRepository {

    private val blacklist = setOf(
        "malicious-site.com",
        "phishing-example.net",
        "steal-credentials.org"
    )

    override suspend fun checkUrl(url: String): PhishingUrl = withContext(Dispatchers.IO) {
        val domain = url.lowercase().removePrefix("http://").removePrefix("https://").split("/").first()
        val isMalicious = blacklist.any { domain.contains(it) }

        PhishingUrl(
            url = url,
            isMalicious = isMalicious,
            threatType = if (isMalicious) "Phishing/Malware" else null,
            detectionSource = "ShieldCore Local Bloom Filter"
        )
    }

    override suspend fun markUrlSafety(url: String, status: LinkSafetyStatus) {
        // Implementation for local database storage
    }
}
