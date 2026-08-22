package com.shieldcore.security.data.repository

import com.shieldcore.security.domain.model.ScamCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PhishingRepositoryTest {

    private lateinit var repository: PhishingRepositoryImpl

    @Before
    fun setUp() {
        repository = PhishingRepositoryImpl()
    }

    @Test
    fun testWhitelistedOfficialDomains_areSafe() = runTest {
        val sbiResult = repository.checkUrl("https://www.onlinesbi.sbi/portal")
        assertFalse(sbiResult.isMalicious)
        assertTrue(sbiResult.detectionSource?.contains("Verified Trusted Organization") == true)

        val hdfcResult = repository.checkUrl("https://netbanking.hdfcbank.com/netbanking")
        assertFalse(hdfcResult.isMalicious)

        val indiaPostResult = repository.checkUrl("https://www.indiapost.gov.in/VAS/Pages/track.aspx")
        assertFalse(indiaPostResult.isMalicious)

        val blueDartResult = repository.checkUrl("https://www.bluedart.com/tracking")
        assertFalse(blueDartResult.isMalicious)
    }

    @Test
    fun testKnownBlacklistAndSuspiciousDomains_areFlagged() = runTest {
        val scamDelivery = repository.checkUrl("https://indiapost-tracking.xyz/pay")
        assertTrue(scamDelivery.isMalicious)

        val scamKyc = repository.checkUrl("https://sbi-yono-kyc.com/update")
        assertTrue(scamKyc.isMalicious)

        val directIp = repository.checkUrl("http://185.220.101.5/login.php")
        assertTrue(directIp.isMalicious)
    }

    @Test
    fun testGenuineBankDebitSMS_isSafe() = runTest {
        val text = "HDFC Bank: Rs 2,450.00 debited from a/c **4920 on 22-AUG-26 at AMAZON RETAIL. Avail Bal: Rs 54,200.00. If not done by you, call 18002026161."
        val report = repository.analyzeMessage(text, "AD-HDFCBK")

        assertFalse(report.isScam)
        assertEquals(ScamCategory.SAFE, report.category)
        assertEquals(0, report.riskScore)
    }

    @Test
    fun testFakeBankKYCSMS_isFlaggedAsScam() = runTest {
        val text = "SBI Alert: Your NetBanking account will be blocked today due to pending PAN card verification. Please complete your KYC immediately at https://sbi-yono-kyc.com to avoid account freeze."
        val report = repository.analyzeMessage(text, "+919876543210")

        assertTrue(report.isScam)
        assertEquals(ScamCategory.BANK_IMPERSONATION, report.category)
        assertTrue(report.riskScore >= 80)
    }

    @Test
    fun testGenuineCourierSMS_withWhitelistedUrl_isSafe() = runTest {
        val text = "Your India Post SpeedPost article #EK829381928IN has been dispatched. Track your delivery at https://www.indiapost.gov.in/track"
        val report = repository.analyzeMessage(text, "VK-INDPOST")

        assertFalse(report.isScam)
        assertEquals(ScamCategory.SAFE, report.category)
    }

    @Test
    fun testFakeDeliveryScamSMS_isFlagged() = runTest {
        val text = "India Post: Your package #IN98273 is held at sorting facility due to incorrect house number. Update your delivery address within 24h at https://indiapost-tracking.xyz/pay or package will be returned to sender."
        val report = repository.analyzeMessage(text, "+919876543210")

        assertTrue(report.isScam)
        assertEquals(ScamCategory.FAKE_DELIVERY, report.category)
        assertTrue(report.riskScore >= 80)
    }

    @Test
    fun testDeceptiveUpiCashbackTrap_isFlagged() = runTest {
        val upiUri = "upi://pay?pa=refund.cashback.support@okaxis&pn=CashbackDepartment&am=2500&tn=ClaimRefund"
        val result = repository.verifyUpiPayment(upiUri)

        assertTrue(result.isDangerousTrap)
        assertTrue(result.warningMessage?.contains("CRITICAL FRAUD TRAP") == true)
        assertEquals("2500", result.amount)
    }

    @Test
    fun testStandardUpiPayment_isNotDangerousTrap() = runTest {
        val upiUri = "upi://pay?pa=merchant@okaxis&pn=GroceryStore&am=350&tn=Groceries"
        val result = repository.verifyUpiPayment(upiUri)

        assertFalse(result.isDangerousTrap)
        assertEquals("350", result.amount)
        assertEquals("GroceryStore", result.payeeName)
    }

    @Test
    fun testElectricityCutoffScam_isFlagged() = runTest {
        val text = "Dear Consumer, Your electricity connection will be disconnected tonight at 9:30 PM because your previous month bill was not updated. Please call electricity officer at 9876543210 immediately to prevent power cut."
        val report = repository.analyzeMessage(text, "+918888877777")

        assertTrue(report.isScam)
        assertEquals(ScamCategory.UTILITY_BILL_SCAM, report.category)
        assertTrue(report.riskScore >= 80)
    }
}
