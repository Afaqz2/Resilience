package com.resilience.app.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.ui.theme.SafeReachDarkGray
import com.resilience.app.ui.theme.SafeReachOffWhite

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToPlaybooks: () -> Unit = {},
    onNavigateToFamilyVault: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 1. Header
            SafeReachHeader()

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Top Control Grid
            ControlGrid(
                isCrisisMode = uiState.isCrisisMode,
                onThemeToggle = { viewModel.toggleCrisisMode(!uiState.isCrisisMode) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Emergency Mode Button
            EmergencyModeButton()

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Primary Navigation Grid
            PrimaryNavigationGrid(
                isCrisisMode = uiState.isCrisisMode,
                onNavigateToPlaybooks = onNavigateToPlaybooks,
                onNavigateToFamilyVault = onNavigateToFamilyVault
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Action Section: What do I do right now?
            ActionSection(onNavigateToPlaybooks = onNavigateToPlaybooks)
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
                .size(56.dp)
                .background(SafeReachDarkGray, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "SafeReach",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                text = "Offline-first emergency companion",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ControlGrid(
    isCrisisMode: Boolean,
    onThemeToggle: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Language Selector
            ControlItem(
                label = "English",
                icon = Icons.Default.Language,
                modifier = Modifier.weight(1f),
                hasDropdown = true
            )
            // QR Dossier
            ControlItem(
                label = "QR dossier",
                icon = Icons.Default.QrCodeScanner,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Saver Toggle
            ControlItem(
                label = "Saver off",
                icon = Icons.Default.BatteryChargingFull,
                modifier = Modifier.weight(1f)
            )
            // Theme Toggle
            ControlItem(
                label = "Theme",
                icon = if (isCrisisMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                modifier = Modifier.weight(1f),
                onClick = onThemeToggle
            )
        }
    }
}

@Composable
fun ControlItem(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    hasDropdown: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        color = SafeReachDarkGray,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
            if (hasDropdown) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EmergencyModeButton() {
    Button(
        onClick = { /* TODO */ },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.FlashOn,
            contentDescription = null,
            tint = Color.Black
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Emergency mode",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun PrimaryNavigationGrid(
    isCrisisMode: Boolean,
    onNavigateToPlaybooks: () -> Unit = {},
    onNavigateToFamilyVault: () -> Unit = {}
) {
    data class NavItemDef(
        val label: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val isActive: Boolean = false,
        val onClick: () -> Unit = {}
    )

    val navItems = listOf(
        NavItemDef("Home",        Icons.Default.Home,            isActive = true),
        NavItemDef("Family plan", Icons.Default.Group,           onClick = onNavigateToFamilyVault),
        NavItemDef("Inventory",   Icons.Default.Inventory),
        NavItemDef("Playbooks",   Icons.Default.MenuBook,        onClick = onNavigateToPlaybooks),
        NavItemDef("Maps",        Icons.Default.Map),
        NavItemDef("Alerts",      Icons.Default.Notifications),
        NavItemDef("Settings",    Icons.Default.Settings)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(navItems) { item ->
            Surface(
                modifier = Modifier
                    .aspectRatio(1.5f)
                    .clickable { item.onClick() },
                shape = RoundedCornerShape(16.dp),
                color = if (item.isActive) Color.White else SafeReachDarkGray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (item.isActive) Color.Black else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            color = if (item.isActive) Color.Black else Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionSection(onNavigateToPlaybooks: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SafeReachDarkGray)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SafeReachDarkGray
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "What do I do right now?",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "One-tap actions work fully offline",
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Local-first",
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sub-action Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPlaybooks() },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Open Playbooks",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "First aid, shelter, water & more",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Surface(
                        color = Color(0xFF1B5E20).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "~30s",
                            color = Color(0xFF81C784),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

data class NavItem(val label: String, val icon: ImageVector, val isActive: Boolean = false)
