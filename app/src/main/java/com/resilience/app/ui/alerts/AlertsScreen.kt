package com.resilience.app.ui.alerts

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.resilience.app.data.model.*
import com.resilience.app.ui.components.TacticalCard
import com.resilience.app.ui.components.TacticalScannerOverlay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ── Severity colour palette ────────────────────────────────────────────────

private val ColorExtreme  = Color(0xFFB33030)  // Tactical Destructive
private val ColorSevere   = Color(0xFFB85C38)  // Tactical Rust
private val ColorModerate = Color(0xFFD4A017)  // Tactical Amber
private val ColorMinor    = Color(0xFF4A5A3A)  // Tactical Olive
private val ColorUnknown  = Color(0xFF8A8A7A)  // Tactical Muted

private fun AlertSeverity.color() = when (this) {
    AlertSeverity.EXTREME  -> ColorExtreme
    AlertSeverity.SEVERE   -> ColorSevere
    AlertSeverity.MODERATE -> ColorModerate
    AlertSeverity.MINOR    -> ColorMinor
    AlertSeverity.UNKNOWN  -> ColorUnknown
}

private fun AlertType.icon(): ImageVector = when (this) {
    AlertType.EARTHQUAKE -> Icons.Default.Vibration
    AlertType.FLOOD      -> Icons.Default.Water
    AlertType.CYCLONE    -> Icons.Default.Air
    AlertType.WILDFIRE   -> Icons.Default.LocalFireDepartment
    AlertType.VOLCANO    -> Icons.Default.Terrain
    AlertType.OTHER      -> Icons.Default.Warning
}

// ── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AlertsTopBar(
                alertCount = uiState.alerts.size,
                lastRefreshedMs = uiState.lastRefreshedMs,
                onBack = onBack,
                onRefresh = viewModel::refresh,
                isRefreshing = uiState.isRefreshing
            )
        },
        bottomBar = {
            // Bottom status bar — required on every screen per spec
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = CircleShape,
                        color = if (uiState.alerts.isNotEmpty()) ColorExtreme
                                else MaterialTheme.colorScheme.secondary
                    ) {}
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.alerts.isNotEmpty()) "THREATS ACTIVE" else "ALL CLEAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
                Text(
                    text = "${uiState.alerts.size} EVENT${if (uiState.alerts.size != 1) "S" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TacticalScannerOverlay(isProminent = false)

            when {
                uiState.isLoading -> LoadingState()
                else -> AlertContent(uiState = uiState)
            }
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsTopBar(
    alertCount: Int,
    lastRefreshedMs: Long?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivePulseIndicator()
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ACTIVE ALERTS",
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 1.sp
                    )
                    if (lastRefreshedMs != null) {
                        val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                        Text(
                            text = "UPDATED ${fmt.format(Date(lastRefreshedMs))}  ·  $alertCount EVENT${if (alertCount != 1) "S" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) {
                    val rotation by rememberInfiniteTransition(label = "spin")
                        .animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
                            label = "rot"
                        )
                    Icon(
                        Icons.Default.Refresh, contentDescription = "Refreshing",
                        modifier = Modifier.rotate(rotation)
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ── Live pulse dot ─────────────────────────────────────────────────────────

@Composable
fun LivePulseIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier.size(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring pulse
        Surface(
            modifier = Modifier
                .size(16.dp)
                .scale(scale),
            shape = CircleShape,
            color = ColorExtreme.copy(alpha = alpha * 0.3f)
        ) {}
        // Inner solid dot
        Surface(
            modifier = Modifier.size(8.dp),
            shape = CircleShape,
            color = ColorExtreme
        ) {}
    }
}

// ── Content ────────────────────────────────────────────────────────────────

@Composable
private fun AlertContent(uiState: AlertsUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Error banner
        if (uiState.error != null) {
            ErrorBanner(message = uiState.error)
        }

        // Location status strip
        LocationStatusStrip(userLat = uiState.userLat, userLon = uiState.userLon)

        AmbientThreatGraph()

        Text(
            "// ACTIVE ALERTS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (uiState.alerts.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.alerts, key = { (alert, _) -> alert.id }) { (alert, distanceKm) ->
                    AlertCard(alert = alert, distanceKm = distanceKm)
                }
            }
        }
    }
}

@Composable
fun AmbientThreatGraph() {
    TacticalCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "AMBIENT THREAT LEVEL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // The bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val bars = listOf(
                    0.3f to ColorMinor,
                    0.5f to ColorMinor,
                    0.4f to ColorMinor,
                    0.6f to ColorSevere,
                    1.0f to ColorExtreme,
                    0.5f to ColorMinor,
                    0.9f to ColorExtreme,
                    0.7f to ColorSevere,
                    1.0f to ColorExtreme,
                    0.8f to ColorSevere,
                    0.7f to ColorSevere,
                    0.4f to ColorMinor
                )

                bars.forEach { (heightFrac, color) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(heightFrac)
                            .padding(horizontal = 2.dp)
                            .background(color = color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "00:00",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Text(
                    "NOW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun AlertCard(alert: ThreatAlert, distanceKm: Double) {
    val severityColor = alert.severity.color()

    TacticalCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
                        .border(1.dp, severityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = alert.type.icon(),
                        contentDescription = null,
                        tint = severityColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = severityColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = alert.type.label.uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = severityColor,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "14 min ago", // Static mockup time
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = severityColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "SECTOR 7-NE", // Static mockup sector override
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.width(16.dp))
                        
                        // Tactical severity blocks mapping
                        val filledBlocks = when (alert.severity) {
                            AlertSeverity.EXTREME -> 5
                            AlertSeverity.SEVERE -> 4
                            AlertSeverity.MODERATE -> 3
                            AlertSeverity.MINOR -> 1
                            AlertSeverity.UNKNOWN -> 2
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(5) { i ->
                                // Optional custom pattern: Draw solid rect or dotted rect
                                Box(
                                    modifier = Modifier
                                        .size(width = 10.dp, height = 8.dp)
                                        .background(
                                            if (i < filledBlocks) severityColor 
                                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
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

// ── Supporting composables ─────────────────────────────────────────────────

@Composable
private fun LocationStatusStrip(userLat: Double?, userLon: Double?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (userLat != null) Icons.Default.LocationOn else Icons.Default.LocationOff,
            contentDescription = null,
            tint = if (userLat != null) MaterialTheme.colorScheme.secondary
                   else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (userLat != null)
                "LOCATION LOCK: ${String.format("%.4f", userLat)}, ${String.format("%.4f", userLon)}"
            else
                "LOCATION UNKNOWN — distances unavailable",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
    }
    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f))
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.SignalWifiOff, contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "SCANNING THREAT FEEDS…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.VerifiedUser, contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "NO ACTIVE THREATS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "All monitored sources report clear.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}
