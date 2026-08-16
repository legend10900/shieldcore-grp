package com.shieldcore.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldcore.security.domain.model.WifiDetails
import com.shieldcore.security.domain.repository.NetworkScanProgress
import com.shieldcore.security.domain.repository.NetworkScannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val repository: NetworkScannerRepository
) : ViewModel() {

    private val _wifiDetails = MutableStateFlow<WifiDetails?>(null)
    val wifiDetails: StateFlow<WifiDetails?> = _wifiDetails

    private val _scanProgress = MutableStateFlow<NetworkScanProgress>(NetworkScanProgress.Idle)
    val scanProgress: StateFlow<NetworkScanProgress> = _scanProgress

    init {
        loadWifiDetails()
    }

    private fun loadWifiDetails() {
        viewModelScope.launch {
            _wifiDetails.value = repository.getCurrentWifiDetails()
        }
    }

    fun startNetworkScan() {
        viewModelScope.launch {
            repository.scanLocalNetwork().collect {
                _scanProgress.value = it
            }
        }
    }
}
