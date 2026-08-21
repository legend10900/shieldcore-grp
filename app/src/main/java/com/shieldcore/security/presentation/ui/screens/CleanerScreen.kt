package com.shieldcore.security.presentation.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.core.utils.FileUtils
import com.shieldcore.security.domain.model.JunkType
import com.shieldcore.security.domain.repository.JunkScanProgress
import com.shieldcore.security.presentation.ui.theme.*
import com.shieldcore.security.presentation.viewmodel.CleanerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanerScreen(
    viewModel: CleanerViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val progress by viewModel.scanProgress.collectAsState()
    val summary by viewModel.cleanSummary.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val context = LocalContext.current

    val hasStoragePermission = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Shield", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("Core", fontWeight = FontWeight.ExtraBold, color = RadiantAmber)
                        Text(" • Junk Cleaner", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 16.sp)
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
            if (!hasStoragePermission) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = DarkCardSurface,
                    borderBrush = Brush.linearGradient(listOf(RadiantAmber.copy(alpha = 0.5f), Color.Transparent))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(RadiantAmber.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = RadiantAmber, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Storage Access Recommended", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                            Text("Grant All Files access for deeper cache and APK cleaning.", fontSize = 11.sp, color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                                    }
                                } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RadiantAmber),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Grant", fontSize = 12.sp, color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            when (val state = progress) {
                is JunkScanProgress.Idle -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedPulseOrb(
                                accentColor = RadiantAmber,
                                icon = Icons.Default.CleaningServices,
                                size = 130.dp,
                                iconSize = 56.dp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Smart Storage Optimizer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Find hidden caches, residual APKs, temp logs, and large residual files.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            GlowGradientButton(
                                text = "Scan Storage for Junk",
                                onClick = { viewModel.startScan() },
                                icon = Icons.Default.Search,
                                gradient = WarningAmberGradient,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }

                is JunkScanProgress.Scanning -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(72.dp),
                                color = RadiantAmber,
                                strokeWidth = 5.dp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "Analyzing Storage...",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                state.currentDir,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            NeonBadge(
                                text = "Identified: ${FileUtils.formatSize(state.currentSize)}",
                                accentColor = RadiantAmber
                            )
                        }
                    }
                }

                is JunkScanProgress.Completed -> {
                    val totalFoundBytes = state.items.sumOf { it.sizeBytes }
                    val selectedBytes = state.items.filter { selectedIds.contains(it.id) }.sumOf { it.sizeBytes }

                    // Storage Overview Glass Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = DarkSurfaceElevated,
                        borderBrush = Brush.linearGradient(listOf(RadiantAmber.copy(alpha = 0.5f), Color.Transparent))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${state.items.size} Cleanable Items Found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Selected: ${FileUtils.formatSize(selectedBytes)} / ${FileUtils.formatSize(totalFoundBytes)}",
                                    fontSize = 12.sp,
                                    color = RadiantAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            TextButton(
                                onClick = {
                                    if (selectedIds.size == state.items.size) {
                                        viewModel.deselectAll()
                                    } else {
                                        viewModel.selectAll(state.items)
                                    }
                                }
                            ) {
                                Text(
                                    if (selectedIds.size == state.items.size) "Deselect All" else "Select All",
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Clean Button
                    GlowGradientButton(
                        text = "Clean Selected (${FileUtils.formatSize(selectedBytes)})",
                        onClick = { viewModel.cleanSelected(state.items) },
                        icon = Icons.Default.DeleteSweep,
                        gradient = WarningAmberGradient,
                        enabled = selectedIds.isNotEmpty()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SectionHeader(
                        title = "Junk Item Breakdown",
                        accentColor = RadiantAmber
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            val isSelected = selectedIds.contains(item.id)
                            val itemAccent = when (item.type) {
                                JunkType.CACHE -> NeonCyan
                                JunkType.OBSOLETE_APK -> RadiantAmber
                                JunkType.TEMP_FILES -> ElectricViolet
                                JunkType.LARGE_FILES -> RoseRed
                                JunkType.EMPTY_FOLDERS -> TextMuted
                            }

                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleItemSelection(item.id) },
                                backgroundColor = if (isSelected) DarkCardSurface else DarkSurface,
                                borderBrush = Brush.linearGradient(
                                    listOf(
                                        if (isSelected) itemAccent.copy(alpha = 0.5f) else DarkCardBorder,
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.toggleItemSelection(item.id) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = RadiantAmber,
                                            uncheckedColor = TextMuted,
                                            checkmarkColor = DarkBackground
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(itemAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (item.type) {
                                                JunkType.CACHE -> Icons.Default.Cached
                                                JunkType.OBSOLETE_APK -> Icons.Default.Android
                                                JunkType.TEMP_FILES -> Icons.Default.Description
                                                JunkType.LARGE_FILES -> Icons.Default.FolderZip
                                                JunkType.EMPTY_FOLDERS -> Icons.Default.FolderOpen
                                            },
                                            contentDescription = null,
                                            tint = itemAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                        Text(
                                            "${item.type.name} • ${item.path}",
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        FileUtils.formatSize(item.sizeBytes),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = itemAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            summary?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissSummary() },
                    containerColor = DarkSurfaceElevated,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = RadiantAmber, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cleanup Complete!", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    },
                    text = {
                        Column {
                            Text(
                                "Successfully freed ${FileUtils.formatSize(it.totalSizeCleaned)} of device storage by removing ${it.itemsRemoved} junk files in ${it.timeTakenMs}ms.",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissSummary() },
                            colors = ButtonDefaults.buttonColors(containerColor = RadiantAmber),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Done", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}
