package com.resilience.app.ui.radio

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resilience.app.data.mesh.BleState
import com.resilience.app.data.mesh.MeshState
import com.resilience.app.data.mesh.NearbyBleDevice
import com.resilience.app.data.mesh.NearbyPeer
import com.resilience.app.data.mesh.NearbyWifiNetwork
import com.resilience.app.ui.theme.*

// ── Sheet Tabs ────────────────────────────────────────────────────────────────

private enum class ScannerTab(val label: String, val icon: ImageVector) {
    PEERS("PEERS", Icons.Default.People),
    WIFI("WI-FI", Icons.Default.Wifi),
    BLE("BLE", Icons.Default.Bluetooth)
}

// ── Main Sheet ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyScannerSheet(
    nearbyByFrequency: Map<Int, List<NearbyPeer>>,
    nearbyWifiNetworks: List<NearbyWifiNetwork>,
    nearbyBleDevices: List<NearbyBleDevice>,
    currentFrequency: Int,
    meshState: MeshState,
    bleState: BleState,
    onTuneTo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ScannerTab.PEERS) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = TacticalBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(TacticalPrimaryRust.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "FREQUENCY SCANNER",
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 2.sp,
                        color = TacticalText
                    )
                    Text(
                        text = "MULTI-TRANSPORT DEEP SCAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalMuted
                    )
                }
                ScannerStatusBadge(meshState = meshState, bleState = bleState)
            }

            HorizontalDivider(color = TacticalPrimaryRust.copy(alpha = 0.2f))
            Spacer(Modifier.height(10.dp))

            // ── Summary Counts ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryChip(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Default.People,
                    count    = nearbyByFrequency.values.sumOf { it.size },
                    label    = "PEERS",
                    color    = TacticalPrimaryRust
                )
                SummaryChip(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Default.Wifi,
                    count    = nearbyWifiNetworks.size,
                    label    = "WIFI NETS",
                    color    = TacticalSecondaryOlive
                )
                SummaryChip(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Default.Bluetooth,
                    count    = nearbyBleDevices.size,
                    label    = "BLE DEVS",
                    color    = TacticalAccentAmber
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Tab Selector ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScannerTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(4.dp),
                        color    = if (isSelected) TacticalPrimaryRust.copy(alpha = 0.2f)
                                   else TacticalSurface,
                        border   = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) TacticalPrimaryRust else TacticalBorder
                        ),
                        onClick  = { selectedTab = tab }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(tab.icon, contentDescription = null,
                                tint = if (isSelected) TacticalPrimaryRust else TacticalMuted,
                                modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isSelected) TacticalPrimaryRust else TacticalMuted)
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Tab Content ───────────────────────────────────────────────────
            when (selectedTab) {
                ScannerTab.PEERS -> PeersTabContent(
                    nearbyByFrequency = nearbyByFrequency,
                    currentFrequency  = currentFrequency,
                    meshState         = meshState,
                    onTuneTo          = onTuneTo
                )
                ScannerTab.WIFI -> WifiTabContent(
                    networks  = nearbyWifiNetworks,
                    onTuneTo  = onTuneTo
                )
                ScannerTab.BLE -> BleTabContent(
                    devices  = nearbyBleDevices,
                    onTuneTo = onTuneTo
                )
            }
        }
    }
}

// ── Peers Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun PeersTabContent(
    nearbyByFrequency: Map<Int, List<NearbyPeer>>,
    currentFrequency: Int,
    meshState: MeshState,
    onTuneTo: (Int) -> Unit
) {
    if (meshState == MeshState.IDLE && nearbyByFrequency.isEmpty()) {
        NoMeshSupportMessage()
    } else if (nearbyByFrequency.isEmpty()) {
        ScanningEmptyState(meshState = meshState, label = "SCANNING FOR MESH PEERS...")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items(nearbyByFrequency.entries.toList()) { (freq, peers) ->
                FrequencyGroupCard(
                    frequency        = freq,
                    peers            = peers,
                    isCurrentChannel = freq == currentFrequency,
                    onTuneTo         = { onTuneTo(freq) }
                )
            }
        }
    }
}

