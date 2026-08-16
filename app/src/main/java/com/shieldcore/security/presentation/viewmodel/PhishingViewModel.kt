package com.shieldcore.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldcore.security.domain.model.LinkSafetyStatus
import com.shieldcore.security.domain.model.PhishingUrl
import com.shieldcore.security.domain.repository.PhishingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhishingUiState(
    val testUrl: String = "",
    val isChecking: Boolean = false,
    val checkResult: PhishingUrl? = null,
    val isVpnActive: Boolean = false
)

@HiltViewModel
class PhishingViewModel @Inject constructor(
    private val repository: PhishingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhishingUiState())
    val uiState: StateFlow<PhishingUiState> = _uiState.asStateFlow()

    fun onUrlInputChanged(url: String) {
        _uiState.update { it.copy(testUrl = url) }
    }

    fun setVpnActive(active: Boolean) {
        _uiState.update { it.copy(isVpnActive = active) }
    }

    fun testUrlSafety() {
        val url = _uiState.value.testUrl.trim()
        if (url.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, checkResult = null) }
            val result = repository.checkUrl(url)
            _uiState.update { it.copy(isChecking = false, checkResult = result) }
        }
    }

    fun markCurrentUrl(status: LinkSafetyStatus) {
        val current = _uiState.value.checkResult ?: return
        viewModelScope.launch {
            repository.markUrlSafety(current.url, status)
            testUrlSafety()
        }
    }
}
