package com.resilience.app.data.db.dao

import androidx.room.*
import com.resilience.app.data.db.entity.PlaybookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybookDao {

    /** Observe all playbooks — emits when the DB changes */
    @Query("SELECT * FROM playbooks ORDER BY category, title")
    fun getAllPlaybooks(): Flow<List<PlaybookEntity>>

    /** Filter by category — e.g. "First Aid" */
    @Query("SELECT * FROM playbooks WHERE category = :category ORDER BY title")
    fun getPlaybooksByCategory(category: String): Flow<List<PlaybookEntity>>

    /** Fetch a single playbook for the detail screen */
    @Query("SELECT * FROM playbooks WHERE id = :id")
    suspend fun getPlaybookById(id: String): PlaybookEntity?

    /** Return all distinct categories for the filter chips */
    @Query("SELECT DISTINCT category FROM playbooks ORDER BY category")
    fun getAllCategories(): Flow<List<String>>

    /** Upsert — used during asset seeding on first launch */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(playbooks: List<PlaybookEntity>)
}
