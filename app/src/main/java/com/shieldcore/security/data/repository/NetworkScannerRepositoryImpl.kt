package com.shieldcore.security.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.shieldcore.security.domain.repository.NetworkDeviceInfo
import com.shieldcore.security.domain.repository.NetworkScannerRepository
import com.shieldcore.security.domain.repository.WifiDiagnostics
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkScannerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkScannerRepository {

    override suspend fun getWifiDiagnostics(): WifiDiagnostics = withContext(Dispatchers.IO) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo

        val ssid = wifiInfo?.ssid?.replace("\"", "") ?: "Unknown SSID"
        val bssid = wifiInfo?.bssid ?: "00:00:00:00:00:00"
        val rssi = wifiInfo?.rssi ?: -100
        val frequency = wifiInfo?.frequency ?: 2412

        val dhcpInfo = wifiManager.dhcpInfo
        val gatewayIp = formatIp(dhcpInfo?.gateway ?: 0)

        val knownSecureDns = setOf("1.1.1.1", "1.0.0.1", "8.8.8.8", "8.8.4.4", "9.9.9.9")
        val isSecure = knownSecureDns.contains(gatewayIp) || gatewayIp.startsWith("192.168.") || gatewayIp.startsWith("10.")

        WifiDiagnostics(
            ssid = ssid,
            bssid = bssid,
            signalStrengthRssi = rssi,
            frequencyMhz = frequency,
            securityProtocol = "WPA2/WPA3 Personal",
            dnsGatewayIp = gatewayIp,
            isDnsSecure = isSecure
        )
    }

    override suspend fun scanLocalSubnetDevices(): List<NetworkDeviceInfo> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<NetworkDeviceInfo>()
        val baseSubnet = "192.168.1."
        val commonPorts = listOf(80, 443, 22, 53, 8080)

        // Parallel scan across subnet range (limited range for responsive performance)
        coroutineScope {
            val tasks = (1..30).map { host ->
                async(Dispatchers.IO) {
                    val ip = "$baseSubnet$host"
                    try {
                        val address = InetAddress.getByName(ip)
                        if (address.isReachable(300)) {
                            val openPorts = mutableListOf<Int>()
                            for (port in commonPorts) {
                                try {
                                    Socket().use { socket ->
                                        socket.connect(java.net.InetSocketAddress(ip, port), 150)
                                        openPorts.add(port)
                                    }
                                } catch (e: Exception) {
                                    // Port closed or filtered
                                }
                            }
                            NetworkDeviceInfo(
                                ipAddress = ip,
                                macAddress = "02:00:00:00:00:00",
                                hostname = address.canonicalHostName ?: ip,
                                openPorts = openPorts,
                                isGateway = host == 1
                            )
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val results = tasks.awaitAll()
            devices.addAll(results.filterNotNull())
        }

        devices
    }

    private fun formatIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}
