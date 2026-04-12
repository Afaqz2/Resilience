package com.resilience.app.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.resilience.app.maps.OfflineMapManager
import com.resilience.app.ui.theme.SafeReachDarkGray

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

// ── POI colours ────────────────────────────────────────────────────────────
private val POI_HOSPITAL = "#E53935"
private val POI_WATER    = "#1E88E5"
private val POI_SHELTER  = "#FB8C00"
private val POI_SHOP     = "#8E24AA"

// ── MapScreen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ── Permission launcher ───────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.onLocationPermissionGranted(context)
        else         viewModel.onLocationPermissionDenied()
    }

    LaunchedEffect(Unit) {
        val alreadyGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)  == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) viewModel.onLocationPermissionGranted(context)
        else                permissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Maps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // POI legend
                    IconButton(onClick = { viewModel.showPoiLegend() }) {
                        Icon(Icons.Default.Info, contentDescription = "POI legend")
                    }
                    // City-preset download
                    IconButton(onClick = { viewModel.showDownloadSheet() }) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Download city map")
                    }
                    // Offline packs indicator — only shown when packs exist
                    if (uiState.downloadedRegions.isNotEmpty()) {
                        IconButton(onClick = { viewModel.showOfflinePacks() }) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = Color(0xFF4CAF50)) {
                                        Text(
                                            text = "${uiState.downloadedRegions.size}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "View offline packs",
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MapLibreComposable(
                modifier              = Modifier.fillMaxSize(),
                hasLocationPermission = uiState.hasLocationPermission,
                currentLocation       = uiState.currentLocation,
                recenterTrigger       = uiState.recenterTrigger
            )

            // ── FABs (right side) ─────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Centre on user
                FloatingActionButton(
                    onClick            = { viewModel.triggerRecenter() },
                    modifier           = Modifier.size(48.dp),
                    containerColor     = Color.White,
                    contentColor       = SafeReachDarkGray,
                    shape              = CircleShape,
                    elevation          = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Centre on me", modifier = Modifier.size(22.dp))
                }
                // Share location
                FloatingActionButton(
                    onClick        = { viewModel.shareLocation(context) },
                    modifier       = Modifier.size(48.dp),
                    containerColor = SafeReachDarkGray,
                    contentColor   = Color.White,
                    shape          = CircleShape,
                    elevation      = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share location", modifier = Modifier.size(20.dp))
                }
            }

            // ── Bottom overlay (mutually exclusive) ───────────────────────
            when {
                uiState.isDownloading -> {
                    DownloadProgressCard(
                        progress   = uiState.downloadProgress ?: 0f,
                        statusText = uiState.downloadStatusText,
                        modifier   = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .fillMaxWidth()
                    )
                }
                uiState.currentLocation != null -> {
                    val loc = uiState.currentLocation!!
                    DownloadAreaCard(
                        lat        = loc.first,
                        lon        = loc.second,
                        onDownload = { viewModel.downloadAroundLocation() },
                        modifier   = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .fillMaxWidth()
                    )
                }
                !uiState.hasLocationPermission -> {
                    PermissionDeniedBanner(
                        onRetry  = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    // ── Sheets ─────────────────────────────────────────────────────────────
    if (uiState.showDownloadSheet) {
        OfflineMapDownloadSheet(
            viewModel = viewModel,
            uiState   = uiState,
            onDismiss = { viewModel.hideDownloadSheet() }
        )
    }
    if (uiState.showPoiLegend) {
        PoiLegendSheet(onDismiss = { viewModel.hidePoiLegend() })
    }
    if (uiState.showOfflinePacks) {
        OfflinePacksSheet(
            regions   = uiState.downloadedRegions,
            viewModel = viewModel,
            onDismiss = { viewModel.hideOfflinePacks() }
        )
    }
}

// ── MapLibre composable ────────────────────────────────────────────────────

@Composable
fun MapLibreComposable(
    modifier: Modifier = Modifier,
    hasLocationPermission: Boolean,
    currentLocation: Pair<Double, Double>?,
    recenterTrigger: Long
) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapRef   = remember { mutableStateOf<MapboxMap?>(null) }
    val styleRef = remember { mutableStateOf<Style?>(null) }

    val mapView = remember {
        Mapbox.getInstance(context)
        MapView(context)
    }

    // Enable blue-dot when permission + style are ready
    LaunchedEffect(hasLocationPermission, styleRef.value) {
        val map   = mapRef.value   ?: return@LaunchedEffect
        val style = styleRef.value ?: return@LaunchedEffect
        if (!hasLocationPermission) return@LaunchedEffect
        try {
            val lc = map.locationComponent
            lc.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style)
                    .useDefaultLocationEngine(true)
                    .build()
            )
            lc.isLocationComponentEnabled = true
            lc.cameraMode = CameraMode.TRACKING
            lc.renderMode = RenderMode.NORMAL
        } catch (_: Exception) {}
    }

    // Animate to first location fix
    LaunchedEffect(currentLocation) {
        val map = mapRef.value ?: return@LaunchedEffect
        currentLocation?.let { (lat, lon) ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 13.0))
        }
    }

    // Re-centre on demand (user tapped FAB)
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger == 0L) return@LaunchedEffect
        val map = mapRef.value ?: return@LaunchedEffect
        currentLocation?.let { (lat, lon) ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 14.0))
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE  -> mapView.onCreate(null)
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else                       -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.also { mv ->
                mv.getMapAsync { map ->
                    mapRef.value = map
                    map.setStyle(OfflineMapManager.DEMO_STYLE_URL) { style ->
                        styleRef.value = style
                        // POI overlay — silent fail if asset missing
                        try {
                            val geojson = context.assets.open("poi_overlay.geojson").bufferedReader().readText()
                            style.addSource(GeoJsonSource("poi-source", geojson))
                            fun layer(id: String, typeVal: String, color: String) =
                                CircleLayer(id, "poi-source")
                                    .withFilter(poiEq("type", typeVal))
                                    .withProperties(
                                        PropertyFactory.circleColor(color),
                                        PropertyFactory.circleRadius(7f),
                                        PropertyFactory.circleStrokeWidth(2f),
                                        PropertyFactory.circleStrokeColor("#FFFFFF")
                                    )
                            style.addLayer(layer("poi-hospitals", "hospital", POI_HOSPITAL))
                            style.addLayer(layer("poi-water",     "water",    POI_WATER))
                            style.addLayer(layer("poi-shelters",  "shelter",  POI_SHELTER))
                            style.addLayer(layer("poi-shops",     "shop",     POI_SHOP))
                        } catch (_: Exception) {}
                    }
                }
            }
        },
        modifier = modifier
    )
}

