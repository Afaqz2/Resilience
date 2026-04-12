package com.resilience.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resilience.app.ui.components.TacticalScannerOverlay

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TacticalScannerOverlay(isProminent = false)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                SettingsTopBar(onBack = onBack)

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Operative Profile Card
                    OperativeProfileCard()

                    Spacer(modifier = Modifier.height(24.dp))

                    // Notifications Section
                    SettingsSectionTitle(title = "// NOTIFICATIONS")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        title = "Alert Notifications",
                        subtitle = "Push alerts for threats & updates",
                        checked = true,
                        onCheckedChange = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsToggleItem(
                        icon = Icons.Default.VolumeUp,
                        title = "Sound Alerts",
                        subtitle = "Audible warnings for critical events",
                        checked = true,
                        onCheckedChange = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsTextItem(
                        icon = Icons.Default.Sensors,
                        title = "Emergency Broadcast",
                        subtitle = "Always-on for critical alerts",
                        value = "ALWAYS ON",
                        valueColor = MaterialTheme.colorScheme.secondary // Olive
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // System Section
                    SettingsSectionTitle(title = "// SYSTEM")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingsToggleItem(
                        icon = Icons.Default.Visibility,
                        title = "Dark Mode",
                        subtitle = "Tactical low-light interface",
                        checked = true,
                        onCheckedChange = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsToggleItem(
                        icon = Icons.Default.Wifi,
                        title = "Auto-Sync Data",
                        subtitle = "Sync when connection is available",
                        checked = false,
                        onCheckedChange = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsTextItem(
                        icon = Icons.Default.Storage,
                        title = "Local Storage",
                        subtitle = "2.4 MB / 50 MB used",
                        value = "5%"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Data & Security Section
                    SettingsSectionTitle(title = "// DATA & SECURITY")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SettingsActionItem(
                        icon = Icons.Default.Download,
                        title = "Export Dossier",
                        subtitle = "Download all data as encrypted package",
                        onClick = {}
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsTextItem(
                        icon = Icons.Default.Shield,
                        title = "Security & Encryption",
                        subtitle = "AES-256 - All data encrypted at rest",
                        value = "SECURE",
                        valueColor = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAFEREACH SYSTEM",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "BUILD 2.4.1-METRO",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable { onBack() },
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
            border = borderStroke()
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.padding(8.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary, // Rust
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "V 2.4.1",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun OperativeProfileCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = borderStroke()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PersonOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPERATIVE #4271",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "CLEARANCE: LEVEL 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STATUS: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary // Olive color
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        letterSpacing = 2.sp
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsBaseItem(icon = icon, title = title, subtitle = subtitle) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onBackground,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                uncheckedTrackColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun SettingsTextItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
) {
    SettingsBaseItem(icon = icon, title = title, subtitle = subtitle) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = valueColor,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    SettingsBaseItem(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SettingsBaseItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = borderStroke()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF2A2A2A), RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            trailingContent()
        }
    }
}

@Composable
private fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp, 
    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
)
