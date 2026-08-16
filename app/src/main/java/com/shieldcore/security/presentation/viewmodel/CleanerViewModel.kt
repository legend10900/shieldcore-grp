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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CleanerViewModel @Inject constructor(
    private val repository: JunkCleanerRepository
) : ViewModel() {

    private val _scanProgress = MutableStateFlow<JunkScanProgress>(JunkScanProgress.Idle)
    val scanProgress: StateFlow<JunkScanProgress> = _scanProgress.asStateFlow()

    private val _cleanSummary = MutableStateFlow<CleanSummary?>(null)
    val cleanSummary: StateFlow<CleanSummary?> = _cleanSummary.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    fun startScan() {
        viewModelScope.launch {
            _selectedIds.value = emptySet()
            _cleanSummary.value = null
            repository.scanForJunk().collect { progress ->
                _scanProgress.value = progress
                if (progress is JunkScanProgress.Completed) {
                    _selectedIds.value = progress.items.map { it.id }.toSet()
                }
            }
        }
    }

    fun toggleItemSelection(id: String) {
        _selectedIds.update { current ->
            if (current.contains(id)) current - id else current + id
        }
    }

    fun selectAll(items: List<JunkItem>) {
        _selectedIds.value = items.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedIds.value = emptySet()
    }

    fun cleanSelected(items: List<JunkItem>) {
        val toClean = items.filter { _selectedIds.value.contains(it.id) }
        if (toClean.isEmpty()) return

        viewModelScope.launch {
            val summary = repository.cleanJunk(toClean)
            _cleanSummary.value = summary
        }
    }

    fun dismissSummary() {
        _cleanSummary.value = null
        startScan()
    }
}
