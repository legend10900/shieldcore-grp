package com.shieldcore.security.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.shieldcore.security.core.BaseViewModel
import com.shieldcore.security.core.UiEffect
import com.shieldcore.security.core.UiEvent
import com.shieldcore.security.core.UiState
import com.shieldcore.security.domain.model.ScanResult
import com.shieldcore.security.domain.model.ScanSummary
import com.shieldcore.security.domain.repository.ScanProgress
import com.shieldcore.security.domain.usecase.ScanDeviceUseCase
import com.shieldcore.security.domain.repository.ScannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AntivirusUiState(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val totalCount: Int = 0,
    val currentAppName: String = "",
    val detectedThreats: List<ScanResult> = emptyList(),
    val lastScanSummary: ScanSummary? = null,
    val scanHistory: List<ScanSummary> = emptyList(),
    val errorMessage: String? = null
) : UiState

sealed class AntivirusUiEvent : UiEvent {
    object StartScan : AntivirusUiEvent()
    object LoadHistory : AntivirusUiEvent()
}

sealed class AntivirusUiEffect : UiEffect {
    data class ShowToast(val message: String) : AntivirusUiEffect()
}

@HiltViewModel
class AntivirusViewModel @Inject constructor(
    private val scanDeviceUseCase: ScanDeviceUseCase,
    private val scannerRepository: ScannerRepository
) : BaseViewModel<AntivirusUiState, AntivirusUiEvent, AntivirusUiEffect>(AntivirusUiState()) {

    init {
        onEvent(AntivirusUiEvent.LoadHistory)
    }

    override fun onEvent(event: AntivirusUiEvent) {
        when (event) {
            is AntivirusUiEvent.StartScan -> startFullDeviceScan()
            is AntivirusUiEvent.LoadHistory -> loadScanHistory()
        }
    }

    private fun startFullDeviceScan() {
        viewModelScope.launch {
            scanDeviceUseCase().collect { progress ->
                when (progress) {
                    is ScanProgress.Idle -> {
                        updateState { copy(isScanning = false, detectedThreats = emptyList()) }
                    }
                    is ScanProgress.Running -> {
                        updateState {
                            copy(
                                isScanning = true,
                                currentAppName = progress.currentApp,
                                scannedCount = progress.scannedCount,
                                totalCount = progress.totalCount,
                                detectedThreats = progress.threatsFound
                            )
                        }
                    }
                    is ScanProgress.Completed -> {
                        updateState {
                            copy(
                                isScanning = false,
                                lastScanSummary = progress.summary,
                                detectedThreats = progress.threats
                            )
                        }
                        sendEffect(AntivirusUiEffect.ShowToast("Scan Completed: ${progress.summary.threatsFound} threats found"))
                        onEvent(AntivirusUiEvent.LoadHistory)
                    }
                    is ScanProgress.Error -> {
                        updateState {
                            copy(
                                isScanning = false,
                                errorMessage = progress.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun loadScanHistory() {
        viewModelScope.launch {
            scannerRepository.getScanHistory().collectLatest { history ->
                updateState { copy(scanHistory = history) }
            }
        }
    }

    fun removeThreat(packageName: String) {
        viewModelScope.launch {
            val success = scannerRepository.removeThreat(packageName)
            if (success) {
                updateState {
                    copy(detectedThreats = detectedThreats.filter { it.packageName != packageName && it.filePath != packageName })
                }
                sendEffect(AntivirusUiEffect.ShowToast("Uninstall prompted for: $packageName"))
            }
        }
    }
}
