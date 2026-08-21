package com.shieldcore.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldcore.security.domain.model.*
import com.shieldcore.security.domain.repository.PhishingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PhishingInspectorTab {
    SMS_MESSAGE_ANALYZER,
    URL_DEEP_INSPECTOR,
    UPI_PAYMENT_VERIFIER
}

data class PhishingUiState(
    val selectedTab: PhishingInspectorTab = PhishingInspectorTab.SMS_MESSAGE_ANALYZER,
    val isSeniorMode: Boolean = true, // Default to senior-friendly high-contrast clear view
    val isVpnActive: Boolean = false,
    
    // SMS / Message Analyzer State
    val messageText: String = "",
    val senderInput: String = "",
    val isAnalyzingMessage: Boolean = false,
    val messageReport: FraudAnalysisReport? = null,

    // URL Inspector State
    val testUrl: String = "",
    val isCheckingUrl: Boolean = false,
    val checkResult: PhishingUrl? = null,

    // UPI Payment Verifier State
    val upiInput: String = "",
    val isCheckingUpi: Boolean = false,
    val upiResult: UpiVerificationResult? = null
)

@HiltViewModel
class PhishingViewModel @Inject constructor(
    private val repository: PhishingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhishingUiState())
    val uiState: StateFlow<PhishingUiState> = _uiState.asStateFlow()

    fun selectTab(tab: PhishingInspectorTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleSeniorMode(enabled: Boolean) {
        _uiState.update { it.copy(isSeniorMode = enabled) }
    }

    fun setVpnActive(active: Boolean) {
        _uiState.update { it.copy(isVpnActive = active) }
    }

    // 1. Message / SMS Analyzer Actions
    fun onMessageTextChanged(text: String) {
        _uiState.update { it.copy(messageText = text) }
    }

    fun onSenderInputChanged(sender: String) {
        _uiState.update { it.copy(senderInput = sender) }
    }

    fun analyzeMessage() {
        val text = _uiState.value.messageText.trim()
        if (text.isEmpty()) return

        val sender = _uiState.value.senderInput.trim().ifEmpty { null }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingMessage = true) }
            val report = repository.analyzeMessage(text, sender)
            _uiState.update { it.copy(isAnalyzingMessage = false, messageReport = report) }
        }
    }

    fun loadScamTemplate(templateType: String) {
        val (text, sender) = when (templateType) {
            "FAKE_DELIVERY" -> Pair(
                "India Post: Your package #IN98273 is held at sorting facility due to incorrect house number. Update your delivery address within 24h at https://indiapost-tracking.xyz/pay or package will be returned to sender.",
                "VM-INDPOST"
            )
            "UPI_CASHBACK" -> Pair(
                "Congratulations! You have received Rs 2,500 cashback on your last transaction. Click here to receive money in your account: upi://pay?pa=refund.cashback.support@okaxis&pn=CashbackDepartment&am=2500&tn=ClaimRefund",
                "+919876543210"
            )
            "ELECTRICITY_CUTOFF" -> Pair(
                "Dear Consumer, Your electricity connection will be disconnected tonight at 9:30 PM because your previous month bill was not updated. Please call electricity officer at 9876543210 immediately to prevent power cut.",
                "+918888877777"
            )
            "BANK_KYC" -> Pair(
                "SBI Alert: Your NetBanking account will be blocked today due to pending PAN card verification. Please complete your KYC immediately at https://sbi-yono-kyc.com to avoid account freeze.",
                "BZ-SBIBNK"
            )
            "LEGITIMATE" -> Pair(
                "Your Amazon order #402-8829102-19283 for 'Noise Pulse Smartwatch' has been dispatched and will be delivered tomorrow by 8 PM.",
                "AX-AMAZON"
            )
            else -> Pair("", "")
        }

        _uiState.update {
            it.copy(
                selectedTab = PhishingInspectorTab.SMS_MESSAGE_ANALYZER,
                messageText = text,
                senderInput = sender,
                messageReport = null
            )
        }
        analyzeMessage()
    }

    // 2. URL Inspector Actions
    fun onUrlInputChanged(url: String) {
        _uiState.update { it.copy(testUrl = url) }
    }

    fun testUrlSafety() {
        val url = _uiState.value.testUrl.trim()
        if (url.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUrl = true, checkResult = null) }
            val result = repository.checkUrl(url)
            _uiState.update { it.copy(isCheckingUrl = false, checkResult = result) }
        }
    }

    fun markCurrentUrl(status: LinkSafetyStatus) {
        val current = _uiState.value.checkResult ?: return
        viewModelScope.launch {
            repository.markUrlSafety(current.url, status)
            testUrlSafety()
        }
    }

    // 3. UPI Verifier Actions
    fun onUpiInputChanged(input: String) {
        _uiState.update { it.copy(upiInput = input) }
    }

    fun verifyUpi() {
        val input = _uiState.value.upiInput.trim()
        if (input.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUpi = true, upiResult = null) }
            val result = repository.verifyUpiPayment(input)
            _uiState.update { it.copy(isCheckingUpi = false, upiResult = result) }
        }
    }
}
