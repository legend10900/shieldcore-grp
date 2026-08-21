package com.shieldcore.security.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.domain.repository.NetworkScanProgress
import com.shieldcore.security.presentation.ui.theme.*
import com.shieldcore.security.presentation.viewmodel.NetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val wifiDetails by viewModel.wifiDetails.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val dnsIntact by viewModel.dnsIntact.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shield", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("Core", fontWeight = FontWeight.ExtraBold, color = SkyBlue)
                        Text(" • Network Audit", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 16.sp)
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Wi-Fi Connection Glass Card
            wifiDetails?.let { wifi ->
                val isStrongSignal = wifi.signalStrength > -65
                val signalColor = if (isStrongSignal) MatrixGreen else RadiantAmber

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkSurfaceElevated,
                    borderBrush = Brush.linearGradient(listOf(SkyBlue.copy(alpha = 0.5f), Color.Transparent))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(SkyBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, SkyBlue.copy(alpha = 0.35f)), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wifi.ssid, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Security: ${wifi.securityProtocol}", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                        NeonBadge(
                            text = "${wifi.signalStrength} dBm",
                            accentColor = signalColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkCardBorder)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("BSSID", fontSize = 10.sp, color = TextMuted)
                            Text(wifi.bssid, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Frequency", fontSize = 10.sp, color = TextMuted)
                            Text("${wifi.frequency} MHz", fontSize = 11.sp, color = SkyBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = (if (dnsIntact == true) MatrixGreen else LaserRed).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, (if (dnsIntact == true) MatrixGreen else LaserRed).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (dnsIntact == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (dnsIntact == true) MatrixGreen else LaserRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dnsIntact == true) "DNS Server Integrity: Verified & Encrypted" else "DNS Integrity: Unverified or Open Server",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dnsIntact == true) MatrixGreen else LaserRed
                            )
                        }
                    }
                }
            } ?: GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceElevated,
                borderBrush = Brush.linearGradient(listOf(DarkCardBorder, Color.Transparent))
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Not connected to Wi-Fi. (Cellular / Offline)", color = TextSecondary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scan Action Button
            GlowGradientButton(
                text = "Scan Connected Subnet Devices",
                onClick = { viewModel.startNetworkScan() },
                icon = Icons.Default.Radar,
                gradient = PrimaryGradient
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (val state = scanProgress) {
                is NetworkScanProgress.Discovery -> {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = DarkCardSurface
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Probing ARP Subnet Hosts...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${state.percentage}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            GradientProgressBar(
                                progress = state.percentage / 100f,
                                gradient = PrimaryGradient
                            )
                        }
                    }
                }

                is NetworkScanProgress.Completed -> {
                    SectionHeader(
                        title = "Connected LAN Devices (${state.devices.size})",
                        accentColor = SkyBlue
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (state.devices.isEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = DarkCardSurface
                        ) {
                            Text("No reachable LAN hosts identified.", fontSize = 13.sp, color = TextMuted)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.devices, key = { it.ipAddress }) { device ->
                                val isThisDevice = device.hostname?.contains("This Device") == true
                                val isGateway = device.ipAddress.endsWith(".1")
                                val cardBorderColor = if (device.isVulnerable) LaserRed else if (isThisDevice) NeonCyan else DarkCardBorder
                                val iconColor = if (device.isVulnerable) LaserRed else if (isGateway) ElectricViolet else SkyBlue

                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = DarkCardSurface,
                                    borderBrush = Brush.linearGradient(
                                        listOf(cardBorderColor.copy(alpha = 0.6f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                                .border(BorderStroke(1.dp, iconColor.copy(alpha = 0.3f)), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isThisDevice)
                                                    Icons.Default.Smartphone
                                                else if (isGateway)
                                                    Icons.Default.Router
                                                else
                                                    Icons.Default.Devices,
                                                contentDescription = null,
                                                tint = iconColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(device.ipAddress, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                                if (isThisDevice) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    NeonBadge(text = "YOU", accentColor = NeonCyan)
                                                } else if (isGateway) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    NeonBadge(text = "GATEWAY", accentColor = ElectricViolet)
                                                }
                                            }
                                            device.hostname?.let {
                                                if (!isThisDevice) {
                                                    Text(it, fontSize = 11.sp, color = TextSecondary)
                                                }
                                            }
                                            Text("MAC: ${device.macAddress ?: "Unknown"}", fontSize = 10.sp, color = TextMuted)
                                            if (device.openPorts.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    "Open Ports: ${device.openPorts.joinToString(", ")}",
                                                    fontSize = 11.sp,
                                                    color = if (device.isVulnerable) LaserRed else SkyBlue,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}
