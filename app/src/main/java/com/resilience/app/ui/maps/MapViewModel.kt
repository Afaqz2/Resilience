package com.resilience.app.ui.maps

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resilience.app.data.db.entity.OfflineRegionEntity
import com.resilience.app.data.repository.OfflineMapRepository
import com.resilience.app.maps.CityPreset
import com.resilience.app.maps.OfflineMapManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.cos

data class MapUiState(
    val downloadedRegions: List<OfflineRegionEntity> = emptyList(),
    /** null = not downloading; 0f–1f = in progress */
    val downloadProgress: Float? = null,
    val downloadStatusText: String = "",
    val isDownloading: Boolean = false,
    val showDownloadSheet: Boolean = false,
    val selectedPreset: CityPreset? = null,
    val snackbarMessage: String? = null,
    /** Current GPS fix — null until permission granted and location resolved */
    val currentLocation: Pair<Double, Double>? = null,
    val hasLocationPermission: Boolean = false
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: OfflineMapRepository,
    private val offlineMapManager: OfflineMapManager
) : ViewModel() {

    private data class MutableMapState(
        val downloadProgress: Float? = null,
        val downloadStatusText: String = "",
        val isDownloading: Boolean = false,
        val showDownloadSheet: Boolean = false,
        val selectedPreset: CityPreset? = null,
        val snackbarMessage: String? = null,
        val currentLocation: Pair<Double, Double>? = null,
        val hasLocationPermission: Boolean = false
    )

    private val _mutableState = MutableStateFlow(MutableMapState())

    val uiState: StateFlow<MapUiState> = combine(
        repository.getAllRegions(),
        _mutableState
    ) { regions, mut ->
        MapUiState(
            downloadedRegions    = regions,
            downloadProgress     = mut.downloadProgress,
            downloadStatusText   = mut.downloadStatusText,
            isDownloading        = mut.isDownloading,
            showDownloadSheet    = mut.showDownloadSheet,
            selectedPreset       = mut.selectedPreset,
            snackbarMessage      = mut.snackbarMessage,
            currentLocation      = mut.currentLocation,
            hasLocationPermission = mut.hasLocationPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState()
    )

    val cityPresets: List<CityPreset> = OfflineMapManager.CITY_PRESETS

    // ── Sheet visibility ───────────────────────────────────────────────────

    fun showDownloadSheet() = _mutableState.update { it.copy(showDownloadSheet = true) }
    fun hideDownloadSheet() = _mutableState.update { it.copy(showDownloadSheet = false) }

    fun selectPreset(preset: CityPreset) =
        _mutableState.update { it.copy(selectedPreset = preset) }

    // ── Location permission + fetch ────────────────────────────────────────

    /** Called by the screen once the user grants (or has already granted) location permission. */
    fun onLocationPermissionGranted(context: Context) {
        _mutableState.update { it.copy(hasLocationPermission = true) }
        fetchCurrentLocation(context)
    }

    fun onLocationPermissionDenied() {
        _mutableState.update { it.copy(hasLocationPermission = false) }
    }

    /**
     * Tries [LocationManager.getLastKnownLocation] first (instant).
     * Falls back to a single [LocationManager.requestLocationUpdates] if no cached fix.
     */
    fun fetchCurrentLocation(context: Context) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Check permission again defensively
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) return

        // Try cached fix from any available provider
        val cached = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstNotNullOfOrNull { provider ->
                try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }

        if (cached != null) {
            _mutableState.update { it.copy(currentLocation = cached.latitude to cached.longitude) }
            return
        }

        // No cached fix — request one fresh update (network is faster for first fix)
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                _mutableState.update { it.copy(currentLocation = location.latitude to location.longitude) }
                try { lm.removeUpdates(this) } catch (_: SecurityException) {}
            }
        }
        try {
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener)
        } catch (_: SecurityException) {}
    }

    // ── Download ───────────────────────────────────────────────────────────

    fun downloadRegion(preset: CityPreset) {
        if (_mutableState.value.isDownloading) return

        _mutableState.update {
            it.copy(
                isDownloading      = true,
                downloadProgress   = 0f,
                downloadStatusText = "Starting…",
                showDownloadSheet  = false
            )
        }

        offlineMapManager.downloadRegion(
            preset = preset,
            regionName = preset.name,
            onProgress = { progress, msg ->
                _mutableState.update { it.copy(downloadProgress = progress, downloadStatusText = msg) }
            },
            onComplete = { regionId ->
                viewModelScope.launch {
                    repository.upsert(
                        OfflineRegionEntity(
                            id           = regionId,
                            name         = preset.name,
                            minLat       = preset.minLat,
                            minLon       = preset.minLon,
                            maxLat       = preset.maxLat,
                            maxLon       = preset.maxLon,
                            styleUrl     = OfflineMapManager.DEMO_STYLE_URL,
                            minZoom      = preset.zoomMin,
                            maxZoom      = preset.zoomMax,
                            downloadedAt = System.currentTimeMillis(),
                            status       = "COMPLETE"
                        )
                    )
                    _mutableState.update {
                        it.copy(
                            isDownloading      = false,
                            downloadProgress   = null,
                            downloadStatusText = "",
                            snackbarMessage    = "${preset.name} downloaded successfully"
                        )
                    }
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    _mutableState.update {
                        it.copy(
                            isDownloading      = false,
                            downloadProgress   = null,
                            downloadStatusText = "",
                            snackbarMessage    = "Download failed: $error"
                        )
                    }
                }
            }
        )
    }

    /**
     * Downloads a ~20 km × 20 km tile pack centred on the user's current GPS fix.
     * Zooms 12–15 give good street-level detail at roughly 50–80 MB.
     */
    fun downloadAroundLocation() {
        val location = _mutableState.value.currentLocation ?: return
        val (lat, lon) = location

        val deltaLat = 0.09                                      // ~10 km north/south
        val deltaLon = 0.09 / cos(Math.toRadians(lat))          // ~10 km east/west

        val preset = CityPreset(
            name    = "My Area (%.3f, %.3f)".format(lat, lon),
            minLat  = lat - deltaLat,
            minLon  = lon - deltaLon,
            maxLat  = lat + deltaLat,
            maxLon  = lon + deltaLon,
            zoomMin = 12.0,
            zoomMax = 15.0
        )
        downloadRegion(preset)
    }

    // ── Delete / Pause / Resume ────────────────────────────────────────────

    fun deleteRegion(region: OfflineRegionEntity) {
        viewModelScope.launch {
            val deleted = offlineMapManager.deleteRegion(region.id)
            if (deleted) {
                repository.deleteById(region.id)
                _mutableState.update { it.copy(snackbarMessage = "${region.name} removed") }
            } else {
                _mutableState.update { it.copy(snackbarMessage = "Could not remove ${region.name}") }
            }
        }
    }

    fun pauseRegion(regionId: Long) = viewModelScope.launch {
        offlineMapManager.pauseRegion(regionId)
        repository.getRegionById(regionId)?.let { entity ->
            repository.upsert(entity.copy(status = "PAUSED"))
        }
    }

    fun resumeRegion(regionId: Long) = viewModelScope.launch {
        offlineMapManager.resumeRegion(regionId)
        repository.getRegionById(regionId)?.let { entity ->
            repository.upsert(entity.copy(status = "DOWNLOADING"))
        }
    }

    // ── Share Location ─────────────────────────────────────────────────────

    fun shareLocation(context: Context) {
        val location = _mutableState.value.currentLocation
        if (location == null) {
            _mutableState.update { it.copy(snackbarMessage = "Location unavailable — ensure GPS is on") }
            return
        }
        val (lat, lon) = location
        val text = "My location: $lat, $lon\nhttps://maps.google.com/?q=$lat,$lon"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("My Location", text))
        _mutableState.update { it.copy(snackbarMessage = "Location copied — ready to paste into SMS") }
    }

    fun clearSnackbar() = _mutableState.update { it.copy(snackbarMessage = null) }
}
