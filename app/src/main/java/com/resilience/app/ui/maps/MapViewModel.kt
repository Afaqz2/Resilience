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

// ── POI legend data ────────────────────────────────────────────────────────

data class PoiEntry(val name: String, val type: String, val city: String)

/** All bundled POIs — mirrors poi_overlay.geojson exactly. */
val BUNDLED_POIS = listOf(
    // Karachi
    PoiEntry("Jinnah Postgraduate Medical Centre", "hospital", "Karachi"),
    PoiEntry("Aga Khan University Hospital",       "hospital", "Karachi"),
    PoiEntry("Civil Hospital Karachi",             "hospital", "Karachi"),
    PoiEntry("Karachi Water Board Supply Point",   "water",    "Karachi"),
    PoiEntry("Expo Centre Emergency Shelter",      "shelter",  "Karachi"),
    PoiEntry("Al-Habib Medical Store (Saddar)",    "shop",     "Karachi"),
    PoiEntry("D-Watson Pharmacy (Gulshan)",        "shop",     "Karachi"),
    PoiEntry("Imtiaz Super Store (DHA)",           "shop",     "Karachi"),
    // Lahore
    PoiEntry("Services Hospital Lahore",           "hospital", "Lahore"),
    PoiEntry("Mayo Hospital Lahore",               "hospital", "Lahore"),
    PoiEntry("Lahore WASA Water Point",            "water",    "Lahore"),
    PoiEntry("Lahore Sports Complex Shelter",      "shelter",  "Lahore"),
    PoiEntry("Fazal Din Pharmacy (Mall Road)",     "shop",     "Lahore"),
    PoiEntry("Metro Cash & Carry (Gulberg)",       "shop",     "Lahore"),
    PoiEntry("Al-Fatah General Store (Defence)",   "shop",     "Lahore"),
    // Islamabad
    PoiEntry("PIMS Hospital Islamabad",            "hospital", "Islamabad"),
    PoiEntry("Shifa International Hospital",       "hospital", "Islamabad"),
    PoiEntry("CDA Water Supply Point",             "water",    "Islamabad"),
    PoiEntry("Jinnah Convention Centre Shelter",   "shelter",  "Islamabad"),
    PoiEntry("Shaheen Chemist (Blue Area)",        "shop",     "Islamabad"),
    PoiEntry("Carrefour (F-10 Markaz)",            "shop",     "Islamabad"),
    PoiEntry("Sunday Bazaar Emergency Supplies",   "shop",     "Islamabad"),
)

// ── UI state ───────────────────────────────────────────────────────────────