// ── Wi-Fi Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun WifiTabContent(
    networks: List<NearbyWifiNetwork>,
    onTuneTo: (Int) -> Unit
) {
    if (networks.isEmpty()) {
        ScanningEmptyState(meshState = null, label = "NO WI-FI NETWORKS FOUND")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items(networks) { network ->
                WifiNetworkCard(network = network, onTuneTo = onTuneTo)
            }
        }
    }
}

@Composable
private fun WifiNetworkCard(
    network: NearbyWifiNetwork,
    onTuneTo: (Int) -> Unit
) {
    val borderColor = if (network.isSaveReachPeer) TacticalAccentAmber else TacticalBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(enabled = network.isSaveReachPeer && network.peerFrequency != null) {
                network.peerFrequency?.let { onTuneTo(it) }
            },
        shape = RoundedCornerShape(4.dp),
        color = if (network.isSaveReachPeer) TacticalAccentAmber.copy(alpha = 0.06f) else TacticalSurface
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null,
                tint = if (network.isSaveReachPeer) TacticalAccentAmber else TacticalMuted,
                modifier = Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = network.ssid,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (network.isSaveReachPeer) TacticalAccentAmber else TacticalText,
                        maxLines = 1)
                    if (network.isSaveReachPeer) {
                        Text(text = "SAVEREACH",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = TacticalAccentAmber)
                    }
                }
                Text(text = network.bssid,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TacticalMuted)
            }

            Column(horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SignalIndicator(dbm = network.rssi)
                Text(text = "${network.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = TacticalMuted)
                if (network.isSaveReachPeer && network.peerFrequency != null) {
                    Text(text = "CH ${network.peerFrequency.toString().padStart(2,'0')}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = TacticalAccentAmber)
                }
            }
        }
    }
}

// ── BLE Tab ───────────────────────────────────────────────────────────────────

@Composable
private fun BleTabContent(
    devices: List<NearbyBleDevice>,
    onTuneTo: (Int) -> Unit
) {
    if (devices.isEmpty()) {
        ScanningEmptyState(meshState = null, label = "NO BLE DEVICES FOUND")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            items(devices) { device ->
                BleDeviceCard(device = device, onTuneTo = onTuneTo)
            }
        }
    }
}

@Composable
private fun BleDeviceCard(
    device: NearbyBleDevice,
    onTuneTo: (Int) -> Unit
) {
    val borderColor = if (device.isSaveReachPeer) TacticalSecondaryOlive else TacticalBorder

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(enabled = device.isSaveReachPeer && device.peerFrequency != null) {
                device.peerFrequency?.let { onTuneTo(it) }
            },
        shape = RoundedCornerShape(4.dp),
        color = if (device.isSaveReachPeer) TacticalSecondaryOlive.copy(alpha = 0.06f) else TacticalSurface
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null,
                tint = if (device.isSaveReachPeer) TacticalSecondaryOlive else TacticalMuted,
                modifier = Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (device.isSaveReachPeer && device.peerCallSign != null)
                                   device.peerCallSign else device.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (device.isSaveReachPeer) TacticalSecondaryOlive else TacticalText,
                        maxLines = 1
                    )
                    if (device.isSaveReachPeer) {
                        Text(text = "PEER",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = TacticalSecondaryOlive)
                    }
                }
                Text(text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = TacticalMuted)
            }

            Column(horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SignalIndicator(dbm = device.rssi)
                Text(text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    color = TacticalMuted)
                if (device.isSaveReachPeer && device.peerFrequency != null) {
                    Text(text = "CH ${device.peerFrequency.toString().padStart(2,'0')}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = TacticalSecondaryOlive)
                }
            }
        }
    }
}

// ── Shared Sub-composables ────────────────────────────────────────────────────

