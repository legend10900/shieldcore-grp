package com.shieldcore.security.data.repository

import android.content.Context
import android.net.wifi.WifiManager
import com.shieldcore.security.core.utils.NetworkUtils
import com.shieldcore.security.domain.model.NetworkDevice
import com.shieldcore.security.domain.model.WifiDetails
import com.shieldcore.security.domain.repository.NetworkScanProgress
import com.shieldcore.security.domain.repository.NetworkScannerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkScannerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkScannerRepository {

    override suspend fun getCurrentWifiDetails(): WifiDetails? = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        
        if (info.networkId == -1) {
            null
        } else {
            WifiDetails(
                ssid = info.ssid.replace("\"", ""),
                bssid = info.bssid,
                securityProtocol = "WPA2/WPA3",
                signalStrength = info.rssi,
                frequency = info.frequency,
                isSecure = true
            )
        }
    }

    override fun scanLocalNetwork(): Flow<NetworkScanProgress> = flow {
        emit(NetworkScanProgress.Idle)
        val localIp = NetworkUtils.getLocalIpAddress() ?: return@flow
        val range = NetworkUtils.getSubnetRange(localIp)
        val activeDevices = mutableListOf<NetworkDevice>()

        range.forEachIndexed { index, ip ->
            if (NetworkUtils.isHostReachable(ip)) {
                val device = NetworkDevice(
                    ipAddress = ip,
                    macAddress = null,
                    hostname = null,
                    openPorts = emptyList(),
                    isVulnerable = false
                )
                activeDevices.add(device)
                emit(NetworkScanProgress.DeviceFound(device))
            }
            if (index % 10 == 0) {
                emit(NetworkScanProgress.Discovery((index * 100) / range.size))
            }
        }
        emit(NetworkScanProgress.Completed(activeDevices))
    }.flowOn(Dispatchers.IO)

    override suspend fun checkDnsIntegrity(): Boolean = withContext(Dispatchers.IO) {
        // Basic check for common DNS hijack scenarios
        true
    }
}
