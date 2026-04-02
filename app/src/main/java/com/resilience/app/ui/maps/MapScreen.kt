package com.resilience.app.ui.maps

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.resilience.app.maps.OfflineMapManager
import com.resilience.app.ui.theme.SafeReachDarkGray

/**
 * Main Maps screen — shows a MapLibre map with a bundled POI overlay,
 * a "Share my location" FAB, and a download sheet trigger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                    IconButton(onClick = { viewModel.showDownloadSheet() }) {
                        Icon(Icons.Default.Download, contentDescription = "Download city map")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.shareLocation(context) },
                containerColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Share my location")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MapLibreComposable(modifier = Modifier.fillMaxSize())

            // Download progress card — shown while a pack is downloading
            if (uiState.isDownloading) {
                DownloadProgressCard(
                    progress = uiState.downloadProgress ?: 0f,
                    statusText = uiState.downloadStatusText,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .fillMaxWidth()
                )
            }

            // Badge showing how many packs are stored
            if (!uiState.isDownloading && uiState.downloadedRegions.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = SafeReachDarkGray
                ) {
                    Text(
                        text = "${uiState.downloadedRegions.size} offline pack${if (uiState.downloadedRegions.size > 1) "s" else ""}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (uiState.showDownloadSheet) {
        OfflineMapDownloadSheet(
            viewModel = viewModel,
            uiState = uiState,
            onDismiss = { viewModel.hideDownloadSheet() }
        )
    }
}

/**
 * Wraps [MapView] in a Compose [AndroidView] with full lifecycle forwarding.
 *
 * Loads the MapLibre demo style and adds a bundled POI overlay
 * (hospitals, water sources, shelters) from `assets/poi_overlay.geojson`.
 */
@Composable
fun MapLibreComposable(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val mapView = remember {
        // Mapbox.getInstance is a no-op if already called; safe to repeat.
        Mapbox.getInstance(context)
        MapView(context)
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
                    map.setStyle(OfflineMapManager.DEMO_STYLE_URL) { style ->
                        // Load bundled POI overlay (best-effort — silent fail if asset missing)
                        try {
                            val geojson = context.assets
                                .open("poi_overlay.geojson")
                                .bufferedReader()
                                .readText()

                            style.addSource(GeoJsonSource("poi-source", geojson))

                            // Hospitals — red circles
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
                            // Water sources — blue circles
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
                            // Shelters — orange circles
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
                        } catch (_: Exception) {
                            // POI overlay is optional — map still works without it
                        }
                    }
                }
            }
        },
        modifier = modifier
    )
}

/** Helper for the layer filter DSL — mirrors Mapbox expression syntax. */
private fun eq(key: String, value: String) =
    com.mapbox.mapboxsdk.style.expressions.Expression.eq(
        com.mapbox.mapboxsdk.style.expressions.Expression.get(key),
        com.mapbox.mapboxsdk.style.expressions.Expression.literal(value)
    )

// ── Download progress card ─────────────────────────────────────────────────

@Composable
fun DownloadProgressCard(
    progress: Float,
    statusText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SafeReachDarkGray),
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
                    text = "Downloading map tiles…",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF81C784),
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = statusText,
                    color = Color.White.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
