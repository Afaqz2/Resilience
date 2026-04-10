package com.resilience.app.data.db.dao

import androidx.room.*
import com.resilience.app.data.db.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM cached_alerts ORDER BY fetchedAt DESC")
    fun observeAll(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<AlertEntity>)

    @Query("DELETE FROM cached_alerts")
    suspend fun clearAll()
}
