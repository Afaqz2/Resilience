package com.resilience.app.ui.radio

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.data.ai.VoiceAgentState
import com.resilience.app.data.audio.PttState
import com.resilience.app.data.mesh.BleState
import com.resilience.app.data.mesh.MeshState
import com.resilience.app.ui.components.TacticalScannerOverlay
import com.resilience.app.ui.theme.*

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun RadioFrequencyScreen(
    onBack: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    viewModel: RadioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── Permission launchers (one per permission type) ──────────────────────
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onPermissionResult() }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.onPermissionResult() }

    val nearbyWifiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onPermissionResult() }

    val bleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { viewModel.onPermissionResult() }

    // Ask for mic on first entry (just once)
    LaunchedEffect(Unit) {
        if (!uiState.hasAudioPermission) {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Scanner sheet ────────────────────────────────────────────────────────
    if (uiState.isScannerSheetOpen) {
        FrequencyScannerSheet(
            nearbyByFrequency  = uiState.nearbyByFrequency,
            nearbyWifiNetworks = uiState.nearbyWifiNetworks,
            nearbyBleDevices   = uiState.nearbyBleDevices,
            currentFrequency   = uiState.currentFrequency,
            meshState          = uiState.meshState,
            bleState           = uiState.bleState,
            onTuneTo           = { viewModel.onTuneToPeer(it) },
            onDismiss          = { viewModel.onCloseScanner() }
        )
    }

    // ── Hardware guide dialog ─────────────────────────────────────────────────
    if (uiState.isGuideOpen) {
        HardwareGuideDialog(
            hardware = uiState.hardware,
            onDismiss = { viewModel.onCloseGuide() }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color    = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TacticalScannerOverlay(isProminent = uiState.isAutoTuning)

            // Tuning indicator overlay
            if (uiState.isAutoTuning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = TacticalAccentAmber,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "SCANNING FREQUENCIES...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalAccentAmber,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Header ───────────────────────────────────────────────────
                RadioHeader(
                    onBack   = onBack,
                    onScan   = { viewModel.onOpenScanner() },
                    onGuide  = { viewModel.onOpenGuide() }
                )

                Spacer(Modifier.height(12.dp))

                // ── Hardware Status Panel ─────────────────────────────────────
                HardwareStatusPanel(
                    hardware = uiState.hardware,
                    meshState = uiState.meshState,
                    bleState = uiState.bleState,
                    onRequestMic = {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onRequestLocation = {
                        locationLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    },
                    onRequestNearbyWifi = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            nearbyWifiLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                        }
                    },
                    onRequestBle = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            bleLauncher.launch(arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_ADVERTISE,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ))
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                // ── Frequency Display ─────────────────────────────────────────
                FrequencyDisplay(
                    frequency  = uiState.currentFrequency,
                    statusLine = uiState.statusLine,
                    pttState   = uiState.pttState,
                    agentState = uiState.agentState
                )

                Spacer(Modifier.height(16.dp))

                // ── Frequency Tuner ───────────────────────────────────────────
                FrequencyTuner(
                    frequency         = uiState.currentFrequency,
                    onFrequencyChange = { viewModel.onFrequencyChange(it) },
                    onAutoTune        = { viewModel.onAutoTune() }
                )

                Spacer(Modifier.height(12.dp))

                // ── Peer Count Row ────────────────────────────────────────────
                PeerCountRow(
                    peerCount  = uiState.nearbyPeers.count { it.frequency == uiState.currentFrequency },
                    totalPeers = uiState.nearbyPeers.size,
                    meshState  = uiState.meshState,
                    bleState   = uiState.bleState
                )

                Spacer(Modifier.weight(1f))

                // ── AI Response / Transcript ──────────────────────────────────
                if (uiState.lastResponse.isNotBlank() || uiState.lastTranscript.isNotBlank()) {
                    TranscriptCard(
                        transcript = uiState.lastTranscript,
                        response   = uiState.lastResponse
                    )
                    Spacer(Modifier.height(12.dp))
                }

                // ── Bottom Controls ───────────────────────────────────────────
                BottomControlRow(
                    isAiMode         = uiState.isAiMode,
                    pttState         = uiState.pttState,
                    agentState       = uiState.agentState,
                    hasPermission    = uiState.hasAudioPermission,
                    onToggleAiMode   = { viewModel.onToggleAiMode() },
                    onPttPress       = { viewModel.onPttPress() },
                    onPttRelease     = { viewModel.onPttRelease() },
                    onNavigateToChat = onNavigateToAiChat
                )

                Spacer(Modifier.height(12.dp))

                // ── Bottom status bar ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(modifier = Modifier.size(6.dp), shape = CircleShape,
                            color = if (uiState.meshState != MeshState.IDLE) TacticalSecondaryOlive else TacticalMuted) {}
                        Text("RF MESH COMMS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalMuted, letterSpacing = 1.sp)
                    }
                    Text(
                        text = "${uiState.nearbyPeers.size} PEERS TOTAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalMuted
                    )
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun RadioHeader(
    onBack: () -> Unit,
    onScan: () -> Unit,
    onGuide: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "RF MESH COMMS",
                style = MaterialTheme.typography.titleMedium,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "OPEN CHANNEL • MULTI-TRANSPORT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        Row {
            // Guide / info button
            IconButton(onClick = onGuide) {
                Icon(Icons.Default.Info, contentDescription = "Hardware Guide",
                    tint = TacticalAccentAmber.copy(alpha = 0.8f))
            }
            // Deep scan button
            IconButton(onClick = onScan) {
                Icon(Icons.Default.Search, contentDescription = "Deep Scan",
                    tint = TacticalAccentAmber)
            }
        }
    }
}

// ── Hardware Status Panel ─────────────────────────────────────────────────────

@Composable
private fun HardwareStatusPanel(
    hardware: HardwareCapabilities,
    meshState: MeshState,
    bleState: BleState,
    onRequestMic: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestNearbyWifi: () -> Unit,
    onRequestBle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, TacticalBorder, RoundedCornerShape(4.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "// HARDWARE STATUS",
            style = MaterialTheme.typography.labelSmall,
            color = TacticalMuted,
            letterSpacing = 2.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Microphone
            HardwareChip(
                modifier    = Modifier.weight(1f),
                icon        = Icons.Default.Mic,
                label       = "MIC",
                isAvailable = true,
                isGranted   = hardware.hasMicPermission,
                isActive    = false,
                onClick     = if (!hardware.hasMicPermission) onRequestMic else null
            )

            // Wi-Fi Aware
            HardwareChip(
                modifier    = Modifier.weight(1f),
                icon        = Icons.Default.Wifi,
                label       = "WI-FI NAN",
                isAvailable = hardware.hasWifiAwareHardware,
                isGranted   = hardware.hasNearbyWifiPermission && hardware.isWifiEnabled,
                isActive    = meshState == MeshState.ADVERTISING || meshState == MeshState.SCANNING,
                onClick     = if (!hardware.hasNearbyWifiPermission) onRequestNearbyWifi else null
            )

            // Bluetooth LE
            HardwareChip(
                modifier    = Modifier.weight(1f),
                icon        = Icons.Default.Bluetooth,
                label       = "BLE",
                isAvailable = hardware.hasBleHardware,
                isGranted   = hardware.hasBleScanPermission && hardware.isBleEnabled,
                isActive    = bleState != BleState.IDLE,
                onClick     = if (!hardware.hasBleScanPermission) onRequestBle else null
            )

            // Location
            HardwareChip(
                modifier    = Modifier.weight(1f),
                icon        = Icons.Default.LocationOn,
                label       = "GPS",
                isAvailable = true,
                isGranted   = hardware.hasLocationPermission,
                isActive    = false,
                onClick     = if (!hardware.hasLocationPermission) onRequestLocation else null
            )
        }
    }
}

