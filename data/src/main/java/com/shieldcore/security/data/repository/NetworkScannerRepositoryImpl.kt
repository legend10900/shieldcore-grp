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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkScannerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkScannerRepository {

    override suspend fun getCurrentWifiDetails(): WifiDetails? = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return@withContext null
        val info = wifiManager.connectionInfo ?: return@withContext null

        if (info.networkId == -1 || info.ssid == "<unknown ssid>") {
            null
        } else {
            val rawSsid = info.ssid.replace("\"", "")
            val isSecure = !rawSsid.isEmpty() && info.networkId != -1
            WifiDetails(
                ssid = if (rawSsid == "<unknown ssid>") "Connected Wi-Fi" else rawSsid,
                bssid = info.bssid ?: "00:00:00:00:00:00",
                securityProtocol = if (isSecure) "WPA2/WPA3 (Secured)" else "Open / Unsecured",
                signalStrength = info.rssi,
                frequency = info.frequency,
                isSecure = isSecure
            )
        }
    }

    override fun scanLocalNetwork(): Flow<NetworkScanProgress> = flow {
        emit(NetworkScanProgress.Idle)
        val localIp = NetworkUtils.getLocalIpAddress() ?: run {
            emit(NetworkScanProgress.Completed(emptyList()))
            return@flow
        }

        val range = NetworkUtils.getSubnetRange(localIp)
        val activeDevices = mutableListOf<NetworkDevice>()
        val totalIps = range.size
        val scannedCounter = AtomicInteger(0)
        val concurrencyLimit = Semaphore(32) // 32 concurrent probes for fast LAN sweep

        coroutineScope {
            val deferredList = range.map { ip ->
                async(Dispatchers.IO) {
                    concurrencyLimit.withPermit {
                        val isReachable = if (ip == localIp) true else NetworkUtils.isHostReachableFast(ip, 250)
                        val count = scannedCounter.incrementAndGet()
                        
                        var device: NetworkDevice? = null
                        if (isReachable) {
                            val mac = if (ip == localIp) "This Device" else (NetworkUtils.getMacFromArp(ip) ?: "Unknown")
                            val hostname = try {
                                val resolved = InetAddress.getByName(ip).canonicalHostName
                                if (resolved != ip) resolved else (if (ip == localIp) "This Device" else null)
                            } catch (_: Exception) {
                                null
                            }
                            val openPorts = NetworkUtils.scanPorts(ip, listOf(80, 443, 22, 53, 8080))
                            val isVulnerable = openPorts.contains(23) || openPorts.contains(21) // Insecure protocols

                            device = NetworkDevice(
                                ipAddress = ip,
                                macAddress = mac,
                                hostname = hostname,
                                openPorts = openPorts,
                                isVulnerable = isVulnerable
                            )
                        }

                        Pair(count, device)
                    }
                }
            }

            for (deferred in deferredList) {
                val (count, device) = deferred.await()
                if (device != null) {
                    activeDevices.add(device)
                    emit(NetworkScanProgress.DeviceFound(device))
                }
                if (count % 15 == 0 || count == totalIps) {
                    emit(NetworkScanProgress.Discovery((count * 100) / totalIps))
                }
            }
        }

        emit(NetworkScanProgress.Completed(activeDevices))
    }.flowOn(Dispatchers.IO)

    override suspend fun checkDnsIntegrity(): Boolean = withContext(Dispatchers.IO) {
        NetworkUtils.checkDnsIntegrity()
    }
}
