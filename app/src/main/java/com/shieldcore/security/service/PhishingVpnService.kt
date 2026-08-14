package com.shieldcore.security.service

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class PhishingVpnService : VpnService(), Runnable {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile
    private var isRunning = false

    // Local cached bloom filter / blocklist of verified phishing domains
    private val phishingBlocklist = setOf(
        "phishing-example.com",
        "fake-bank-login.secure-update.xyz",
        "credential-harvest.net"
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            startVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        try {
            val builder = Builder()
                .addAddress("10.1.10.1", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .setSession("ShieldCorePhishingShield")

            vpnInterface = builder.establish()
            isRunning = true
            vpnThread = Thread(this, "PhishingVpnThread").apply { start() }
        } catch (e: Exception) {
            Log.e("PhishingVpnService", "Failed to start VPN interface", e)
        }
    }

    override fun run() {
        try {
            val inputStream = FileInputStream(vpnInterface?.fileDescriptor)
            val outputStream = FileOutputStream(vpnInterface?.fileDescriptor)
            val buffer = ByteBuffer.allocate(32768)

            while (isRunning) {
                val readBytes = inputStream.read(buffer.array())
                if (readBytes > 0) {
                    // Non-intrusive DNS packet inspection logic
                    buffer.limit(readBytes)
                    val packetDomain = parseHostFromDnsPacket(buffer.array(), readBytes)

                    if (packetDomain != null && isDomainBlocked(packetDomain)) {
                        Log.w("PhishingVpnService", "BLOCKED PHISHING CONNECTION: $packetDomain")
                        // Drop packet or redirect to local warning page
                        buffer.clear()
                        continue
                    }

                    // Forward legitimate packet
                    outputStream.write(buffer.array(), 0, readBytes)
                    buffer.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("PhishingVpnService", "VPN processing loop ended", e)
        }
    }

    private fun parseHostFromDnsPacket(data: ByteArray, length: Int): String? {
        // Lightweight DNS QNAME parser
        return try {
            if (length < 28) return null
            // Check UDP DNS port (53)
            val domainBuilder = StringBuilder()
            var idx = 28 // Start of QNAME in standard IPv4+UDP DNS packet
            while (idx < length) {
                val labelLen = data[idx].toInt() and 0xFF
                if (labelLen == 0) break
                if (domainBuilder.isNotEmpty()) domainBuilder.append(".")
                idx++
                if (idx + labelLen > length) return null
                domainBuilder.append(String(data, idx, labelLen))
                idx += labelLen
            }
            if (domainBuilder.isEmpty()) null else domainBuilder.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun isDomainBlocked(domain: String): Boolean {
        return phishingBlocklist.any { domain.contains(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        vpnInterface?.close()
        vpnInterface = null
    }
}