@Composable
private fun HardwareChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isAvailable: Boolean,
    isGranted: Boolean,
    isActive: Boolean,
    onClick: (() -> Unit)?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chip_pulse_$label")
    val activeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chip_alpha_$label"
    )

    val borderColor = when {
        isActive   -> TacticalSecondaryOlive.copy(alpha = activeAlpha)
        isGranted  -> TacticalSecondaryOlive.copy(alpha = 0.5f)
        !isGranted && isAvailable -> TacticalAccentAmber.copy(alpha = 0.7f)
        else       -> TacticalDestructive.copy(alpha = 0.4f)
    }

    val dotColor = when {
        isActive  -> TacticalSecondaryOlive
        isGranted -> TacticalSecondaryOlive.copy(alpha = 0.6f)
        isAvailable && !isGranted -> TacticalAccentAmber
        else      -> TacticalDestructive.copy(alpha = 0.6f)
    }

    Surface(
        modifier  = modifier
            .border(1.dp, borderColor, RoundedCornerShape(4.dp)),
        shape     = RoundedCornerShape(4.dp),
        color     = if (!isAvailable) TacticalDestructive.copy(alpha = 0.06f)
                    else if (isGranted) TacticalSecondaryOlive.copy(alpha = 0.08f)
                    else TacticalAccentAmber.copy(alpha = 0.08f),
        onClick   = { onClick?.invoke() },
        enabled   = onClick != null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = dotColor,
                    modifier = Modifier.size(12.dp)
                )
                Surface(modifier = Modifier.size(5.dp), shape = CircleShape, color = dotColor) {}
            }
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = dotColor,
                fontSize  = 8.sp,
                maxLines  = 1,
                textAlign = TextAlign.Center
            )
            // Status label
            Text(
                text = when {
                    !isAvailable         -> "N/A"
                    isActive             -> "ACTIVE"
                    isGranted            -> "READY"
                    else                 -> "TAP"
                },
                style    = MaterialTheme.typography.labelSmall,
                fontSize = 7.sp,
                color    = dotColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Hardware Guide Dialog ─────────────────────────────────────────────────────

@Composable
fun HardwareGuideDialog(
    hardware: HardwareCapabilities,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = TacticalBackground,
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(TacticalPrimaryRust.copy(alpha = 0.5f))
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HARDWARE GUIDE",
                        style = MaterialTheme.typography.titleSmall,
                        color = TacticalText,
                        letterSpacing = 2.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close",
                            tint = TacticalMuted, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(color = TacticalPrimaryRust.copy(alpha = 0.2f))

                GuideEntry(
                    icon        = Icons.Default.Wifi,
                    title       = "WI-FI AWARE (NAN)",
                    range       = "~80–150 m",
                    description = "Best transport. Peer discovery + audio streaming without any router or internet. Requires API 26+.",
                    statusColor = if (hardware.hasWifiAwareHardware && hardware.hasNearbyWifiPermission)
                                        TacticalSecondaryOlive else TacticalAccentAmber,
                    statusLabel = if (!hardware.hasWifiAwareHardware) "NOT SUPPORTED"
                                  else if (!hardware.hasNearbyWifiPermission) "NO PERMISSION"
                                  else "READY"
                )

                GuideEntry(
                    icon        = Icons.Default.Bluetooth,
                    title       = "BLUETOOTH LE",
                    range       = "~15–40 m",
                    description = "Discovery beacon fallback. Broadcasts your channel so others can find you. Low bandwidth — voice routed via Wi-Fi.",
                    statusColor = if (hardware.hasBleHardware && hardware.hasBleScanPermission && hardware.isBleEnabled)
                                        TacticalSecondaryOlive else TacticalAccentAmber,
                    statusLabel = if (!hardware.hasBleHardware) "NOT SUPPORTED"
                                  else if (!hardware.hasBleScanPermission) "NO PERMISSION"
                                  else if (!hardware.isBleEnabled) "BLE OFF"
                                  else "READY"
                )

                GuideEntry(
                    icon        = Icons.Default.Mic,
                    title       = "MICROPHONE",
                    range       = "PTT Input",
                    description = "Required. Audio captured at 16 kHz, Opus-encoded at 16 kbps and broadcast to all peers on the same channel.",
                    statusColor = if (hardware.hasMicPermission) TacticalSecondaryOlive else TacticalAccentAmber,
                    statusLabel = if (hardware.hasMicPermission) "GRANTED" else "TAP MIC CHIP"
                )

                GuideEntry(
                    icon        = Icons.Default.LocationOn,
                    title       = "LOCATION",
                    range       = "Required by OS",
                    description = "Android requires location permission for BLE scanning (API ≤ 30). Not used for tracking.",
                    statusColor = if (hardware.hasLocationPermission) TacticalSecondaryOlive else TacticalAccentAmber,
                    statusLabel = if (hardware.hasLocationPermission) "GRANTED" else "TAP GPS CHIP"
                )

                HorizontalDivider(color = TacticalPrimaryRust.copy(alpha = 0.15f))

                Text(
                    text = "▶▶ AUTO-TUNE scans all discovered channels and jumps to the one with the most active peers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TacticalMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun GuideEntry(
    icon: ImageVector,
    title: String,
    range: String,
    description: String,
    statusColor: Color,
    statusLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape    = RoundedCornerShape(4.dp),
            color    = statusColor.copy(alpha = 0.12f),
            border   = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(statusColor.copy(alpha = 0.3f))
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = statusColor,
                    modifier = Modifier.size(16.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TacticalText, letterSpacing = 1.sp)
                Text(text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = statusColor)
            }
            Text(text = "RANGE: $range",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = TacticalMuted)
            Text(text = description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = TacticalMuted.copy(alpha = 0.8f))
        }
    }
}

