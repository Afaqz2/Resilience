package com.resilience.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.resilience.app.data.db.dao.PlaybookDao
import com.resilience.app.data.db.entity.PlaybookEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for Survival Playbooks.
 *
 * On first use the repository seeds the Room DB from the bundled
 * `assets/playbooks.json` file.  All subsequent reads come from Room
 * so the feature is fully offline.
 */
@Singleton
class PlaybookRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PlaybookDao
) {

    /** Observe every playbook — updates reactively on DB change */
    fun getAllPlaybooks(): Flow<List<PlaybookEntity>> = dao.getAllPlaybooks()

    /** Observe playbooks filtered by category */
    fun getPlaybooksByCategory(category: String): Flow<List<PlaybookEntity>> =
        dao.getPlaybooksByCategory(category)

    /** Observe all distinct category names for the filter chips */
    fun getAllCategories(): Flow<List<String>> = dao.getAllCategories()

    /** Fetch one playbook by ID (for the detail screen) */
    suspend fun getPlaybookById(id: String): PlaybookEntity? = dao.getPlaybookById(id)

    /**
     * Seeds the database from `assets/playbooks.json`.
     *
     * Safe to call every launch — uses REPLACE strategy so content
     * updates are picked up automatically after asset updates.
     */
    suspend fun seedFromAssets() {
        val json = context.assets.open("playbooks.json")
            .bufferedReader()
            .use { it.readText() }

        val type = object : TypeToken<List<PlaybookEntity>>() {}.type
        val playbooks: List<PlaybookEntity> = Gson().fromJson(json, type)
        dao.upsertAll(playbooks)
    }
}