@Composable
private fun ScannerStatusBadge(meshState: MeshState, bleState: BleState) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_anim")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_pulse"
    )

    val isScanning = meshState == MeshState.SCANNING || meshState == MeshState.ADVERTISING ||
                     bleState != BleState.IDLE

    Surface(
        shape  = RoundedCornerShape(4.dp),
        color  = if (isScanning) TacticalSecondaryOlive.copy(alpha = 0.2f) else TacticalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalSecondaryOlive.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Surface(modifier = Modifier.size(5.dp), shape = CircleShape,
                color = if (isScanning) TacticalSecondaryOlive.copy(alpha = alpha) else TacticalMuted) {}
            Text(
                text  = if (isScanning) "SCANNING" else "IDLE",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = if (isScanning) TacticalSecondaryOlive else TacticalMuted
            )
        }
    }
}

@Composable
private fun SummaryChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(4.dp),
        color    = color.copy(alpha = 0.08f),
        border   = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
            Text(text = "$count", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold, color = color, fontSize = 11.sp)
            Text(text = label, style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun FrequencyGroupCard(
    frequency: Int,
    peers: List<NearbyPeer>,
    isCurrentChannel: Boolean,
    onTuneTo: () -> Unit
) {
    val borderColor = if (isCurrentChannel) TacticalAccentAmber else TacticalPrimaryRust.copy(alpha = 0.3f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onTuneTo() },
        shape = RoundedCornerShape(4.dp),
        color = TacticalSurface
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CH ${frequency.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentChannel) TacticalAccentAmber else TacticalText,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "${peers.size} PEER${if (peers.size > 1) "S" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalMuted
                    )
                }
                if (isCurrentChannel) {
                    Text("CURRENT", style = MaterialTheme.typography.labelSmall,
                        color = TacticalAccentAmber)
                } else {
                    Surface(
                        shape  = RoundedCornerShape(4.dp),
                        color  = TacticalPrimaryRust.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TacticalPrimaryRust.copy(alpha = 0.4f))
                    ) {
                        Text("TUNE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalPrimaryRust)
                    }
                }
            }

            if (peers.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                peers.take(3).forEach { peer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null,
                            tint = TacticalMuted, modifier = Modifier.size(12.dp))
                        Text(text = peer.callSign,
                            style = MaterialTheme.typography.labelSmall,
                            color = TacticalText.copy(alpha = 0.7f))
                        Text(text = peer.transport.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = TacticalMuted)
                        Spacer(Modifier.weight(1f))
                        SignalIndicator(dbm = peer.signalStrength)
                    }
                }
                if (peers.size > 3) {
                    Text("+${peers.size - 3} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = TacticalMuted,
                        modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SignalIndicator(dbm: Int) {
    val bars = when {
        dbm >= -50 -> 4
        dbm >= -65 -> 3
        dbm >= -75 -> 2
        else       -> 1
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment     = Alignment.Bottom,
        modifier              = Modifier.height(12.dp)
    ) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((4 + i * 2).dp)
                    .background(
                        if (i < bars) TacticalSecondaryOlive else TacticalMuted.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
private fun ScanningEmptyState(meshState: MeshState?, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Search, contentDescription = null,
                tint = TacticalPrimaryRust.copy(alpha = 0.4f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (meshState == MeshState.SCANNING) label else label,
                style = MaterialTheme.typography.titleSmall,
                color = TacticalText.copy(alpha = 0.5f),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Devices must be within range with RF Mesh enabled",
                style = MaterialTheme.typography.bodySmall,
                color = TacticalMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NoMeshSupportMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.WifiOff, contentDescription = null,
                tint = TacticalDestructive.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "WI-FI AWARE NOT SUPPORTED",
                style = MaterialTheme.typography.titleSmall,
                color = TacticalDestructive,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "This device lacks Wi-Fi Aware (NAN) hardware.\nBLE fallback is active for discovery.",
                style = MaterialTheme.typography.bodySmall,
                color = TacticalMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