private fun poiEq(key: String, value: String) =
    com.mapbox.mapboxsdk.style.expressions.Expression.eq(
        com.mapbox.mapboxsdk.style.expressions.Expression.get(key),
        com.mapbox.mapboxsdk.style.expressions.Expression.literal(value)
    )

// ── POI Legend Sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiLegendSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            item {
                Text(
                    text     = "Map Legend",
                    style    = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                Text(
                    text     = "${BUNDLED_POIS.size} bundled points of interest across Karachi, Lahore & Islamabad",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(16.dp))
                // Legend key
                listOf(
                    Triple("Hospitals",     POI_HOSPITAL, "🔴"),
                    Triple("Water Sources", POI_WATER,    "🔵"),
                    Triple("Shelters",      POI_SHELTER,  "🟠"),
                    Triple("Shops",         POI_SHOP,     "🟣"),
                ).forEach { (label, hex, emoji) ->
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("$emoji  $label", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
            }

            // Group POIs by type then city
            val byType = BUNDLED_POIS.groupBy { it.type }
            listOf("hospital" to "🔴 Hospitals", "water" to "🔵 Water Sources", "shelter" to "🟠 Shelters", "shop" to "🟣 Shops")
                .forEach { (typeKey, header) ->
                    val entries = byType[typeKey] ?: return@forEach
                    item {
                        Text(
                            text     = header,
                            style    = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    items(entries) { poi ->
                        Row(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("•  ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            Column {
                                Text(poi.name, style = MaterialTheme.typography.bodySmall)
                                Text(poi.city, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
        }
    }
}

// ── Offline Packs Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflinePacksSheet(
    regions: List<com.resilience.app.data.db.entity.OfflineRegionEntity>,
    viewModel: MapViewModel,
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
                .padding(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text  = "Downloaded Maps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text     = "These maps open fully offline — no internet needed.",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(12.dp))

                // Total storage summary
                val totalMb = regions.sumOf { it.sizeBytes } / 1_048_576f
                Surface(
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    color    = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${regions.size} pack${if (regions.size > 1) "s" else ""} stored", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("${"%.1f".format(totalMb)} MB total", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(8.dp))
            }

            items(regions, key = { it.id }) { region ->
                OfflinePackRow(
                    region   = region,
                    onDelete = { viewModel.deleteRegion(region) },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun OfflinePackRow(
    region: com.resilience.app.data.db.entity.OfflineRegionEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirm by remember { mutableStateOf(false) }
    val sizeMb = region.sizeBytes / 1_048_576f
    val date   = remember(region.downloadedAt) {
        java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(region.downloadedAt))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Map, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(region.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sizeMb > 0f) Text("${"%.1f".format(sizeMb)} MB", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50))
                    Text("Downloaded $date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373))
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete ${region.name}?") },
            text  = { Text("Tile pack will be permanently removed. You can re-download it later.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; onDelete() }) {
                    Text("Delete", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ── Bottom overlay cards ───────────────────────────────────────────────────

@Composable
fun DownloadAreaCard(
    lat: Double, lon: Double,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SafeReachDarkGray),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudDownload, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Download area around you", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("~20 km × 20 km · zoom 12–15 · fully offline", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onDownload,
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape   = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Download", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DownloadProgressCard(
    progress: Float,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SafeReachDarkGray),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CloudDownload, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Downloading map tiles…", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth(),
                color      = Color(0xFF4CAF50),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(statusText, color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
                Text("${(progress * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PermissionDeniedBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF37474F)),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOff, null, tint = Color(0xFFFFB74D), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Location permission needed", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Required to show your position and download the area around you.", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(10.dp)) {
                Text("Allow", color = Color.White)
            }
        }
    }
}
