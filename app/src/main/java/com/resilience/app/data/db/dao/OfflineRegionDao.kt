package com.resilience.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.resilience.app.data.db.entity.OfflineRegionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineRegionDao {

    @Query("SELECT * FROM offline_regions ORDER BY name")
    fun getAllRegions(): Flow<List<OfflineRegionEntity>>

    @Query("SELECT * FROM offline_regions WHERE id = :id")
    suspend fun getRegionById(id: Long): OfflineRegionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(region: OfflineRegionEntity)

    @Query("DELETE FROM offline_regions WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Returns regions whose [OfflineRegionEntity.downloadedAt] is older than [cutoffMillis]. */
    @Query("SELECT * FROM offline_regions WHERE downloadedAt < :cutoffMillis")
    suspend fun getRegionsOlderThan(cutoffMillis: Long): List<OfflineRegionEntity>
}
