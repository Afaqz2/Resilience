package com.resilience.app.data.repository

import com.resilience.app.data.db.dao.OfflineRegionDao
import com.resilience.app.data.db.entity.OfflineRegionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for offline map region metadata.
 *
 * Room stores the metadata (name, bbox, timestamps) while MapLibre's
 * OfflineManager holds the actual tile files.  Both are always kept
 * in sync via [OfflineMapManager].
 */
@Singleton
class OfflineMapRepository @Inject constructor(
    private val dao: OfflineRegionDao
) {
    fun getAllRegions(): Flow<List<OfflineRegionEntity>> = dao.getAllRegions()

    suspend fun getRegionById(id: Long): OfflineRegionEntity? = dao.getRegionById(id)

    suspend fun upsert(region: OfflineRegionEntity) = dao.upsert(region)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun getRegionsOlderThan(cutoffMillis: Long): List<OfflineRegionEntity> =
        dao.getRegionsOlderThan(cutoffMillis)
}
