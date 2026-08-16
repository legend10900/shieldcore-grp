package com.shieldcore.security.domain.repository

import com.shieldcore.security.domain.model.LinkSafetyStatus
import com.shieldcore.security.domain.model.PhishingUrl

interface PhishingRepository {
    /**
     * Checks if a URL is malicious against local and remote databases.
     */
    suspend fun checkUrl(url: String): PhishingUrl

    /**
     * Adds a URL to the local whitelist or blacklist.
     */
    suspend fun markUrlSafety(url: String, status: LinkSafetyStatus)
}
