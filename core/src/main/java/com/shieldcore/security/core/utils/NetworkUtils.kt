package com.shieldcore.security.core.utils

import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface

object NetworkUtils {

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is InetAddress && address.hostAddress.indexOf(':') < 0) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getSubnetRange(localIp: String): List<String> {
        val prefix = localIp.substringBeforeLast(".")
        return (1..254).map { "$prefix.$it" }
    }

    fun isHostReachable(ip: String, timeout: Int = 500): Boolean {
        return try {
            InetAddress.getByName(ip).isReachable(timeout)
        } catch (e: Exception) {
            false
        }
    }
}
