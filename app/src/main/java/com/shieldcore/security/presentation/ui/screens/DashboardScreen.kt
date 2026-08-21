package com.shieldcore.security.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.shieldcore.security.presentation.ui.theme.*
import com.shieldcore.security.presentation.viewmodel.AntivirusViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavController,
    antivirusViewModel: AntivirusViewModel = hiltViewModel()
) {
    val uiState by antivirusViewModel.uiState.collectAsState()
    val lastScan = uiState.scanHistory.firstOrNull() ?: uiState.lastScanSummary

    val lastScanText = remember(lastScan) {
        if (lastScan != null) {
            val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            "Last scanned: ${sdf.format(Date(lastScan.endTime))}"
        } else {
            "Full device scan recommended"
        }
    }

    val threatsFound = lastScan?.threatsFound ?: 0
    val isSecure = threatsFound == 0

    val toolkitItems = remember {
        listOf(
            ToolkitItemData("Anti-Virus Scan", "Radar & JNI Engine", Icons.Default.Security, MatrixGreen, "scanner"),
            ToolkitItemData("Smart Junk Cleaner", "Storage Optimizer", Icons.Default.CleaningServices, RadiantAmber, "cleaner"),
            ToolkitItemData("Wi-Fi & LAN Audit", "Subnet & ARP Probe", Icons.Default.Wifi, SkyBlue, "network"),
            ToolkitItemData("App Locker", "Biometric Lock", Icons.Default.Lock, ElectricViolet, "applock"),
            ToolkitItemData("Data Breach", "k-Anonymity Leak Check", Icons.Default.LockReset, LaserRed, "breach"),
            ToolkitItemData("Threat Shield", "Scam & Link Verifier", Icons.Default.Policy, CoralTeal, "phishing"),
            ToolkitItemData("Battery Optimizer", "Live Telemetry", Icons.Default.Bolt, NeonOrange, "battery")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Shield",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Core",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan
                        )
                    }
                    Text(
                        text = "Next-Gen Mobile Protection Suite",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = (if (isSecure) MatrixGreen else LaserRed).copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, (if (isSecure) MatrixGreen else LaserRed).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isSecure) MatrixGreen else LaserRed, CircleShape)
                        )
                        Text(
                            text = if (isSecure) "ARMED" else "THREAT",
                            color = if (isSecure) MatrixGreen else LaserRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Hero Security Status Card
        item(key = "hero_card") {
            val statusColor = if (isSecure) MatrixGreen else LaserRed
            val statusGradient = if (isSecure) SafeGreenGradient else DangerRedGradient

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceElevated,
                borderBrush = Brush.linearGradient(
                    listOf(statusColor.copy(alpha = 0.6f), Color(0x2038BDF8))
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pulsing Shield Orb
                    AnimatedPulseOrb(
                        accentColor = statusColor,
                        icon = if (isSecure) Icons.Default.Security else Icons.Default.Warning,
                        size = 88.dp,
                        iconSize = 38.dp
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isSecure) "System Protected" else "Threats Detected",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSecure) "Real-time native engine active" else "$threatsFound security risks identified",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSecure) EmeraldLight else LaserRed,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = lastScanText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { navController.navigate("scanner") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(statusGradient)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Radar,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Scan Now",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Quick Stats Ribbon
        item(key = "live_stats") {
            SectionHeader(title = "Live Security Status", accentColor = NeonCyan)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Antivirus",
                    value = if (threatsFound > 0) "$threatsFound Threats" else "Clean",
                    icon = Icons.Default.Shield,
                    accentColor = if (threatsFound > 0) LaserRed else MatrixGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("scanner") }
                )
                MetricCard(
                    title = "Junk Files",
                    value = "Clean",
                    icon = Icons.Default.CleaningServices,
                    accentColor = RadiantAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("cleaner") }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Network",
                    value = "LAN Audited",
                    icon = Icons.Default.Wifi,
                    accentColor = SkyBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("network") }
                )
                MetricCard(
                    title = "App Locker",
                    value = "Biometric",
                    icon = Icons.Default.Lock,
                    accentColor = ElectricViolet,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("applock") }
                )
            }
        }

        // Security Toolkit Section
        item(key = "toolkit") {
            SectionHeader(title = "Security Toolkit", accentColor = ElectricViolet)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                toolkitItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            ToolkitGridCard(
                                item = item,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate(item.route) }
                            )
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

data class ToolkitItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val route: String
)

@Composable
fun ToolkitGridCard(
    item: ToolkitItemData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.height(118.dp),
        backgroundColor = DarkCardSurface,
        borderBrush = Brush.linearGradient(
            listOf(item.accentColor.copy(alpha = 0.45f), Color.Transparent)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(item.accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, item.accentColor.copy(alpha = 0.35f)), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = item.subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
