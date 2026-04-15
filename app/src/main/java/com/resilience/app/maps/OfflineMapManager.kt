package com.resilience.app.maps

import android.content.Context
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Preset bounding box for a city that can be downloaded as an offline tile pack.
 *
 * [zoomMin]/[zoomMax] control the tile resolution stored.
 * Zoom 10–14 covers city-level navigation at ~30–50 MB per city.
 */
data class CityPreset(
    val name: String,
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val zoomMin: Double = 10.0,
    val zoomMax: Double = 14.0
)

/**
 * Wraps MapLibre's [OfflineManager] for downloading, listing, pausing,
 * resuming, and deleting offline tile packs.
 */
@Singleton
class OfflineMapManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /**
         * CartoDB Voyager GL style — full road network, labels, buildings.
         * Free & public, no API key required.
         */
        const val DEMO_STYLE_URL = "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"

        /** Preset city bounding boxes for the city selector. */
        val CITY_PRESETS = listOf(
            CityPreset("Karachi",    24.74, 66.85, 25.10, 67.28),
            CityPreset("Lahore",     31.35, 74.15, 31.70, 74.55),
            CityPreset("Islamabad",  33.55, 72.95, 33.80, 73.25),
            CityPreset("Rawalpindi", 33.50, 73.00, 33.70, 73.20),
            CityPreset("Peshawar",   33.95, 71.40, 34.10, 71.65),
            CityPreset("Quetta",     30.15, 66.90, 30.30, 67.10),
            CityPreset("Multan",     30.14, 71.38, 30.30, 71.55),
            CityPreset("Faisalabad", 31.35, 72.95, 31.55, 73.20)
        )
    }

    private val offlineManager: OfflineManager by lazy {
        // Initialize MapLibre SDK (no API key needed for open tile servers).
        MapLibre.getInstance(context)
        OfflineManager.getInstance(context)
    }

    /**
     * Starts downloading tiles for [preset] and calls back with progress.
     *
     * [onProgress] is invoked on each tile batch: progress ∈ [0, 1], statusText for display.
     * [onComplete] is invoked with the MapLibre region ID when the pack is fully downloaded.
     * [onError] is invoked with a human-readable error description.
     */
    fun downloadRegion(
        preset: CityPreset,
        regionName: String,
        onProgress: (progress: Float, statusText: String, sizeBytes: Long) -> Unit,
        onComplete: (regionId: Long, sizeBytes: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        val bounds = LatLngBounds.Builder()
            .include(LatLng(preset.maxLat, preset.maxLon))
            .include(LatLng(preset.minLat, preset.minLon))
            .build()

        val definition = OfflineTilePyramidRegionDefinition(
            DEMO_STYLE_URL,
            bounds,
            preset.zoomMin,
            preset.zoomMax,
            context.resources.displayMetrics.density
        )

        val metadata = JSONObject().apply {
            put("name", regionName)
        }.toString().toByteArray(Charsets.UTF_8)

        offlineManager.createOfflineRegion(
            definition,
            metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            val required  = status.requiredResourceCount
                            val completed = status.completedResourceCount
                            val sizeBytes = status.completedResourceSize
                            val sizeMb    = sizeBytes / 1_048_576f
                            val progress  = if (required > 0) completed.toFloat() / required else 0f
                            val text      = "$completed / $required tiles · ${"%.1f".format(sizeMb)} MB"
                            onProgress(progress, text, sizeBytes)
                            if (status.isComplete) onComplete(offlineRegion.id, sizeBytes)
                        }

                        override fun onError(error: OfflineRegionError) {
                            onError(error.reason ?: "Unknown download error")
                        }

                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            onError("Tile count limit exceeded ($limit tiles)")
                        }
                    })
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                }

                override fun onError(error: String) = onError(error)
            }
        )
    }

    /** Returns all offline regions currently stored by MapLibre. */
    suspend fun listRegions(): List<OfflineRegion> = suspendCancellableCoroutine { cont ->
        offlineManager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
            override fun onList(offlineRegions: Array<OfflineRegion>?) {
                cont.resume(offlineRegions?.toList() ?: emptyList())
            }
            override fun onError(error: String) { cont.resume(emptyList()) }
        })
    }

    /** Deletes the MapLibre tile pack for [regionId]. Returns true on success. */
    suspend fun deleteRegion(regionId: Long): Boolean {
        val region = listRegions().find { it.id == regionId } ?: return false
        return suspendCancellableCoroutine { cont ->
            region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() = cont.resume(true)
                override fun onError(error: String) = cont.resume(false)
            })
        }
    }

    /** Pauses an in-progress download. */
    suspend fun pauseRegion(regionId: Long) {
        listRegions().find { it.id == regionId }
            ?.setDownloadState(OfflineRegion.STATE_INACTIVE)
    }

    /** Resumes a paused download. */
    suspend fun resumeRegion(regionId: Long) {
        listRegions().find { it.id == regionId }
            ?.setDownloadState(OfflineRegion.STATE_ACTIVE)
    }
}
