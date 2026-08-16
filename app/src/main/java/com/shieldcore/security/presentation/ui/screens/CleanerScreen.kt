package com.shieldcore.security.presentation.ui.screens

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shieldcore.security.core.utils.FileUtils
import com.shieldcore.security.domain.model.JunkType
import com.shieldcore.security.domain.repository.JunkScanProgress
import com.shieldcore.security.presentation.viewmodel.CleanerViewModel

@Composable
fun CleanerScreen(viewModel: CleanerViewModel = hiltViewModel()) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Smart Junk Cleaner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Clear hidden caches, residual APK files, thumbnails, and temporary logs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!hasStoragePermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Storage Permission Recommended", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Grant All Files access to clean residual APKs and external logs.", fontSize = 11.sp)
                    }
                    TextButton(onClick = {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                            }
                        } catch (_: Exception) {}
                    }) {
                        Text("Grant")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        when (val state = progress) {
            is JunkScanProgress.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No active junk scan", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.startScan() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Storage for Junk")
                        }
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
                        CircularProgressIndicator(modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Analyzing: ${state.currentDir}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Found so far: ${FileUtils.formatSize(state.currentSize)}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            is JunkScanProgress.Completed -> {
                val totalFoundBytes = state.items.sumOf { it.sizeBytes }
                val selectedBytes = state.items.filter { selectedIds.contains(it.id) }.sumOf { it.sizeBytes }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${state.items.size} Junk Items Found", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Selected: ${FileUtils.formatSize(selectedBytes)} of ${FileUtils.formatSize(totalFoundBytes)}", fontSize = 12.sp)
                        }

                        Row {
                            TextButton(onClick = {
                                if (selectedIds.size == state.items.size) {
                                    viewModel.deselectAll()
                                } else {
                                    viewModel.selectAll(state.items)
                                }
                            }) {
                                Text(if (selectedIds.size == state.items.size) "Deselect" else "Select All")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.cleanSelected(state.items) },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clean Selected (${FileUtils.formatSize(selectedBytes)})")
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleItemSelection(item.id) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "${item.type.name} • ${item.path}",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    FileUtils.formatSize(item.sizeBytes),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
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
                title = { Text("Cleaning Complete") },
                text = { Text("Successfully freed ${FileUtils.formatSize(it.totalSizeCleaned)} by removing ${it.itemsRemoved} files in ${it.timeTakenMs}ms.") },
                confirmButton = {
                    Button(onClick = { viewModel.dismissSummary() }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

