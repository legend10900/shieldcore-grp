package com.shieldcore.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldcore.security.domain.repository.ScanProgress
import com.shieldcore.security.domain.usecase.PerformFullScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val performFullScanUseCase: PerformFullScanUseCase
) : ViewModel() {

    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)
    val scanProgress: StateFlow<ScanProgress> = _scanProgress

    fun startFullScan() {
        viewModelScope.launch {
            performFullScanUseCase().collect {
                _scanProgress.value = it
            }
        }
    }
}
