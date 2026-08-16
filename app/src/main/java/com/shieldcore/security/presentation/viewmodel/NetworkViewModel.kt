package com.shieldcore.security.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shieldcore.security.domain.model.WifiDetails
import com.shieldcore.security.domain.repository.NetworkScanProgress
import com.shieldcore.security.domain.repository.NetworkScannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val repository: NetworkScannerRepository
) : ViewModel() {

    private val _wifiDetails = MutableStateFlow<WifiDetails?>(null)
    val wifiDetails: StateFlow<WifiDetails?> = _wifiDetails.asStateFlow()

    private val _scanProgress = MutableStateFlow<NetworkScanProgress>(NetworkScanProgress.Idle)
    val scanProgress: StateFlow<NetworkScanProgress> = _scanProgress.asStateFlow()

    private val _dnsIntact = MutableStateFlow<Boolean?>(null)
    val dnsIntact: StateFlow<Boolean?> = _dnsIntact.asStateFlow()

    init {
        loadWifiDetails()
        checkDns()
    }

    fun loadWifiDetails() {
        viewModelScope.launch {
            _wifiDetails.value = repository.getCurrentWifiDetails()
        }
    }

    fun checkDns() {
        viewModelScope.launch {
            _dnsIntact.value = repository.checkDnsIntegrity()
        }
    }

    fun startNetworkScan() {
        viewModelScope.launch {
            checkDns()
            loadWifiDetails()
            repository.scanLocalNetwork().collect {
                _scanProgress.value = it
            }
        }
    }
}
