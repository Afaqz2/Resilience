package com.resilience.app.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.ui.components.TacticalCard
import com.resilience.app.ui.components.TacticalIconButton
import com.resilience.app.ui.components.TacticalScannerOverlay

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToPlaybooks: () -> Unit = {},
    onNavigateToFamilyVault: () -> Unit = {},
    onNavigateToMaps: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToRadio: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // METRO 2033: Background noise / scanner overlay. Prominent on Home.
            TacticalScannerOverlay(isProminent = true)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. Header
                SafeReachHeader()

                Spacer(modifier = Modifier.height(24.dp))

                // 2. Emergency Mode Button
                EmergencyModeButton()

                Spacer(modifier = Modifier.height(24.dp))
                
                // 3. System Status
                Text(
                    text = "// NAVIGATION",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelMedium
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // 4. Primary Navigation Grid
                PrimaryNavigationGrid(
                    isCrisisMode = uiState.isCrisisMode,
                    onNavigateToPlaybooks = onNavigateToPlaybooks,
                    onNavigateToFamilyVault = onNavigateToFamilyVault,
                    onNavigateToMaps = onNavigateToMaps,
                    onNavigateToInventory = onNavigateToInventory,
                    onNavigateToRadio = onNavigateToRadio,
                    onNavigateToAiChat = onNavigateToAiChat,
                    onNavigateToAlerts = onNavigateToAlerts,
                    onNavigateToSettings = onNavigateToSettings
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Active Directive (replaces Action Section)
                ActiveDirectiveCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onNavigateToPlaybooks = onNavigateToPlaybooks
                )
                
                // 6. Bottom System Nominal Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.14f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.secondary // Olive
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SYSTEM NOMINAL",
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Text(
                            text = "V 2.4.1",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SafeReachHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo Container
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, // Rust
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SAFEREACH",
                style = MaterialTheme.typography.titleLarge,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "TACTICAL SURVIVAL SYSTEM",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TacticalIconButton(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                onClick = {},
                containerSize = 36.dp,
                iconSize = 16.dp
            )
            TacticalIconButton(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "QR dossier",
                onClick = {},
                containerSize = 36.dp,
                iconSize = 16.dp
            )
        }
    }
}

@Composable
fun EmergencyModeButton() {
    // emergency-pulse: glow expands and contracts on amber color
    val infiniteTransition = rememberInfiniteTransition(label = "emergency")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_radius"
    )
    val amberColor = MaterialTheme.colorScheme.tertiary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .drawBehind {
                // Outer amber glow pulse
                drawRect(
                    color = amberColor.copy(alpha = glowAlpha * 0.25f),
                    size = size.copy(
                        width = size.width + glowRadius * 2,
                        height = size.height + glowRadius * 2
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(-glowRadius, -glowRadius)
                )
            },
        color = MaterialTheme.colorScheme.tertiary, // Amber
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
                .clickable { /* TODO */ }
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.background)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "EMERGENCY MODE",
                color = MaterialTheme.colorScheme.background,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.background)
        }
    }
}

@Composable
fun PrimaryNavigationGrid(
    isCrisisMode: Boolean,
    onNavigateToPlaybooks: () -> Unit,
    onNavigateToFamilyVault: () -> Unit,
    onNavigateToMaps: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToRadio: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    data class NavItemDef(
        val label: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val isActive: Boolean = false,
        val onClick: () -> Unit = {}
    )

    val navItems = listOf(
        NavItemDef("HOME",        Icons.Default.Home,            isActive = true),
        NavItemDef("FAMILY PLAN", Icons.Default.Group,           onClick = onNavigateToFamilyVault),
        NavItemDef("INVENTORY",   Icons.Default.Inventory,       onClick = onNavigateToInventory),
        NavItemDef("PLAYBOOKS",   Icons.Default.MenuBook,        onClick = onNavigateToPlaybooks),
        NavItemDef("MAPS",        Icons.Default.Map,             onClick = onNavigateToMaps),
        NavItemDef("RF COMMS",    Icons.Default.Radio,           onClick = onNavigateToRadio),
        NavItemDef("AI CHAT",     Icons.Default.Psychology,      onClick = onNavigateToAiChat),
        NavItemDef("ALERTS",      Icons.Default.Notifications,   onClick = onNavigateToAlerts),
        NavItemDef("SETTINGS",    Icons.Default.Settings,        onClick = onNavigateToSettings)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(navItems) { item ->
            TacticalCard(
                modifier = Modifier
                    .aspectRatio(1.2f)
                    .clickable { item.onClick() }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            // Active: rust primary; Inactive: muted foreground per spec
                            tint = if (item.isActive) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            modifier = Modifier.size(20.dp) // spec: w-5 h-5
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = item.label,
                            color = if (item.isActive) MaterialTheme.colorScheme.onBackground
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveDirectiveCard(
    modifier: Modifier = Modifier,
    onNavigateToPlaybooks: () -> Unit
) {
    TacticalCard(modifier = modifier.clickable { onNavigateToPlaybooks() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary // Rust
                ) {}
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ACTIVE DIRECTIVE",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "What do I do right now?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Review your emergency playbook. Ensure supplies are stocked. Verify rally points with family members. Stay vigilant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "PRIORITY: MEDIUM",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "24H AGO",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
