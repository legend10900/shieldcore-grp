package com.shieldcore.security.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shieldcore.security.domain.repository.PhishingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

/**
 * Local VPN service to monitor outbound DNS requests for phishing protection.
 */
@AndroidEntryPoint
class PhishingVpnService : VpnService() {

    @Inject
    lateinit var phishingRepository: PhishingRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false

    companion object {
        const val ACTION_START = "com.shieldcore.security.action.START_VPN"
        const val ACTION_STOP = "com.shieldcore.security.action.STOP_VPN"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "shieldcore_vpn_channel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopVpn()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (!isRunning) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    setupVpn()
                }
                return START_STICKY
            }
        }
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Web & Phishing Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active protection against malicious and phishing websites"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ShieldCore Web Shield Active")
            .setContentText("Protecting outbound network traffic and DNS lookups")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }

    private fun setupVpn() {
        try {
            val builder = Builder()
                .setSession("ShieldCore Web Shield")
                .addAddress("10.0.0.2", 32)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()
            isRunning = true

            serviceScope.launch {
                monitorTraffic()
            }
        } catch (e: Exception) {
            Log.e("PhishingVpnService", "Failed to establish VPN interface", e)
        }
    }

    private suspend fun monitorTraffic() {
        vpnInterface?.let { pfd ->
            val input = FileInputStream(pfd.fileDescriptor)
            val output = FileOutputStream(pfd.fileDescriptor)
            val buffer = ByteBuffer.allocate(32768)

            while (currentCoroutineContext().isActive && isRunning) {
                try {
                    val length = input.read(buffer.array())
                    if (length > 0) {
                        // Extract DNS query domain if packet is IPv4 UDP to port 53
                        val bytes = buffer.array()
                        if (length > 28 && bytes[9].toInt() == 17) { // UDP
                            val dstPort = ((bytes[22].toInt() and 0xFF) shl 8) or (bytes[23].toInt() and 0xFF)
                            if (dstPort == 53 && length > 40) {
                                // DNS query parsing
                                parseDnsQueryDomain(bytes, length)?.let { domain ->
                                    val result = phishingRepository.checkUrl("https://$domain")
                                    if (result.isMalicious) {
                                        Log.w("PhishingVpnService", "Blocking malicious DNS request: $domain")
                                        buffer.clear()
                                        yield()
                                        return@let
                                    }
                                }
                            }
                        }

                        output.write(bytes, 0, length)
                    }
                } catch (e: Exception) {
                    if (!isRunning) break
                }
                buffer.clear()
                yield()
            }
        }
    }

    private fun parseDnsQueryDomain(packet: ByteArray, length: Int): String? {
        try {
            // DNS header starts after IP header (20 bytes) and UDP header (8 bytes) = offset 28
            var offset = 28 + 12 // Skip DNS header (12 bytes)
            val domainBuilder = StringBuilder()

            while (offset < length) {
                val labelLength = packet[offset].toInt() and 0xFF
                if (labelLength == 0) break
                offset++
                if (offset + labelLength > length) break

                if (domainBuilder.isNotEmpty()) {
                    domainBuilder.append('.')
                }
                domainBuilder.append(String(packet, offset, labelLength, Charsets.US_ASCII))
                offset += labelLength
            }

            return if (domainBuilder.isNotEmpty()) domainBuilder.toString() else null
        } catch (_: Exception) {
            return null
        }
    }

    private fun stopVpn() {
        isRunning = false
        serviceScope.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
