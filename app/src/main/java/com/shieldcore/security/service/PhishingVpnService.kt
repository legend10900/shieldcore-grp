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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.Arrays
import javax.inject.Inject

/**
 * Local DNS-filtering VPN service to protect against phishing and malicious domains
 * without interrupting regular TCP/UDP internet connectivity.
 */
@AndroidEntryPoint
class PhishingVpnService : VpnService() {

    @Inject
    lateinit var phishingRepository: PhishingRepository

    private var vpnInterface: ParcelFileDescriptor? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isRunning = false
    private val domainVerdictCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    companion object {
        const val ACTION_START = "com.shieldcore.security.action.START_VPN"
        const val ACTION_STOP = "com.shieldcore.security.action.STOP_VPN"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "shieldcore_vpn_channel"
        private const val VIRTUAL_DNS = "10.0.0.1"
        private const val VIRTUAL_IP = "10.0.0.2"
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
                description = "Active real-time protection against phishing and malicious websites"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ShieldCore Web Shield Active")
            .setContentText("DNS Filtering & Phishing Protection Enabled")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .build()
    }

    private fun setupVpn() {
        try {
            val builder = Builder()
                .setSession("ShieldCore Web Shield")
                .addAddress(VIRTUAL_IP, 32)
                .addDnsServer(VIRTUAL_DNS)
                .addRoute(VIRTUAL_DNS, 32) // Only route DNS packets to TUN

            vpnInterface = builder.establish()
            isRunning = true

            serviceScope.launch {
                processDnsPackets()
            }
        } catch (e: Exception) {
            Log.e("PhishingVpnService", "Failed to establish VPN interface", e)
        }
    }

