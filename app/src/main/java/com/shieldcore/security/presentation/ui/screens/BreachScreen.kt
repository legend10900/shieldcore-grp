package com.shieldcore.security.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.shieldcore.security.presentation.viewmodel.BreachViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreachScreen(
    viewModel: BreachViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shield", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("Core", fontWeight = FontWeight.ExtraBold, color = LaserRed)
                        Text(" • Breach Monitor", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 16.sp)
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
            // k-Anonymity Privacy Guarantee Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceElevated,
                borderBrush = Brush.linearGradient(listOf(ElectricViolet.copy(alpha = 0.4f), Color.Transparent))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ElectricViolet.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Mathematical k-Anonymity Protected", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("Only SHA-1 hash prefixes are queried. Your raw email is never transmitted.", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Email Search Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkCardSurface,
                borderBrush = Brush.linearGradient(listOf(LaserRed.copy(alpha = 0.35f), Color.Transparent))
            ) {
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    label = { Text("Enter email address to check...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LaserRed) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LaserRed,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                GlowGradientButton(
                    text = "Scan Global Leaks & Breaches",
                    onClick = { viewModel.checkEmailBreaches() },
                    icon = Icons.Default.LockReset,
                    gradient = DangerRedGradient,
                    isLoading = uiState.isLoading
                )

                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error, color = LaserRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.hasSearched && !uiState.isLoading) {
                if (uiState.breaches.isEmpty()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = DarkSurfaceElevated,
                        borderBrush = Brush.linearGradient(listOf(MatrixGreen.copy(alpha = 0.5f), Color.Transparent))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MatrixGreen, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("Clean Record: No Breaches Found", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                                Text("This email does not appear in known public breach compilations.", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    SectionHeader(
                        title = "Compromised Breaches (${uiState.breaches.size})",
                        accentColor = LaserRed
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.breaches, key = { it.title + it.date }) { breach ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = DarkCardSurface,
                                borderBrush = Brush.linearGradient(listOf(LaserRed.copy(alpha = 0.5f), Color.Transparent))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = LaserRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(breach.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                    Spacer(modifier = Modifier.weight(1f))
                                    NeonBadge(text = breach.date, accentColor = LaserRed)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(breach.description, fontSize = 12.sp, color = TextSecondary)

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Exposed Sensitive Data:", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(breach.dataClasses, key = { it }) { dataClass ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = LaserRed.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, LaserRed.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = dataClass,
                                                color = LaserRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
