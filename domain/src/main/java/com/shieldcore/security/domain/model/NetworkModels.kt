package com.shieldcore.security.domain.model

data class WifiDetails(
    val ssid: String,
    val bssid: String,
    val securityProtocol: String,
    val signalStrength: Int,
    val frequency: Int,
    val isSecure: Boolean
)

data class NetworkDevice(
    val ipAddress: String,
    val macAddress: String?,
    val hostname: String?,
    val openPorts: List<Int>,
    val isVulnerable: Boolean
)
