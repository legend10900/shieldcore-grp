package com.shieldcore.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldcore.security.domain.model.CleanSummary
import com.shieldcore.security.domain.model.JunkItem
import com.shieldcore.security.domain.repository.JunkCleanerRepository
import com.shieldcore.security.domain.repository.JunkScanProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CleanerViewModel @Inject constructor(
    private val repository: JunkCleanerRepository
) : ViewModel() {

    private val _scanProgress = MutableStateFlow<JunkScanProgress>(JunkScanProgress.Idle)
    val scanProgress: StateFlow<JunkScanProgress> = _scanProgress

    private val _cleanSummary = MutableStateFlow<CleanSummary?>(null)
    val cleanSummary: StateFlow<CleanSummary?> = _cleanSummary

    fun startScan() {
        viewModelScope.launch {
            repository.scanForJunk().collect {
                _scanProgress.value = it
            }
        }
    }

    fun cleanSelected(items: List<JunkItem>) {
        viewModelScope.launch {
            val summary = repository.cleanJunk(items)
            _cleanSummary.value = summary
            // Refresh scan after cleaning
            startScan()
        }
    }
}
