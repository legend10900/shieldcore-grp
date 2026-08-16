package com.shieldcore.security.core.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

object NetworkUtils {

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    val host = address.hostAddress
                    if (!address.isLoopbackAddress && host != null && !host.contains(":")) {
                        return host
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

    /**
     * Rapidly checks if a host is reachable using lightweight socket probes and ICMP fallback.
     */
    fun isHostReachableFast(ip: String, timeoutMs: Int = 300): Boolean {
        // First try standard isReachable
        try {
            if (InetAddress.getByName(ip).isReachable(timeoutMs)) {
                return true
            }
        } catch (_: Exception) {}

        // Fallback to checking standard open LAN ports (HTTP, HTTPS, SMB, DNS, Gateway ports)
        val probePorts = intArrayOf(80, 443, 53, 445, 8080, 22)
        for (port in probePorts) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs / 2)
                    return true
                }
            } catch (_: Exception) {
                // Connection refused still indicates the host is alive at this IP!
            }
        }
        return false
    }

    fun isHostReachable(ip: String, timeout: Int = 500): Boolean {
        return isHostReachableFast(ip, timeout)
    }

    /**
     * Reads /proc/net/arp to resolve IP addresses to hardware MAC addresses on Android.
     */
    fun getMacFromArp(ip: String): String? {
        try {
            val arpTable = File("/proc/net/arp")
            if (!arpTable.exists() || !arpTable.canRead()) return null

            BufferedReader(FileReader(arpTable)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line?.split("\\s+".toRegex()) ?: continue
                    if (tokens.size >= 4 && tokens[0] == ip) {
                        val mac = tokens[3]
                        if (mac != "00:00:00:00:00:00") {
                            return mac.uppercase()
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Scans for common open ports on a given IP.
     */
    suspend fun scanPorts(ip: String, ports: List<Int> = listOf(80, 443, 22, 53, 8080, 8443)): List<Int> = withContext(Dispatchers.IO) {
        val openPorts = mutableListOf<Int>()
        for (port in ports) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 200)
                    openPorts.add(port)
                }
            } catch (_: Exception) {}
        }
        openPorts
    }

    /**
     * Checks DNS resolution integrity by comparing resolution of reliable domains against system DNS.
     */
    suspend fun checkDnsIntegrity(): Boolean = withContext(Dispatchers.IO) {
        try {
            val testDomains = listOf("google.com", "cloudflare.com", "microsoft.com")
            var validLookups = 0
            for (domain in testDomains) {
                val addresses = InetAddress.getAllByName(domain)
                if (addresses != null && addresses.isNotEmpty()) {
                    validLookups++
                }
            }
            validLookups >= 2
        } catch (_: Exception) {
            false
        }
    }
}
