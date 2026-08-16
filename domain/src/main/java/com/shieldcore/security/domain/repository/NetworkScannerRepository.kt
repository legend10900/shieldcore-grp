package com.shieldcore.security.domain.repository

import com.shieldcore.security.domain.model.NetworkDevice
import com.shieldcore.security.domain.model.WifiDetails
import kotlinx.coroutines.flow.Flow

interface NetworkScannerRepository {
    /**
     * Gets current Wi-Fi connection details and security status.
     */
    suspend fun getCurrentWifiDetails(): WifiDetails?

    /**
     * Scans the local subnet for active devices and open ports.
     */
    fun scanLocalNetwork(): Flow<NetworkScanProgress>

    /**
     * Checks if DNS is hijacked.
     */
    suspend fun checkDnsIntegrity(): Boolean
}

sealed class NetworkScanProgress {
    object Idle : NetworkScanProgress()
    data class DeviceFound(val device: NetworkDevice) : NetworkScanProgress()
    data class Discovery(val percentage: Int) : NetworkScanProgress()
    data class Completed(val devices: List<NetworkDevice>) : NetworkScanProgress()
}
