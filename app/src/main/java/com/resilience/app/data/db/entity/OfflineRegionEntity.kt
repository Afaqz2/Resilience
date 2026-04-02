package com.resilience.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks downloaded MapLibre offline tile packs in Room.
 *
 * [id] mirrors the region ID assigned by MapLibre's OfflineManager
 * so we can cross-reference Room metadata with the actual tile store
 * without scanning all regions on every access.
 */
@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val minLat: Double,
    val minLon: Double,
    val maxLat: Double,
    val maxLon: Double,
    val styleUrl: String,
    val minZoom: Double,
    val maxZoom: Double,
    /** Epoch milliseconds — used by MapTileCleanupWorker to prune stale packs */
    val downloadedAt: Long,
    /** One of: DOWNLOADING | COMPLETE | PAUSED | ERROR */
    val status: String = "COMPLETE"
)
