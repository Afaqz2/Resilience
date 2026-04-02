package com.resilience.app.ui.maps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resilience.app.data.db.entity.OfflineRegionEntity
import com.resilience.app.maps.CityPreset
import com.resilience.app.ui.theme.SafeReachDarkGray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet for downloading city map packs and managing existing ones.
 *
 * Contains:
 *  • City preset selector (horizontal filter chips)
 *  • Download button that triggers [MapViewModel.downloadRegion]
 *  • List of already-downloaded packs with delete / pause / resume actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapDownloadSheet(
    viewModel: MapViewModel,
    uiState: MapUiState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // ── Section: Download a new pack ───────────────────────────────
            item {
                Text(
                    text = "Download City Map",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Text(
                    text = "Select a city to save its map tiles for offline use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            // City preset chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.cityPresets) { preset ->
                        val isSelected = uiState.selectedPreset?.name == preset.name
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectPreset(preset) },
                            label = { Text(preset.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SafeReachDarkGray,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Download button
            item {
                val preset = uiState.selectedPreset
                Button(
                    onClick = {
                        preset?.let { viewModel.downloadRegion(it) }
                    },
                    enabled = preset != null && !uiState.isDownloading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SafeReachDarkGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (preset != null) "Download ${preset.name}" else "Select a city above",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))

                if (preset != null) {
                    Text(
                        text = "Zoom 10–14 · ~30–50 MB · works fully offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Section: Manage downloaded packs ───────────────────────────
            if (uiState.downloadedRegions.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Downloaded Packs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(uiState.downloadedRegions, key = { it.id }) { region ->
                    DownloadedPackCard(
                        region = region,
                        onDelete = { viewModel.deleteRegion(region) },
                        onPause  = { viewModel.pauseRegion(region.id) },
                        onResume = { viewModel.resumeRegion(region.id) },
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadedPackCard(
    region: OfflineRegionEntity,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(region.downloadedAt) {
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(region.downloadedAt))
    }
    val statusColor = when (region.status) {
        "COMPLETE"    -> Color(0xFF81C784)
        "DOWNLOADING" -> Color(0xFF64B5F6)
        "PAUSED"      -> Color(0xFFFFB74D)
        else          -> Color(0xFFE57373)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = region.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Downloaded $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = region.status,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Pause / Resume (only relevant while downloading)
            if (region.status == "DOWNLOADING") {
                IconButton(onClick = onPause) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color(0xFFFFB74D))
                }
            } else if (region.status == "PAUSED") {
                IconButton(onClick = onResume) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = Color(0xFF64B5F6))
                }
            }

            // Delete
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete pack", tint = Color(0xFFE57373))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${region.name}?") },
            text = { Text("The offline tile pack will be permanently removed. You can download it again later.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Delete", color = Color(0xFFE57373)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
