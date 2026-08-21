package com.shieldcore.security.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.shieldcore.security.presentation.ui.theme.*
import com.shieldcore.security.presentation.viewmodel.BatteryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryScreen(
    viewModel: BatteryViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val cpuApps by viewModel.cpuApps.collectAsState()
    val isOptimized by viewModel.isOptimized.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shield", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("Core", fontWeight = FontWeight.ExtraBold, color = NeonOrange)
                        Text(" • Battery Optimizer", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 16.sp)
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            batteryInfo?.let { info ->
                val batteryAccent = when {
                    info.isCharging -> NeonCyan
                    info.level > 50 -> MatrixGreen
                    info.level > 20 -> RadiantAmber
                    else -> LaserRed
                }

                // Battery Hero Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkSurfaceElevated,
                    borderBrush = Brush.linearGradient(listOf(batteryAccent.copy(alpha = 0.5f), Color.Transparent))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedPulseOrb(
                            accentColor = batteryAccent,
                            icon = if (info.isCharging) Icons.Default.Bolt else Icons.Default.BatteryStd,
                            size = 110.dp,
                            iconSize = 52.dp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${info.level}%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        NeonBadge(
                            text = if (info.isCharging) "Charging Fast" else "Discharging Normally",
                            accentColor = batteryAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hardware Telemetry Matrix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isHot = info.temperature > 39.0
                    val tempAccent = if (isHot) LaserRed else SkyBlue

                    MetricCard(
                        title = "Temperature",
                        value = "${info.temperature}°C",
                        icon = Icons.Default.Thermostat,
                        accentColor = tempAccent,
                        subtitle = if (isHot) "Running Hot" else "Optimal",
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Health",
                        value = getHealthString(info.health),
                        icon = Icons.Default.HealthAndSafety,
                        accentColor = MatrixGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Voltage",
                        value = "${String.format(java.util.Locale.US, "%.2f", info.voltage / 1000f)} V",
                        icon = Icons.Default.ElectricMeter,
                        accentColor = RadiantAmber,
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "Optimization",
                        value = if (isOptimized) "Max Boost" else "Standard",
                        icon = Icons.Default.Speed,
                        accentColor = ElectricViolet,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (cpuApps.isNotEmpty()) {
                    SectionHeader(title = "Background Memory Consumers", accentColor = NeonOrange)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cpuApps, key = { it }) { appName ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkCardSurface,
                                border = BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(NeonOrange, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(appName, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                GlowGradientButton(
                    text = if (isOptimized) "Optimized (Memory Cleaned)" else "Optimize Power & Clean RAM",
                    onClick = { viewModel.optimize() },
                    icon = Icons.Default.Bolt,
                    gradient = WarningAmberGradient
                )
            }
        }
    }
}

private fun getHealthString(health: Int): String {
    return when (health) {
        2 -> "Good"
        3 -> "Overheat"
        4 -> "Dead"
        5 -> "Over Voltage"
        7 -> "Cold"
        else -> "Good / Normal"
    }
}