data class MapUiState(
    val downloadedRegions: List<OfflineRegionEntity> = emptyList(),
    val downloadProgress: Float? = null,
    val downloadStatusText: String = "",
    val isDownloading: Boolean = false,
    val showDownloadSheet: Boolean = false,
    val selectedPreset: CityPreset? = null,
    val snackbarMessage: String? = null,
    val currentLocation: Pair<Double, Double>? = null,
    val hasLocationPermission: Boolean = false,
    /** Incremented each time the user taps "centre on me" */
    val recenterTrigger: Long = 0L,
    val showPoiLegend: Boolean = false,
    val showOfflinePacks: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────

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
        val hasLocationPermission: Boolean = false,
        val recenterTrigger: Long = 0L,
        val showPoiLegend: Boolean = false,
        val showOfflinePacks: Boolean = false
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
            hasLocationPermission = mut.hasLocationPermission,
            recenterTrigger      = mut.recenterTrigger,
            showPoiLegend        = mut.showPoiLegend,
            showOfflinePacks     = mut.showOfflinePacks
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState()
    )

    val cityPresets: List<CityPreset> = OfflineMapManager.CITY_PRESETS

    // ── Sheet toggles ──────────────────────────────────────────────────────

    fun showDownloadSheet()  = _mutableState.update { it.copy(showDownloadSheet = true) }
    fun hideDownloadSheet()  = _mutableState.update { it.copy(showDownloadSheet = false) }
    fun selectPreset(p: CityPreset) = _mutableState.update { it.copy(selectedPreset = p) }

    fun showPoiLegend()      = _mutableState.update { it.copy(showPoiLegend = true) }
    fun hidePoiLegend()      = _mutableState.update { it.copy(showPoiLegend = false) }

    fun showOfflinePacks()   = _mutableState.update { it.copy(showOfflinePacks = true) }
    fun hideOfflinePacks()   = _mutableState.update { it.copy(showOfflinePacks = false) }

    // ── Map re-centre ──────────────────────────────────────────────────────

    fun triggerRecenter() = _mutableState.update { it.copy(recenterTrigger = it.recenterTrigger + 1) }

    // ── Location permission + fetch ────────────────────────────────────────

    fun onLocationPermissionGranted(context: Context) {
        _mutableState.update { it.copy(hasLocationPermission = true) }
        fetchCurrentLocation(context)
    }

    fun onLocationPermissionDenied() =
        _mutableState.update { it.copy(hasLocationPermission = false) }

    fun fetchCurrentLocation(context: Context) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val cached = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstNotNullOfOrNull { provider ->
                try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }
        if (cached != null) {
            _mutableState.update { it.copy(currentLocation = cached.latitude to cached.longitude) }
            return
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                _mutableState.update { it.copy(currentLocation = location.latitude to location.longitude) }
                try { lm.removeUpdates(this) } catch (_: SecurityException) {}
            }
        }
        try { lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener) }
        catch (_: SecurityException) {}
    }

    // ── Download ───────────────────────────────────────────────────────────

    fun downloadRegion(preset: CityPreset) {
        if (_mutableState.value.isDownloading) return
        _mutableState.update {
            it.copy(isDownloading = true, downloadProgress = 0f, downloadStatusText = "Starting…", showDownloadSheet = false)
        }
        offlineMapManager.downloadRegion(
            preset = preset,
            regionName = preset.name,
            onProgress = { progress, msg, _ ->
                _mutableState.update { it.copy(downloadProgress = progress, downloadStatusText = msg) }
            },
            onComplete = { regionId, sizeBytes ->
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
                            status       = "COMPLETE",
                            sizeBytes    = sizeBytes
                        )
                    )
                    _mutableState.update {
                        it.copy(
                            isDownloading      = false,
                            downloadProgress   = null,
                            downloadStatusText = "",
                            snackbarMessage    = "${preset.name} downloaded — tap ✓ to view offline packs"
                        )
                    }
                }
            },
            onError = { error ->
                viewModelScope.launch {
                    _mutableState.update {
                        it.copy(isDownloading = false, downloadProgress = null, downloadStatusText = "", snackbarMessage = "Download failed: $error")
                    }
                }
            }
        )
    }

    fun downloadAroundLocation() {
        val location = _mutableState.value.currentLocation ?: return
        val (lat, lon) = location
        val deltaLat = 0.09
        val deltaLon = 0.09 / cos(Math.toRadians(lat))
        downloadRegion(
            CityPreset(
                name    = "My Area (%.3f, %.3f)".format(lat, lon),
                minLat  = lat - deltaLat,
                minLon  = lon - deltaLon,
                maxLat  = lat + deltaLat,
                maxLon  = lon + deltaLon,
                zoomMin = 12.0,
                zoomMax = 15.0
            )
        )
    }

    // ── Delete / Pause / Resume ────────────────────────────────────────────

    fun deleteRegion(region: OfflineRegionEntity) {
        viewModelScope.launch {
            if (offlineMapManager.deleteRegion(region.id)) {
                repository.deleteById(region.id)
                _mutableState.update { it.copy(snackbarMessage = "${region.name} removed") }
            } else {
                _mutableState.update { it.copy(snackbarMessage = "Could not remove ${region.name}") }
            }
        }
    }

    fun pauseRegion(regionId: Long) = viewModelScope.launch {
        offlineMapManager.pauseRegion(regionId)
        repository.getRegionById(regionId)?.let { repository.upsert(it.copy(status = "PAUSED")) }
    }

    fun resumeRegion(regionId: Long) = viewModelScope.launch {
        offlineMapManager.resumeRegion(regionId)
        repository.getRegionById(regionId)?.let { repository.upsert(it.copy(status = "DOWNLOADING")) }
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
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cb.setPrimaryClip(ClipData.newPlainText("My Location", text))
        _mutableState.update { it.copy(snackbarMessage = "Location copied — ready to paste into SMS") }
    }

    fun clearSnackbar() = _mutableState.update { it.copy(snackbarMessage = null) }
}
