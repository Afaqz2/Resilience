package com.resilience.app.ui.maps

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
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

data class MapUiState(
    val downloadedRegions: List<OfflineRegionEntity> = emptyList(),
    /** null = not downloading; 0f–1f = in progress */
    val downloadProgress: Float? = null,
    val downloadStatusText: String = "",
    val isDownloading: Boolean = false,
    val showDownloadSheet: Boolean = false,
    val selectedPreset: CityPreset? = null,
    val snackbarMessage: String? = null
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
        val snackbarMessage: String? = null
    )

    private val _mutableState = MutableStateFlow(MutableMapState())

    val uiState: StateFlow<MapUiState> = combine(
        repository.getAllRegions(),
        _mutableState
    ) { regions, mut ->
        MapUiState(
            downloadedRegions  = regions,
            downloadProgress   = mut.downloadProgress,
            downloadStatusText = mut.downloadStatusText,
            isDownloading      = mut.isDownloading,
            showDownloadSheet  = mut.showDownloadSheet,
            selectedPreset     = mut.selectedPreset,
            snackbarMessage    = mut.snackbarMessage
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

    // ── Download ───────────────────────────────────────────────────────────

    fun downloadRegion(preset: CityPreset) {
        if (_mutableState.value.isDownloading) return

        _mutableState.update {
            it.copy(isDownloading = true, downloadProgress = 0f, downloadStatusText = "Starting…", showDownloadSheet = false)
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

    /**
     * Reads the last known GPS fix and copies "lat, lon + Google Maps link"
     * to the clipboard so it can be pasted into SMS / WhatsApp with no internet.
     */
    fun shareLocation(context: Context) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _mutableState.update { it.copy(snackbarMessage = "Location permission required") }
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstNotNullOfOrNull { provider ->
                try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }

        if (location != null) {
            val text = buildString {
                append("My location: ${location.latitude}, ${location.longitude}\n")
                append("https://maps.google.com/?q=${location.latitude},${location.longitude}")
            }
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("My Location", text))
            _mutableState.update { it.copy(snackbarMessage = "Location copied — ready to paste into SMS") }
        } else {
            _mutableState.update { it.copy(snackbarMessage = "Location unavailable — ensure GPS is on") }
        }
    }

    fun clearSnackbar() = _mutableState.update { it.copy(snackbarMessage = null) }
}