// ── Frequency Display ─────────────────────────────────────────────────────────

@Composable
private fun FrequencyDisplay(
    frequency: Int,
    statusLine: String,
    pttState: PttState,
    agentState: VoiceAgentState
) {
    val glowColor by animateColorAsState(
        targetValue = when {
            pttState == PttState.TRANSMITTING      -> TacticalDestructive
            agentState == VoiceAgentState.LISTENING  -> TacticalAccentAmber
            agentState == VoiceAgentState.SPEAKING   -> TacticalSecondaryOlive
            else -> TacticalPrimaryRust.copy(alpha = 0.4f)
        },
        animationSpec = tween(300),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, glowColor, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "CH",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                letterSpacing = 4.sp
            )
            Text(
                text = frequency.toString().padStart(2, '0'),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = glowColor,
                letterSpacing = 8.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = statusLine,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Frequency Tuner ───────────────────────────────────────────────────────────

@Composable
private fun FrequencyTuner(
    frequency: Int,
    onFrequencyChange: (Int) -> Unit,
    onAutoTune: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ◀◀ Step -5
        TunerButton(label = "◀◀", onClick = { onFrequencyChange(frequency - 5) })
        // ◀ Step -1
        TunerButton(label = "◀", onClick = { onFrequencyChange(frequency - 1) })

        // Tick marks visual
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                .border(1.dp, TacticalPrimaryRust.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(11) { i ->
                    val tickFreq = frequency - 5 + i
                    val isCurrent = tickFreq == frequency
                    Box(
                        modifier = Modifier
                            .width(if (isCurrent) 3.dp else 1.dp)
                            .height(if (isCurrent) 28.dp else 14.dp)
                            .background(
                                if (isCurrent) TacticalAccentAmber
                                else TacticalPrimaryRust.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }

        // ▶ Step +1
        TunerButton(label = "▶", onClick = { onFrequencyChange(frequency + 1) })
        // ▶▶ AUTO-TUNE to most audible channel
        TunerButton(
            label   = "▶▶",
            onClick = onAutoTune,
            color   = TacticalAccentAmber
        )
    }

    // Auto-tune hint label
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text  = "▶▶ = AUTO-TUNE",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = TacticalAccentAmber.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun TunerButton(
    label: String,
    onClick: () -> Unit,
    color: Color = TacticalPrimaryRust
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape    = RoundedCornerShape(4.dp),
        color    = MaterialTheme.colorScheme.surface,
        border   = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        onClick  = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, color = color,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ── Peer Count Row ────────────────────────────────────────────────────────────

@Composable
private fun PeerCountRow(
    peerCount: Int,
    totalPeers: Int,
    meshState: MeshState,
    bleState: BleState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, TacticalSecondaryOlive.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val isActive = meshState != MeshState.IDLE || bleState != BleState.IDLE
            Surface(modifier = Modifier.size(8.dp), shape = CircleShape,
                color = if (isActive) TacticalSecondaryOlive else TacticalMuted) {}
            Text(
                text  = if (peerCount == 0) "NO PEERS ON THIS CHANNEL"
                        else "$peerCount PEER${if (peerCount > 1) "S" else ""} ACTIVE • $totalPeers TOTAL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = when (meshState) {
                    MeshState.SCANNING    -> "WIFI SCANNING"
                    MeshState.ADVERTISING -> "WIFI ACTIVE"
                    MeshState.CONNECTED   -> "CONNECTED"
                    MeshState.IDLE        -> "WIFI IDLE"
                },
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = TacticalAccentAmber.copy(alpha = 0.8f)
            )
            if (bleState != BleState.IDLE) {
                Text(
                    text  = "BLE ${bleState.name}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TacticalSecondaryOlive.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ── Transcript Card ───────────────────────────────────────────────────────────

@Composable
private fun TranscriptCard(transcript: String, response: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
            .border(1.dp, TacticalAccentAmber.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (transcript.isNotBlank()) {
            Text("YOU: $transcript",
                style = MaterialTheme.typography.bodySmall,
                color = TacticalAccentAmber.copy(alpha = 0.8f))
        }
        if (response.isNotBlank()) {
            Text("AI: $response",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f))
        }
    }
}

// ── Bottom Controls ───────────────────────────────────────────────────────────

@Composable
private fun BottomControlRow(
    isAiMode: Boolean,
    pttState: PttState,
    agentState: VoiceAgentState,
    hasPermission: Boolean,
    onToggleAiMode: () -> Unit,
    onPttPress: () -> Unit,
    onPttRelease: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI Mode Toggle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape    = RoundedCornerShape(4.dp),
                color    = if (isAiMode) TacticalAccentAmber.copy(alpha = 0.25f)
                           else MaterialTheme.colorScheme.surface,
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAiMode) TacticalAccentAmber else TacticalPrimaryRust.copy(alpha = 0.3f)
                ),
                onClick  = onToggleAiMode
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "AI Mode",
                        tint = if (isAiMode) TacticalAccentAmber
                               else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(if (isAiMode) "AI ON" else "AI",
                style = MaterialTheme.typography.labelSmall,
                color = if (isAiMode) TacticalAccentAmber
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }

        // PTT Button
        PttButton(
            isAiMode      = isAiMode,
            pttState      = pttState,
            agentState    = agentState,
            hasPermission = hasPermission,
            onPress       = onPttPress,
            onRelease     = onPttRelease
        )

        // AI Chat Text Button
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape    = RoundedCornerShape(4.dp),
                color    = MaterialTheme.colorScheme.surface,
                border   = androidx.compose.foundation.BorderStroke(
                    1.dp, TacticalPrimaryRust.copy(alpha = 0.3f)
                ),
                onClick  = onNavigateToChat
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Chat, contentDescription = "AI Chat",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("AI CHAT", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
    }
}

// ── PTT Button ────────────────────────────────────────────────────────────────

@Composable
private fun PttButton(
    isAiMode: Boolean,
    pttState: PttState,
    agentState: VoiceAgentState,
    hasPermission: Boolean,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    val isActive = pttState == PttState.TRANSMITTING || agentState != VoiceAgentState.IDLE

    val scale by animateFloatAsState(
        targetValue    = if (isActive) 0.92f else 1f,
        animationSpec  = spring(stiffness = Spring.StiffnessMediumLow),
        label          = "ptt_scale"
    )

    val pttColor by animateColorAsState(
        targetValue = when {
            !hasPermission                           -> TacticalMuted
            agentState == VoiceAgentState.LISTENING  -> TacticalAccentAmber
            pttState == PttState.TRANSMITTING        -> TacticalDestructive
            isAiMode                                 -> TacticalAccentAmber.copy(alpha = 0.6f)
            else                                     -> TacticalPrimaryRust
        },
        animationSpec = tween(200),
        label = "ptt_color"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(pttColor.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    CircleShape
                )
                .border(2.dp, pttColor, CircleShape)
                .pointerInput(hasPermission) {
                    detectTapGestures(
                        onPress = { _ ->
                            if (hasPermission) {
                                onPress()
                                tryAwaitRelease()
                                onRelease()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isAiMode) Icons.Default.Mic else Icons.Default.Radio,
                    contentDescription = "PTT",
                    tint     = pttColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        pttState == PttState.TRANSMITTING        -> "TX"
                        agentState == VoiceAgentState.LISTENING  -> "..."
                        isAiMode                                 -> "ASK"
                        else                                     -> "PTT"
                    },
                    style       = MaterialTheme.typography.labelSmall,
                    fontWeight  = FontWeight.Bold,
                    color       = pttColor,
                    letterSpacing = 2.sp
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text  = if (!hasPermission) "MIC BLOCKED" else "HOLD TO TALK",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}
