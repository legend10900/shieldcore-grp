package com.shieldcore.security.presentation.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.presentation.ui.theme.*
import com.shieldcore.security.presentation.viewmodel.AppLockViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    viewModel: AppLockViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shield", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("Core", fontWeight = FontWeight.ExtraBold, color = ElectricViolet)
                        Text(" • App Locker", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 16.sp)
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
            // Accessibility service advisory card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceElevated,
                borderBrush = Brush.linearGradient(listOf(ElectricViolet.copy(alpha = 0.5f), Color.Transparent))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(ElectricViolet.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ElectricViolet, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accessibility Service Required", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                        Text("Enable ShieldCore Accessibility to detect when locked apps open.", fontSize = 11.sp, color = TextSecondary)
                    }
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Enable", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Search installed applications...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricViolet) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricViolet,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricViolet)
                }
            } else {
                val lockedCount = uiState.installedApps.count { it.isLocked }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(
                        title = "Installed Applications",
                        accentColor = ElectricViolet,
                        modifier = Modifier.weight(1f)
                    )
                    NeonBadge(
                        text = "$lockedCount / ${uiState.installedApps.size} Locked",
                        accentColor = if (lockedCount > 0) ElectricViolet else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(uiState.filteredApps, key = { it.packageName }) { app ->
                        val cardBorderColor = if (app.isLocked) ElectricViolet else DarkCardBorder

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = if (app.isLocked) DarkSurfaceElevated else DarkCardSurface,
                            borderBrush = Brush.linearGradient(
                                listOf(
                                    if (app.isLocked) ElectricViolet.copy(alpha = 0.5f) else DarkCardBorder,
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background((if (app.isLocked) ElectricViolet else TextMuted).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (app.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (app.isLocked) ElectricViolet else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text(app.packageName, fontSize = 10.sp, color = TextMuted, maxLines = 1)
                                }
                                Switch(
                                    checked = app.isLocked,
                                    onCheckedChange = { viewModel.toggleAppLock(app.packageName, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ElectricViolet,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkBackground
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
