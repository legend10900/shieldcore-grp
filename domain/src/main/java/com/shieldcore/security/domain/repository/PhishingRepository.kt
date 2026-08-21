package com.shieldcore.security.domain.repository

import com.shieldcore.security.domain.model.FraudAnalysisReport
import com.shieldcore.security.domain.model.LinkSafetyStatus
import com.shieldcore.security.domain.model.PhishingUrl
import com.shieldcore.security.domain.model.UpiVerificationResult

interface PhishingRepository {
    /**
     * Checks if a URL is malicious against local and remote databases.
     */
    suspend fun checkUrl(url: String): PhishingUrl

    /**
     * Adds a URL to the local whitelist or blacklist.
     */
    suspend fun markUrlSafety(url: String, status: LinkSafetyStatus)

    /**
     * Analyzes incoming message / SMS text for scams, payment traps, and deceptive URLs.
     */
    suspend fun analyzeMessage(text: String, sender: String? = null): FraudAnalysisReport

    /**
     * Inspects UPI payment intent string or VPA address for deceptive traps.
     */
    suspend fun verifyUpiPayment(upiUriOrVpa: String): UpiVerificationResult
}