    private suspend fun processDnsPackets() = withContext(Dispatchers.IO) {
        vpnInterface?.let { pfd ->
            val input = FileInputStream(pfd.fileDescriptor)
            val output = FileOutputStream(pfd.fileDescriptor)
            val packetBuffer = ByteArray(4096)

            // Create protected upstream socket to query public DNS without looping into VPN
            val upstreamSocket = DatagramSocket()
            protect(upstreamSocket)
            upstreamSocket.soTimeout = 2500
            val upstreamDns = InetAddress.getByName("1.1.1.1") // Cloudflare DNS

            while (isActive && isRunning) {
                try {
                    val length = input.read(packetBuffer)
                    if (length > 28) {
                        // Check if IPv4 (version 4) and UDP (protocol 17)
                        val version = (packetBuffer[0].toInt() and 0xF0) shr 4
                        val protocol = packetBuffer[9].toInt() and 0xFF
                        if (version == 4 && protocol == 17) {
                            val ipHeaderLen = (packetBuffer[0].toInt() and 0x0F) * 4
                            val srcPort = ((packetBuffer[ipHeaderLen].toInt() and 0xFF) shl 8) or (packetBuffer[ipHeaderLen + 1].toInt() and 0xFF)
                            val dstPort = ((packetBuffer[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (packetBuffer[ipHeaderLen + 3].toInt() and 0xFF)

                            if (dstPort == 53) {
                                val dnsPayloadOffset = ipHeaderLen + 8
                                val dnsPayloadLen = length - dnsPayloadOffset

                                if (dnsPayloadLen > 12) {
                                    val dnsPayload = Arrays.copyOfRange(packetBuffer, dnsPayloadOffset, length)
                                    val domain = parseDnsDomain(dnsPayload)

                                    var isBlocked = false
                                    if (domain != null) {
                                        val cached = domainVerdictCache[domain]
                                        if (cached != null) {
                                            isBlocked = cached
                                        } else {
                                            val checkResult = phishingRepository.checkUrl("https://$domain")
                                            isBlocked = checkResult.isMalicious
                                            domainVerdictCache[domain] = isBlocked
                                        }
                                        if (isBlocked) {
                                            Log.w("PhishingVpnService", "Sinkholing malicious domain: $domain")
                                        }
                                    }

                                    val responseDnsPayload = if (isBlocked) {
                                        buildNxDomainResponse(dnsPayload)
                                    } else {
                                        // Forward to real upstream DNS
                                        try {
                                            val queryPacket = DatagramPacket(dnsPayload, dnsPayload.size, upstreamDns, 53)
                                            upstreamSocket.send(queryPacket)

                                            val respBuffer = ByteArray(4096)
                                            val respPacket = DatagramPacket(respBuffer, respBuffer.size)
                                            upstreamSocket.receive(respPacket)
                                            Arrays.copyOf(respPacket.data, respPacket.length)
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }

                                    if (responseDnsPayload != null) {
                                        // Construct return IPv4 + UDP packet
                                        val responsePacket = buildUdpIpPacket(
                                            srcIp = VIRTUAL_DNS,
                                            dstIp = VIRTUAL_IP,
                                            srcPort = 53,
                                            dstPort = srcPort,
                                            payload = responseDnsPayload
                                        )
                                        output.write(responsePacket)
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                    if (!isRunning) break
                }
                yield()
            }

            try {
                upstreamSocket.close()
            } catch (_: Exception) {}
        }
    }

    private fun parseDnsDomain(dnsPayload: ByteArray): String? {
        try {
            var offset = 12 // Skip DNS header
            val domainBuilder = StringBuilder()
            while (offset < dnsPayload.size) {
                val len = dnsPayload[offset].toInt() and 0xFF
                if (len == 0) break
                offset++
                if (offset + len > dnsPayload.size) break
                if (domainBuilder.isNotEmpty()) domainBuilder.append('.')
                domainBuilder.append(String(dnsPayload, offset, len, Charsets.US_ASCII))
                offset += len
            }
            return if (domainBuilder.isNotEmpty()) domainBuilder.toString() else null
        } catch (_: Exception) {
            return null
        }
    }

    private fun buildNxDomainResponse(query: ByteArray): ByteArray {
        val resp = query.clone()
        if (resp.size >= 4) {
            // Set QR=1 (Response), RCODE=3 (NXDOMAIN)
            resp[2] = (resp[2].toInt() or 0x81).toByte()
            resp[3] = (resp[3].toInt() or 0x03).toByte()
        }
        return resp
    }

    private fun buildUdpIpPacket(srcIp: String, dstIp: String, srcPort: Int, dstPort: Int, payload: ByteArray): ByteArray {
        val totalLength = 20 + 8 + payload.size
        val packet = ByteArray(totalLength)

        // IPv4 Header
        packet[0] = 0x45.toByte() // Version 4, Header length 5 (20 bytes)
        packet[1] = 0.toByte()
        packet[2] = ((totalLength shr 8) and 0xFF).toByte()
        packet[3] = (totalLength and 0xFF).toByte()
        packet[4] = 0; packet[5] = 0 // Identification
        packet[6] = 0x40.toByte(); packet[7] = 0 // Don't fragment
        packet[8] = 64.toByte() // TTL
        packet[9] = 17.toByte() // UDP Protocol

        val srcBytes = InetAddress.getByName(srcIp).address
        val dstBytes = InetAddress.getByName(dstIp).address
        System.arraycopy(srcBytes, 0, packet, 12, 4)
        System.arraycopy(dstBytes, 0, packet, 16, 4)

        // Compute IP Header Checksum
        var checksum = 0
        for (i in 0 until 20 step 2) {
            if (i == 10) continue
            val word = ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            checksum += word
        }
        while (checksum shr 16 != 0) {
            checksum = (checksum and 0xFFFF) + (checksum shr 16)
        }
        checksum = checksum.inv() and 0xFFFF
        packet[10] = ((checksum shr 8) and 0xFF).toByte()
        packet[11] = (checksum and 0xFF).toByte()

        // UDP Header
        val udpLength = 8 + payload.size
        packet[20] = ((srcPort shr 8) and 0xFF).toByte()
        packet[21] = (srcPort and 0xFF).toByte()
        packet[22] = ((dstPort shr 8) and 0xFF).toByte()
        packet[23] = (dstPort and 0xFF).toByte()
        packet[24] = ((udpLength shr 8) and 0xFF).toByte()
        packet[25] = (udpLength and 0xFF).toByte()
        packet[26] = 0; packet[27] = 0 // Checksum optional for IPv4 UDP

        // DNS Payload
        System.arraycopy(payload, 0, packet, 28, payload.size)

        return packet
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
