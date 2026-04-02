package com.resilience.app.ui.maps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resilience.app.maps.CityPreset
import com.resilience.app.ui.theme.SafeReachDarkGray

/**
 * Bottom sheet for choosing a city preset and downloading its tile pack.
 * Pack management (delete / view sizes) lives in [OfflinePacksSheet] (accessible
 * via the green ✓ icon in the TopAppBar once a pack is downloaded).
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
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = SafeReachDarkGray, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Download City Map", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text     = "Select a city to save its tiles for fully offline use. Roads, buildings, and POIs all work without internet.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── City chips ────────────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding          = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.cityPresets) { preset ->
                        val isSelected = uiState.selectedPreset?.name == preset.name
                        FilterChip(
                            selected = isSelected,
                            onClick  = { viewModel.selectPreset(preset) },
                            label    = { Text(preset.name) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SafeReachDarkGray,
                                selectedLabelColor     = Color.White
                            )
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Size estimate + Download button ───────────────────────────
            item {
                val preset = uiState.selectedPreset
                if (preset != null) {
                    Surface(
                        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        color    = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                "Zoom levels" to "10 – 14",
                                "Est. size"   to "30 – 60 MB",
                                "Coverage"    to "Full city"
                            ).forEach { (label, value) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Button(
                    onClick = { preset?.let { viewModel.downloadRegion(it) } },
                    enabled = preset != null && !uiState.isDownloading,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
                    colors  = ButtonDefaults.buttonColors(containerColor = SafeReachDarkGray),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (preset != null) "Download ${preset.name}" else "Select a city above",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Already-downloaded summary ─────────────────────────────────
            if (uiState.downloadedRegions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                    Spacer(Modifier.height(12.dp))
                    val totalMb = uiState.downloadedRegions.sumOf { it.sizeBytes } / 1_048_576f
                    Text(
                        text     = "${uiState.downloadedRegions.size} pack${if (uiState.downloadedRegions.size > 1) "s" else ""} already downloaded · ${"%.1f".format(totalMb)} MB stored",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Text(
                        text     = "Tap the ✓ icon at the top to manage them.",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
