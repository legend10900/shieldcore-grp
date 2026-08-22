package com.shieldcore.security.data.repository

import com.shieldcore.security.domain.model.*
import com.shieldcore.security.domain.repository.PhishingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhishingRepositoryImpl @Inject constructor() : PhishingRepository {

    private val userMarkedStatus = ConcurrentHashMap<String, LinkSafetyStatus>()

    // 1. Trusted Whitelist: Official Banking, Government, Courier, and Major Verified Tech Platforms
    private val trustedDomains = setOf(
        // Government & Public Services
        "gov.in", "nic.in", "gov", "mil", "edu", "ac.in",
        "incometax.gov.in", "epfindia.gov.in", "uidai.gov.in", "parivahan.gov.in",
        "passportindia.gov.in", "cybercrime.gov.in", "ceir.gov.in", "sancharsaathi.gov.in",
        "indiapost.gov.in", "rbi.org.in", "npci.org.in",

        // Indian Banks & Financial Institutions
        "sbi.co.in", "onlinesbi.sbi", "statebankofindia.com",
        "hdfcbank.com", "hdfc.com",
        "icicibank.com", "icici.com",
        "axisbank.com", "axisbank.co.in",
        "kotak.com", "kotakbank.com",
        "pnbindia.in", "pnb.bank.in",
        "bankofbaroda.in", "bankofbaroda.com",
        "canarabank.com", "unionbankofindia.co.in",
        "idfcfirstbank.com", "yesbank.in", "indusind.com",
        "federalbank.co.in", "bankofindia.co.in",

        // International Financial & Payments
        "chase.com", "bankofamerica.com", "wellsfargo.com", "citi.com",
        "barclays.co.uk", "hsbc.com", "paypal.com", "stripe.com",
        "binance.com", "coinbase.com", "visa.com", "mastercard.com",

        // Verified Couriers & Logistics
        "bluedart.com", "fedex.com", "dhl.com", "dhl.de",
        "delhivery.com", "dtdc.in", "ekartlogistics.com",
        "shadowfax.in", "usps.com", "ups.com", "shiprocket.in",

        // Major Verified E-Commerce, Tech & Utilities
        "amazon.in", "amazon.com", "flipkart.com", "myntra.com",
        "swiggy.com", "zomato.com", "paytm.com", "phonepe.com",
        "google.com", "google.co.in", "apple.com", "microsoft.com",
        "whatsapp.com", "telegram.org", "youtube.com", "github.com",
        "meta.com", "instagram.com", "netflix.com", "spotify.com"
    )

    // 2. Known Malicious / Phishing Blacklist
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
        "telegram-vip-earnings.xyz",
        "gpay-reward-claim.top",
        "phonepe-cashback2024.xyz",
        "whatsapp-gift-voucher.top",
        "meta-security-review.xyz"
    )

    // 3. High-Abuse TLDs frequently utilized in automated phishing kit campaigns
    private val suspiciousTlds = setOf(
        "xyz", "top", "work", "tk", "ml", "ga", "cf", "gq", "fit", "buzz", "click", "country", "kim", "science", "live", "site", "online", "club", "rest", "surf", "loan"
    )

    // 4. Typosquatting / Brand Impersonation patterns
    private val brandImpersonations = listOf(
        "paypal-login", "apple-id-verify", "bankofamerica-secure", "netflix-update",
        "chase-verify", "wellsfargo-secure", "google-security-verify", "metamask-support",
        "binance-withdraw", "coinbase-login", "microsoft-security-alert", "indiapost-",
        "sbi-kyc", "sbi-yono", "hdfc-netbanking", "hdfc-alert", "icici-reward",
        "axisbank-kyc", "electricity-board", "paytm-cashback", "phonepe-reward",
        "gpay-refund", "amazon-winner", "fedex-tracking"
    )

    // Regex for extracting URLs
    private val urlPattern = Pattern.compile(
        "((https?|ftp)://|(www|t\\.me|wa\\.me|bit\\.ly|tinyurl\\.com|is\\.gd)/)[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for UPI intent URI or VPA handles (e.g., test@okhdfcbank, upi://pay?pa=...)
    private val upiUriPattern = Pattern.compile("upi://pay\\?[^\\s]+", Pattern.CASE_INSENSITIVE)
    private val vpaPattern = Pattern.compile("[a-zA-Z0-9.\\-_]+@(okaxis|okhdfcbank|okicici|oksbi|paytm|ybl|ibl|axl|upi|apl)", Pattern.CASE_INSENSITIVE)

    // Regex for Indian TRAI Registered DLT Sender Headers (e.g., VM-HDFCBK, AX-AMAZON, VK-INDPOST, BP-SBIBNK)
    private val dltHeaderPattern = Pattern.compile("^[A-Za-z]{2}-[A-Za-z0-9]{3,8}$")
    // Regex for phone numbers (e.g., +919876543210, 9876543210)
    private val phoneSenderPattern = Pattern.compile("^(\\+91|91|0)?[6-9]\\d{9}$")

    /**
     * Extracts and normalizes the effective domain / host from a URL string.
     */
    private fun extractDomain(url: String): String {
        val trimmed = url.trim().lowercase(Locale.ROOT)
        return try {
            val formatted = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                "https://$trimmed"
            } else trimmed
            val uri = URI(formatted)
            val host = uri.host ?: trimmed
            host.removePrefix("www.")
        } catch (_: Exception) {
            trimmed.removePrefix("http://")
                .removePrefix("https://")
                .split("/", "?", "#", ":")
                .first()
                .removePrefix("www.")
        }
    }

    /**
     * Checks whether the domain belongs to the verified trusted whitelist.
     */
    private fun isDomainWhitelisted(domain: String): Boolean {
        val cleanDomain = domain.lowercase(Locale.ROOT)
        return trustedDomains.any { trusted ->
            cleanDomain == trusted || cleanDomain.endsWith(".$trusted")
        }
    }

    override suspend fun checkUrl(url: String): PhishingUrl = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = false, detectionSource = "Empty Input")
        }

        val domain = extractDomain(cleanUrl)

        // 1. User manual overrides
        if (userMarkedStatus[domain] == LinkSafetyStatus.SAFE) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = false, detectionSource = "User Whitelist")
        }
        if (userMarkedStatus[domain] == LinkSafetyStatus.PHISHING || userMarkedStatus[domain] == LinkSafetyStatus.MALWARE) {
            return@withContext PhishingUrl(cleanUrl, isMalicious = true, threatType = "User Blacklisted", detectionSource = "User Custom Rules")
        }

        // 2. Official Trusted Whitelist Check
        if (isDomainWhitelisted(domain)) {
            return@withContext PhishingUrl(
                url = cleanUrl,
                isMalicious = false,
                threatType = null,
                detectionSource = "Verified Trusted Organization ($domain)"
            )
        }

        // 3. Known Phishing Domain Blacklist Check
        if (staticBlacklist.any { domain == it || domain.endsWith(".$it") || it == domain }) {
            return@withContext PhishingUrl(
                url = cleanUrl,
                isMalicious = true,
                threatType = "Known Phishing Domain",
                detectionSource = "ShieldCore Threat Intelligence Database"
            )
        }

        // 4. Heuristic: Direct public IP address host (malware C2 or temporary scam host)
        val isDirectIp = domain.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$".toRegex())
        if (isDirectIp && !domain.startsWith("192.168.") && !domain.startsWith("10.") && !domain.startsWith("127.") && !domain.startsWith("172.")) {
            return@withContext PhishingUrl(
                url = cleanUrl,
                isMalicious = true,
                threatType = "Suspicious Direct-IP Web Host",
                detectionSource = "Heuristic Threat Engine"
            )
        }

        // 5. Heuristic: Brand Impersonation & Typosquatting
        for (pattern in brandImpersonations) {
            if (domain.contains(pattern) && !isDomainWhitelisted(domain)) {
                return@withContext PhishingUrl(
                    url = cleanUrl,
                    isMalicious = true,
                    threatType = "Brand Impersonation / Deceptive Domain",
                    detectionSource = "Heuristic Typosquatting Analyzer"
                )
            }
        }

        // 6. Heuristic: Suspicious high-abuse TLD combined with sensitive security keywords
        val tld = domain.substringAfterLast(".", "")
        if (suspiciousTlds.contains(tld)) {
            val sensitiveKeywords = listOf("login", "verify", "account", "bank", "wallet", "track", "pay", "kyc", "pan", "yono", "refund", "cashback")
            val hasSensitive = sensitiveKeywords.any { domain.contains(it) }
            if (hasSensitive) {
                return@withContext PhishingUrl(
                    url = cleanUrl,
                    isMalicious = true,
                    threatType = "Deceptive High-Risk TLD ($tld) Portal",
                    detectionSource = "Heuristic Threat Engine"
                )
            }
        }

        // 7. Credential Embedding / Obfuscation inside URL
        if (cleanUrl.contains("@") && (cleanUrl.startsWith("http://", ignoreCase = true) || cleanUrl.startsWith("https://", ignoreCase = true))) {
            return@withContext PhishingUrl(
                url = cleanUrl,
                isMalicious = true,
                threatType = "Credential Injection / URL Authority Spoofing",
                detectionSource = "Heuristic Threat Engine"
            )
        }

        PhishingUrl(
            url = cleanUrl,
            isMalicious = false,
            threatType = null,
            detectionSource = "ShieldCore Engine (No Active Threats Found)"
        )
    }

    override suspend fun markUrlSafety(url: String, status: LinkSafetyStatus) = withContext(Dispatchers.IO) {
        val domain = extractDomain(url)
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
                        val key = parts[0].lowercase(Locale.ROOT)
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

            val deceptiveKeywords = listOf("cashback", "refund", "prize", "winner", "reward", "customer support", "kyc update", "lottery", "gift", "bonus")
            val isDeceptivePayee = payeeName != null && deceptiveKeywords.any { payeeName.contains(it, ignoreCase = true) }
            val isDeceptiveNote = note != null && deceptiveKeywords.any { note.contains(it, ignoreCase = true) }

            if (isDeceptivePayee || isDeceptiveNote) {
                return@withContext UpiVerificationResult(
                    rawInput = input,
                    payeeAddress = payeeAddress,
                    payeeName = payeeName,
                    amount = amount,
                    note = note,
                    isDangerousTrap = true,
                    warningMessage = "⚠️ CRITICAL FRAUD TRAP: Payee '${payeeName ?: "Unknown"}' impersonates cashback/refund support. Clicking this and entering your UPI PIN will TRANSFER ${if (amount != null) "₹$amount" else "money"} OUT of your account!",
                    explanation = "Scammers distribute 'upi://pay' payment links falsely claiming you will receive a refund or lottery reward. In India, UPI NEVER requires entering a PIN or approving a payment request to receive funds."
                )
            }

            // Standard legitimate UPI payment intent
            val amtDisplay = if (!amount.isNullOrBlank()) " ₹$amount" else ""
            val payeeDisplay = payeeName ?: payeeAddress ?: "Recipient"
            return@withContext UpiVerificationResult(
                rawInput = input,
                payeeAddress = payeeAddress,
                payeeName = payeeName,
                amount = amount,
                note = note,
                isDangerousTrap = false,
                warningMessage = "Standard UPI Payment Request: Authorizes a payment of$amtDisplay to $payeeDisplay.",
                explanation = "This is a standard payment link. Verify that you intended to send money to $payeeDisplay before entering your PIN in your UPI app."
            )
        } else if (vpaPattern.matcher(input).find()) {
            return@withContext UpiVerificationResult(
                rawInput = input,
                payeeAddress = input,
                payeeName = null,
                amount = null,
                note = null,
                isDangerousTrap = false,
                warningMessage = null,
                explanation = "Valid UPI VPA format ($input). Always double-check the recipient identity before approving payment requests in your banking app."
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
        val lowerText = text.lowercase(Locale.ROOT)
        val highlighted = mutableListOf<String>()
        val technical = mutableListOf<String>()
        var score = 0
        var category = ScamCategory.SAFE

        // 1. Extract URLs and UPI handles
        val extractedUrls = mutableListOf<String>()
        val urlMatcher = urlPattern.matcher(text)
        while (urlMatcher.find()) {
            extractedUrls.add(urlMatcher.group())
        }

        val extractedUpi = mutableListOf<String>()
        val upiMatcher = upiUriPattern.matcher(text)
        while (upiMatcher.find()) {
            extractedUpi.add(upiMatcher.group())
        }
        val vpaMatcher = vpaPattern.matcher(text)
        while (vpaMatcher.find()) {
            extractedUpi.add(vpaMatcher.group())
        }

        // Evaluate extracted URLs
        val urlReports = mutableListOf<PhishingUrl>()
        var hasMaliciousUrl = false
        var hasWhitelistedUrl = false

        for (u in extractedUrls) {
            val rep = checkUrl(u)
            urlReports.add(rep)
            if (rep.isMalicious) {
                hasMaliciousUrl = true
                score += 35
                technical.add("Flagged Malicious Link: ${rep.url} (${rep.threatType ?: "Suspicious Domain"})")
            } else {
                val domain = extractDomain(u)
                if (isDomainWhitelisted(domain)) {
                    hasWhitelistedUrl = true
                    technical.add("Verified Official Domain: $domain")
                }
            }
        }

        // Evaluate extracted UPI
        val upiReports = mutableListOf<UpiVerificationResult>()
        var hasDangerousUpiTrap = false
        for (u in extractedUpi) {
            val rep = verifyUpiPayment(u)
            upiReports.add(rep)
            if (rep.isDangerousTrap) {
                hasDangerousUpiTrap = true
                score += 45
                technical.add("Deceptive UPI Pay Intent: ${rep.payeeName ?: rep.payeeAddress}")
            }
        }

        // 2. Sender Identity Credibility Analysis
        val isSenderOfficialDlt = sender != null && dltHeaderPattern.matcher(sender.trim()).matches()
        val isSenderPhone = sender != null && phoneSenderPattern.matcher(sender.trim().replace(" ", "").replace("-", "")).matches()

        if (isSenderPhone) {
            technical.add("Sender is a personal mobile number ($sender) rather than registered corporate DLT header")
        } else if (isSenderOfficialDlt) {
            technical.add("Registered TRAI DLT Corporate Sender Header: $sender")
        }

        // 3. Normal Transaction & Safe Message Detection
        // Normal bank debit/credit alerts with no threats should NOT trigger false alarms
        val isNormalBankTransaction = (lowerText.contains("debited") || lowerText.contains("credited") || lowerText.contains("spent on") || lowerText.contains("otp for")) &&
                !lowerText.contains("blocked") && !lowerText.contains("suspended") && !lowerText.contains("freeze") && !lowerText.contains("update kyc") && !lowerText.contains("pending pan")

        // 4. Multi-Factor Scam Signature Classifiers

        // A. Delivery Scam (Require Impersonation + Urgency/Ultimatum + Unverified/Malicious Link)
        val deliveryKeywords = listOf("indiapost", "india post", "fedex", "bluedart", "dhl", "usps", "delivery held", "package pending", "parcel blocked", "incomplete address", "update delivery address", "customs fee", "redelivery")
        val deliveryUrgency = listOf("held", "blocked", "pending", "incomplete address", "update address", "customs fee", "redelivery", "returned to sender")
        val hasDeliveryKeywords = deliveryKeywords.any { lowerText.contains(it) }
        val hasDeliveryUrgency = deliveryUrgency.any { lowerText.contains(it) }

        if (hasDeliveryKeywords && hasDeliveryUrgency && extractedUrls.isNotEmpty()) {
            if (hasMaliciousUrl || !hasWhitelistedUrl) {
                category = ScamCategory.FAKE_DELIVERY
                score += 85
                highlighted.addAll(deliveryKeywords.filter { lowerText.contains(it) })
                technical.add("Delivery Scam Signature: Urgency ultimatum combined with unofficial/suspicious update link")
            }
        }

        // B. UPI Payment / Cashback Trap
        val upiTrapKeywords = listOf("cashback received", "congratulations won", "claim reward", "enter pin to receive", "refund approved", "scratch card won", "claim cashback", "click to receive")
        val matchedUpiKeywords = upiTrapKeywords.filter { lowerText.contains(it) }
        if (hasDangerousUpiTrap || matchedUpiKeywords.isNotEmpty()) {
            category = ScamCategory.UPI_PAYMENT_TRAP
            score += 90
            highlighted.addAll(matchedUpiKeywords)
            technical.add("UPI PIN Trap: Deceptive refund/reward lure attempting to trigger account debit")
        }

        // C. Bank KYC / Account Freeze Scam
        val hasBankContext = lowerText.contains("bank") || lowerText.contains("netbanking") || lowerText.contains("sbi") ||
                lowerText.contains("hdfc") || lowerText.contains("icici") || lowerText.contains("axis") || lowerText.contains("kotak") ||
                lowerText.contains("account") || lowerText.contains("yono") || lowerText.contains("kyc") || lowerText.contains("pan")

        val hasBankThreatOrAction = lowerText.contains("blocked") || lowerText.contains("suspend") || lowerText.contains("freeze") ||
                lowerText.contains("deactivat") || lowerText.contains("pan card") || lowerText.contains("pan verification") ||
                lowerText.contains("complete your kyc") || lowerText.contains("update kyc") || lowerText.contains("kyc update") ||
                lowerText.contains("pending pan") || lowerText.contains("debit card blocked") || lowerText.contains("link pan") ||
                lowerText.contains("account freeze") || lowerText.contains("avoid account") || lowerText.contains("restricted")

        val isBankScamAttempt = hasBankContext && hasBankThreatOrAction && !isNormalBankTransaction

        if (isBankScamAttempt) {
            // Require either a non-whitelisted URL, malicious URL, or personal phone sender
            if (hasMaliciousUrl || (extractedUrls.isNotEmpty() && !hasWhitelistedUrl) || isSenderPhone) {
                category = ScamCategory.BANK_IMPERSONATION
                score += 90
                highlighted.add("Bank / KYC Freeze Threat")
                technical.add("Bank Impersonation Threat: High-pressure account freeze ultimatum with unofficial link/contact")
            }
        }

        // D. Electricity / Utility Disconnection Threat
        val hasUtilityContext = lowerText.contains("electricity") || lowerText.contains("power") || lowerText.contains("bijli") || lowerText.contains("bescom") || lowerText.contains("mseb") || lowerText.contains("tneb") || lowerText.contains("bill")
        val hasDisconnectionThreat = lowerText.contains("disconnect") || lowerText.contains("cut off") || lowerText.contains("cutoff") || lowerText.contains("power cut") || lowerText.contains("will be disconnected") || lowerText.contains("bill not updated") || lowerText.contains("unpaid electricity")
        val isUtilityScamAttempt = hasUtilityContext && hasDisconnectionThreat

        if (isUtilityScamAttempt && (isSenderPhone || lowerText.contains("call ") || (extractedUrls.isNotEmpty() && !hasWhitelistedUrl))) {
            category = ScamCategory.UTILITY_BILL_SCAM
            score += 85
            highlighted.add("Utility Disconnection Threat")
            technical.add("Utility Cutoff Scam: Extortion-style power disconnection threat directing victim to personal number")
        }

        // E. Lottery / Prize Scam
        val lotteryKeywords = listOf("won 25,00,000", "lottery winner", "kbc prize", "lucky draw winner", "car winner", "claim money immediately", "won 25 lakh")
        val matchedLottery = lotteryKeywords.filter { lowerText.contains(it) }
        if (matchedLottery.isNotEmpty()) {
            if (category == ScamCategory.SAFE) {
                category = ScamCategory.LOTTERY_PRIZE
            }
            score += 80
            highlighted.addAll(matchedLottery)
            technical.add("Advance Fee / Lottery Fraud: Fabricated reward lure")
        }

        // F. Work-From-Home / Task Scam
        val jobKeywords = listOf("earn 5000 daily", "part-time job", "like youtube videos", "telegram task", "daily payout", "hotel review job", "task earning")
        val matchedJob = jobKeywords.filter { lowerText.contains(it) }
        if (matchedJob.isNotEmpty()) {
            if (category == ScamCategory.SAFE) {
                category = ScamCategory.PART_TIME_JOB
            }
            score += 75
            highlighted.addAll(matchedJob)
            technical.add("Task Scam Signature: High-payout easy work lure leading to fraudulent cryptocurrency/deposit scheme")
        }

        // G. Malicious APK / Remote Desktop Scam
        if (lowerText.contains(".apk") || lowerText.contains("download app to fix") || lowerText.contains("quicksupport") || lowerText.contains("anydesk") || lowerText.contains("rustdesk") || lowerText.contains("teamviewer")) {
            category = ScamCategory.MALICIOUS_APK
            score += 95
            highlighted.add("Remote Access / Sideloaded APK")
            technical.add("Remote Access / Trojan APK: Direct attempt to trick user into installing screen sharing or sideloaded APK")
        }

        // H. Fallback if unverified malicious URL is present
        if (hasMaliciousUrl && category == ScamCategory.SAFE) {
            category = ScamCategory.SUSPICIOUS_LINK
            score += 60
        }

        // Whitelisted safety check: If a message is a normal transaction or genuine courier with whitelisted URL, reset score
        if (hasWhitelistedUrl && !hasDangerousUpiTrap && !hasMaliciousUrl && !isBankScamAttempt && !isUtilityScamAttempt && matchedLottery.isEmpty() && matchedJob.isEmpty()) {
            score = 0
            category = ScamCategory.SAFE
        }

        if (isNormalBankTransaction && extractedUrls.isEmpty() && !hasDangerousUpiTrap && !isBankScamAttempt) {
            score = 0
            category = ScamCategory.SAFE
        }

        val clampedScore = score.coerceIn(0, 100)
        val isScam = clampedScore >= 50

        // Senior-Friendly Plain Language Guidance
        val seniorAdvice = when (category) {
            ScamCategory.FAKE_DELIVERY ->
                "🛑 DO NOT CLICK OR PAY! Genuine courier companies (India Post, BlueDart, FedEx) NEVER ask for address updates or small ₹5-₹20 fees via SMS links. Your parcel is not in danger."
            ScamCategory.UPI_PAYMENT_TRAP ->
                "⚠️ NEVER ENTER YOUR PIN! You only enter your UPI PIN when PAYING money out. You NEVER need a PIN to receive cashback or refunds. Entering your PIN will immediately transfer money away."
            ScamCategory.BANK_IMPERSONATION ->
                "🛑 FAKE BANK ALERT: Banks NEVER threaten account suspension or ask for PAN/KYC updates via SMS links or personal mobile numbers. Do not click. If in doubt, visit your official bank branch."
            ScamCategory.UTILITY_BILL_SCAM ->
                "🛑 FAKE DISCONNECTION THREAT: Electricity boards do NOT send threats from personal 10-digit mobile numbers. Do not call the number listed in this message. Your power will NOT be cut off."
            ScamCategory.LOTTERY_PRIZE ->
                "🛑 100% FAKE LOTTERY: Legitimate organizations never award cash for competitions you did not enter. Scammers will ask for an 'advance processing fee' and steal your money."
            ScamCategory.PART_TIME_JOB ->
                "⚠️ FRAUD WARNING: No legitimate company pays thousands per day for simply liking videos. They will ask you to deposit money in Telegram groups and steal it."
            ScamCategory.MALICIOUS_APK ->
                "🛑 HIGH DANGER: NEVER install apps, .apk files, or remote screen tools (AnyDesk/QuickSupport) from message links. These apps can read your bank OTPs and steal your funds."
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
