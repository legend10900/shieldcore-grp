package com.shieldcore.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldcore.security.domain.repository.BatteryInfo
import com.shieldcore.security.domain.repository.BatteryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val repository: BatteryRepository
) : ViewModel() {

    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getBatteryInfo().collect {
                _batteryInfo.value = it
            }
        }
    }

    fun optimize() {
        viewModelScope.launch {
            repository.optimizePower()
        }
    }
}
