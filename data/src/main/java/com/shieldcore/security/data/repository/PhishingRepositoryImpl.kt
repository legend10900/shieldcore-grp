package com.shieldcore.security.data.repository

import com.shieldcore.security.domain.model.*
import com.shieldcore.security.domain.repository.PhishingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
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
        "account-verification-alert.online",
        "indiapost-tracking.xyz",
        "indiapost-update.top",
        "sbi-yono-kyc.com",
        "sbi-pan-update.net",
        "hdfc-netbanking-alert.xyz",
        "electricity-bill-pay.site",
        "paytm-refund-claim.top",
        "fedex-delivery-customs.xyz",
        "usps-redelivery-held.top",
        "telegram-vip-earnings.xyz"
    )

    private val suspiciousTlds = setOf(
        "xyz", "top", "work", "tk", "ml", "ga", "cf", "gq", "fit", "buzz", "click", "country", "kim", "science", "live", "site", "online", "club"
    )

    private val brandImpersonations = listOf(
        "paypal-login", "apple-id-verify", "bankofamerica-secure", "netflix-update",
        "chase-verify", "wellsfargo-secure", "google-security-verify", "metamask-support",
        "binance-withdraw", "coinbase-login", "microsoft-security-alert", "indiapost-",
        "sbi-kyc", "hdfc-alert", "icici-reward", "electricity-board", "paytm-cashback",
        "phonepe-reward", "gpay-refund", "amazon-winner", "fedex-tracking"
    )

    // Regex for URL extraction
    private val urlPattern = Pattern.compile(
        "((https?|ftp)://|(www|t\\.me|wa\\.me|bit\\.ly|tinyurl\\.com|is\\.gd)/)[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for UPI intent URI or VPA handles (e.g., test@okhdfcbank, upi://pay?pa=...)
    private val upiUriPattern = Pattern.compile("upi://pay\\?[^\\s]+", Pattern.CASE_INSENSITIVE)
    private val vpaPattern = Pattern.compile("[a-zA-Z0-9.\\-_]+@(okaxis|okhdfcbank|okicici|oksbi|paytm|ybl|ibl|axl|upi|apl)", Pattern.CASE_INSENSITIVE)

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
        if (suspiciousTlds.contains(tld) && (domain.contains("login") || domain.contains("verify") || domain.contains("account") || domain.contains("bank") || domain.contains("wallet") || domain.contains("track") || domain.contains("pay") || domain.contains("kyc"))) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = true, threatType = "Suspicious TLD Fraud Portal", detectionSource = "Heuristic Analyzer")
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

    override suspend fun verifyUpiPayment(upiUriOrVpa: String): UpiVerificationResult = withContext(Dispatchers.IO) {
        val input = upiUriOrVpa.trim()

        if (input.startsWith("upi://pay", ignoreCase = true)) {
            var payeeAddress: String? = null
            var payeeName: String? = null
            var amount: String? = null
            var note: String? = null

            try {
                val query = input.substringAfter("?", "")
                val pairs = query.split("&")
                for (pair in pairs) {
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].lowercase()
                        val value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name())
                        when (key) {
                            "pa" -> payeeAddress = value
                            "pn" -> payeeName = value
                            "am" -> amount = value
                            "tn" -> note = value
                        }
                    }
                }
            } catch (_: Exception) {}

            val deceptivePayeeNames = listOf("cashback", "refund", "prize", "winner", "reward", "customer support", "kyc update", "lottery")
            val isDeceptiveName = payeeName != null && deceptivePayeeNames.any { payeeName.contains(it, ignoreCase = true) }

            val isDangerous = true // Clicking ANY upi://pay intent in SMS initiates a money TRANSFER out of the user's account!
            val warning = "⚠️ HIGH RISK: This link will INITIATE A PAYMENT OUT of your bank account. Entering your UPI PIN will DEDUCT money."
            val explanation = "Scammers send 'upi://pay' payment links falsely claiming you are receiving a refund or cashback. In reality, UPI NEVER requires a PIN or payment link to receive funds."

            return@withContext UpiVerificationResult(
                rawInput = input,
                payeeAddress = payeeAddress,
                payeeName = payeeName,
                amount = amount,
                note = note,
                isDangerousTrap = isDangerous,
                warningMessage = if (isDeceptiveName) "Deceptive Payee Name: '$payeeName' impersonates refund/prize support!" else warning,
                explanation = explanation
            )
        } else if (vpaPattern.matcher(input).find()) {
            return@withContext UpiVerificationResult(
                rawInput = input,
                payeeAddress = input,
                payeeName = null,
                amount = null,
                note = null,
                isDangerousTrap = false,
                warningMessage = "Valid UPI VPA format. Only send funds to verified recipients.",
                explanation = "UPI handles allow direct money transfers. Verify the recipient identity before approving payment requests in your UPI app."
            )
        }

        UpiVerificationResult(
            rawInput = input,
            payeeAddress = null,
            payeeName = null,
            amount = null,
            note = null,
            isDangerousTrap = false,
            warningMessage = null,
            explanation = "Standard text with no active UPI payment intent detected."
        )
    }

    override suspend fun analyzeMessage(text: String, sender: String?): FraudAnalysisReport = withContext(Dispatchers.IO) {
        val lowerText = text.lowercase()
        val highlighted = mutableListOf<String>()
        val technical = mutableListOf<String>()
        var score = 0
        var category = ScamCategory.SAFE

        // 1. Extract URLs
        val extractedUrls = mutableListOf<String>()
        val urlMatcher = urlPattern.matcher(text)
        while (urlMatcher.find()) {
            extractedUrls.add(urlMatcher.group())
        }

        // 2. Extract UPI handles and links
        val extractedUpi = mutableListOf<String>()
        val upiMatcher = upiUriPattern.matcher(text)
        while (upiMatcher.find()) {
            extractedUpi.add(upiMatcher.group())
        }
        val vpaMatcher = vpaPattern.matcher(text)
        while (vpaMatcher.find()) {
            extractedUpi.add(vpaMatcher.group())
        }

        // 3. Scam Signature Classifiers
        // A. Fake Courier / Delivery Trap
        val deliveryKeywords = listOf("delivery held", "package pending", "parcel blocked", "incomplete address", "update address", "indiapost", "india post", "fedex", "bluedart", "dhl", "usps", "customs fee", "redelivery")
        val matchedDelivery = deliveryKeywords.filter { lowerText.contains(it) }
        if (matchedDelivery.isNotEmpty() && extractedUrls.isNotEmpty()) {
            category = ScamCategory.FAKE_DELIVERY
            score += 85
            highlighted.addAll(matchedDelivery)
            technical.add("Delivery Scam Signature: Urgency + postal impersonation with external address update link")
        }

        // B. UPI Payment / Cashback Trap
        val upiKeywords = listOf("cashback received", "congratulations won", "claim reward", "enter pin to receive", "refund approved", "scratch card", "upi://pay", "click to claim")
        val matchedUpi = upiKeywords.filter { lowerText.contains(it) }
        if (matchedUpi.isNotEmpty() || extractedUpi.any { it.startsWith("upi://pay") }) {
            category = ScamCategory.UPI_PAYMENT_TRAP
            score += 90
            highlighted.addAll(matchedUpi)
            technical.add("UPI PIN Trap: Deceptive refund/reward phrasing designed to trick user into authorizing debit")
        }

        // C. Bank KYC / Account Freeze
        val bankKeywords = listOf("account blocked", "netbanking suspended", "update kyc", "pan card expired", "sbi yono", "hdfc netbanking", "icici alert", "axis bank alert", "within 24 hours", "debit card blocked")
        val matchedBank = bankKeywords.filter { lowerText.contains(it) }
        if (matchedBank.isNotEmpty()) {
            if (category == ScamCategory.SAFE || score < 90) {
                category = ScamCategory.BANK_IMPERSONATION
            }
            score += 90
            highlighted.addAll(matchedBank)
            technical.add("Bank KYC Impersonation: High-urgency intimidation demanding credential/PAN submission")
        }

        // D. Electricity / Utility Disconnection Scam
        val utilityKeywords = listOf("electricity power will be disconnected", "power cutoff", "unpaid electricity bill", "bill not updated", "call electricity officer", "disconnect tonight", "urgently call")
        val matchedUtility = utilityKeywords.filter { lowerText.contains(it) }
        if (matchedUtility.isNotEmpty()) {
            if (category == ScamCategory.SAFE || score < 85) {
                category = ScamCategory.UTILITY_BILL_SCAM
            }
            score += 85
            highlighted.addAll(matchedUtility)
            technical.add("Utility Cutoff Threat: Fake disconnection ultimatum directing victim to call scammer")
        }

        // E. Lottery / Prize Scam
        val lotteryKeywords = listOf("won 25,00,000", "lottery winner", "kbc prize", "lucky draw", "car winner", "claim money immediately")
        val matchedLottery = lotteryKeywords.filter { lowerText.contains(it) }
        if (matchedLottery.isNotEmpty()) {
            if (category == ScamCategory.SAFE) {
                category = ScamCategory.LOTTERY_PRIZE
            }
            score += 80
            highlighted.addAll(matchedLottery)
            technical.add("Advance Fee / Lottery Fraud: Fabricated prize lure")
        }

        // F. Work-From-Home / Task Scam
        val jobKeywords = listOf("earn 5000 daily", "part-time job", "like youtube videos", "telegram task", "daily payout", "hotel review job")
        val matchedJob = jobKeywords.filter { lowerText.contains(it) }
        if (matchedJob.isNotEmpty()) {
            if (category == ScamCategory.SAFE) {
                category = ScamCategory.PART_TIME_JOB
            }
            score += 75
            highlighted.addAll(matchedJob)
            technical.add("Task Scam Signature: High-payout easy work lure leading to cryptocurrency/deposit trap")
        }

        // G. Malicious APK Link
        if (lowerText.contains(".apk") || lowerText.contains("download app to fix") || lowerText.contains("quicksupport") || lowerText.contains("anydesk")) {
            category = ScamCategory.MALICIOUS_APK
            score += 95
            highlighted.add(".apk / Remote Access link")
            technical.add("Remote Access / Trojan APK: Direct prompt to sideload unauthorized Android package")
        }

        // 4. URL deep evaluation
        val urlReports = mutableListOf<PhishingUrl>()
        for (u in extractedUrls) {
            val rep = checkUrl(u)
            urlReports.add(rep)
            if (rep.isMalicious) {
                score += 30
                if (category == ScamCategory.SAFE) {
                    category = ScamCategory.SUSPICIOUS_LINK
                }
                technical.add("Flagged Domain: ${rep.url} (${rep.threatType})")
            }
        }

        // 5. UPI deep evaluation
        val upiReports = mutableListOf<UpiVerificationResult>()
        for (u in extractedUpi) {
            val rep = verifyUpiPayment(u)
            upiReports.add(rep)
            if (rep.isDangerousTrap) {
                score += 40
                category = ScamCategory.UPI_PAYMENT_TRAP
                technical.add("Active upi://pay Payment Intent: ${rep.payeeAddress ?: "Unknown Payee"}")
            }
        }

        val clampedScore = score.coerceIn(0, 100)
        val isScam = clampedScore >= 50

        // 6. Senior-Friendly Plain Language Advice Generation
        val seniorAdvice = when (category) {
            ScamCategory.FAKE_DELIVERY -> 
                "🛑 DO NOT CLICK OR PAY! Genuine postal and courier services (like India Post or FedEx) NEVER ask for address updates or small ₹5-₹20 fees via SMS. Your parcel is not in danger."
            ScamCategory.UPI_PAYMENT_TRAP -> 
                "⚠️ NEVER ENTER YOUR PIN! You only enter your UPI PIN when PAYING money. You NEVER need a PIN to receive cashback or refunds. Entering your PIN will DEDUCT money from your account."
            ScamCategory.BANK_IMPERSONATION -> 
                "🛑 FAKE BANK ALERT: Banks NEVER threaten account suspension or ask for PAN/KYC updates via SMS links. Do not click. If worried, visit your physical bank branch."
            ScamCategory.UTILITY_BILL_SCAM -> 
                "🛑 FAKE DISCONNECTION THREAT: Electricity boards do not send threats from personal mobile numbers. Do not call the number listed in this message. Your power will NOT be cut off."
            ScamCategory.LOTTERY_PRIZE -> 
                "🛑 100% FAKE LOTTERY: Legitimate organizations never give prizes for competitions you did not enter. Scammers will ask for an 'advance processing fee' and steal your money."
            ScamCategory.PART_TIME_JOB -> 
                "⚠️ FRAUD WARNING: No legitimate company pays thousands per day for simply liking videos. They will ask you to deposit money in Telegram groups and steal it."
            ScamCategory.MALICIOUS_APK -> 
                "🛑 HIGH DANGER: NEVER install apps or .apk files from message links. These apps can read your bank OTPs, record your screen, and empty your bank account."
            ScamCategory.SUSPICIOUS_LINK -> 
                "⚠️ SUSPICIOUS WEBSITE: The web link in this message leads to an unverified or high-risk domain. Do not enter passwords, phone numbers, or credit card details."
            ScamCategory.SAFE -> 
                "✅ SAFE: This message appears legitimate and does not contain known scam urgency tactics, payment traps, or deceptive links."
        }

        FraudAnalysisReport(
            rawText = text,
            sender = sender,
            isScam = isScam,
            riskScore = clampedScore,
            category = category,
            highlightedKeywords = highlighted.distinct(),
            extractedUrls = extractedUrls.distinct(),
            extractedUpiHandles = extractedUpi.distinct(),
            urlReports = urlReports,
            upiReports = upiReports,
            seniorAdvice = seniorAdvice,
            technicalDetails = technical.distinct()
        )
    }
}
