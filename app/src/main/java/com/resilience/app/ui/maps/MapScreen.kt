package com.resilience.app.ui.maps

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ── Permission launcher ────────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.onLocationPermissionGranted(context)
        else         viewModel.onLocationPermissionDenied()
    }

    // On first composition: check if permission already held, otherwise ask
    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) viewModel.onLocationPermissionGranted(context)
        else                 permissionLauncher.launch(LOCATION_PERMISSIONS)
    }

    // Show snackbar messages
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
                    // City-preset sheet (alternative to location-based download)
                    IconButton(onClick = { viewModel.showDownloadSheet() }) {
                        Icon(Icons.Default.Download, contentDescription = "Browse city presets")
                    }
                    // Share location shortcut
                    IconButton(onClick = { viewModel.shareLocation(context) }) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Share my location")
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
            // Map — always rendered; location dot appears once permission is granted
            MapLibreComposable(
                modifier = Modifier.fillMaxSize(),
                hasLocationPermission = uiState.hasLocationPermission,
                currentLocation = uiState.currentLocation
            )

            // Offline packs badge (top-right)
            if (uiState.downloadedRegions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = SafeReachDarkGray
                ) {
                    Text(
                        text = "${uiState.downloadedRegions.size} pack${if (uiState.downloadedRegions.size > 1) "s" else ""} offline",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Bottom overlay — three mutually exclusive states:
            when {
                // 1. Actively downloading — show progress
                uiState.isDownloading -> {
                    DownloadProgressCard(
                        progress   = uiState.downloadProgress ?: 0f,
                        statusText = uiState.downloadStatusText,
                        modifier   = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            .fillMaxWidth()
                    )
                }

                // 2. Location obtained — show "Download this area" card
                uiState.currentLocation != null -> {
                    val loc = uiState.currentLocation
                    DownloadAreaCard(
                        lat        = loc.first,
                        lon        = loc.second,
                        onDownload = { viewModel.downloadAroundLocation() },
                        modifier   = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            .fillMaxWidth()
                    )
                }

                // 3. Permission denied — show banner with retry button
                !uiState.hasLocationPermission -> {
                    PermissionDeniedBanner(
                        onRetry  = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    // City-preset download sheet
    if (uiState.showDownloadSheet) {
        OfflineMapDownloadSheet(
            viewModel = viewModel,
            uiState   = uiState,
            onDismiss = { viewModel.hideDownloadSheet() }
        )
    }
}

// ── MapLibre composable ────────────────────────────────────────────────────

@Composable
fun MapLibreComposable(
    modifier: Modifier = Modifier,
    hasLocationPermission: Boolean,
    currentLocation: Pair<Double, Double>?
) {
    val context   = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapRef   = remember { mutableStateOf<MapboxMap?>(null) }
    val styleRef = remember { mutableStateOf<Style?>(null) }

    val mapView = remember {
        Mapbox.getInstance(context)
        MapView(context)
    }

    // Enable the blue-dot location component when permission arrives and style is ready
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
        } catch (_: Exception) { /* location component unavailable */ }
    }

    // Animate camera to user's location once we have a fix
    LaunchedEffect(currentLocation) {
        val map = mapRef.value ?: return@LaunchedEffect
        currentLocation?.let { (lat, lon) ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 13.0))
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
                        // Bundled POI overlay (silent fail if asset missing)
                        try {
                            val geojson = context.assets
                                .open("poi_overlay.geojson")
                                .bufferedReader()
                                .readText()
                            style.addSource(GeoJsonSource("poi-source", geojson))
                            style.addLayer(
                                CircleLayer("poi-hospitals", "poi-source")
                                    .withFilter(eq("type", "hospital"))
                                    .withProperties(
                                        PropertyFactory.circleColor("#E53935"),
                                        PropertyFactory.circleRadius(7f),
                                        PropertyFactory.circleStrokeWidth(2f),
                                        PropertyFactory.circleStrokeColor("#FFFFFF")
                                    )
                            )
                            style.addLayer(
                                CircleLayer("poi-water", "poi-source")
                                    .withFilter(eq("type", "water"))
                                    .withProperties(
                                        PropertyFactory.circleColor("#1E88E5"),
                                        PropertyFactory.circleRadius(7f),
                                        PropertyFactory.circleStrokeWidth(2f),
                                        PropertyFactory.circleStrokeColor("#FFFFFF")
                                    )
                            )
                            style.addLayer(
                                CircleLayer("poi-shelters", "poi-source")
                                    .withFilter(eq("type", "shelter"))
                                    .withProperties(
                                        PropertyFactory.circleColor("#FB8C00"),
                                        PropertyFactory.circleRadius(7f),
                                        PropertyFactory.circleStrokeWidth(2f),
                                        PropertyFactory.circleStrokeColor("#FFFFFF")
                                    )
                            )
                        } catch (_: Exception) {}
                    }
                }
            }
        },
        modifier = modifier
    )
}

private fun eq(key: String, value: String) =
    com.mapbox.mapboxsdk.style.expressions.Expression.eq(
        com.mapbox.mapboxsdk.style.expressions.Expression.get(key),
        com.mapbox.mapboxsdk.style.expressions.Expression.literal(value)
    )

// ── Bottom overlay cards ───────────────────────────────────────────────────

/** Shown when location is known — lets the user download the area around them. */
@Composable
fun DownloadAreaCard(
    lat: Double,
    lon: Double,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = SafeReachDarkGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Download area around you",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = "~20 km × 20 km · zoom 12–15 · works fully offline",
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onDownload,
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                shape   = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Download", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Shown while a tile pack is downloading. */
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
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = "Downloading map tiles…",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth(),
                color      = Color(0xFF81C784),
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = statusText,
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text  = "${(progress * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Shown when the user has denied location permission. */
@Composable
fun PermissionDeniedBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF37474F)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOff,
                contentDescription = null,
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Location permission needed",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = "Required to show your position and download the area around you.",
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onRetry,
                shape   = RoundedCornerShape(10.dp)
            ) {
                Text("Allow", color = Color.White)
            }
        }
    }
}
