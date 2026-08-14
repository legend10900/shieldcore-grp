package com.shieldcore.security.domain.repository

data class NetworkDeviceInfo(
    val ipAddress: String,
    val macAddress: String,
    val hostname: String,
    val openPorts: List<Int>,
    val isGateway: Boolean
)

data class WifiDiagnostics(
    val ssid: String,
    val bssid: String,
    val signalStrengthRssi: Int,
    val frequencyMhz: Int,
    val securityProtocol: String,
    val dnsGatewayIp: String,
    val isDnsSecure: Boolean
)

interface NetworkScannerRepository {
    suspend fun getWifiDiagnostics(): WifiDiagnostics
    suspend fun scanLocalSubnetDevices(): List<NetworkDeviceInfo>
}
