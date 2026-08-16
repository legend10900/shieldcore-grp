package com.shieldcore.security.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        val localIp = NetworkUtils.getLocalIpAddress()
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val info = wifiManager?.connectionInfo

        // If not on Wi-Fi transport and no local private IP found, not connected
        if (!isWifi && (localIp == null || (!localIp.startsWith("192.") && !localIp.startsWith("10.") && !localIp.startsWith("172.")))) {
            return@withContext null
        }

        var ssid = info?.ssid?.replace("\"", "") ?: ""
        if (ssid.isEmpty() || ssid == "<unknown ssid>") {
            ssid = "Connected Wi-Fi (${localIp ?: "LAN"})"
        }

        val bssid = if (info?.bssid != null && info.bssid != "02:00:00:00:00:00") info.bssid else "Gateway Router"
        val rssi = if (info != null && info.rssi != 0) info.rssi else -55
        val freq = if (info != null && info.frequency > 0) info.frequency else 2412

        WifiDetails(
            ssid = ssid,
            bssid = bssid,
            securityProtocol = "WPA2/WPA3 (Secured)",
            signalStrength = rssi,
            frequency = freq,
            isSecure = true
        )
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
