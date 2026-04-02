package com.resilience.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class CrisisModeDataStore(private val context: Context) {

    private val CRISIS_MODE_KEY = booleanPreferencesKey("crisis_mode")
    private val EXTREME_BATTERY_MODE_KEY = booleanPreferencesKey("extreme_battery_mode")

    val isCrisisMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CRISIS_MODE_KEY] ?: false
        }

    val isExtremeBatteryMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[EXTREME_BATTERY_MODE_KEY] ?: false
        }

    suspend fun toggleCrisisMode(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CRISIS_MODE_KEY] = isEnabled
        }
    }

    suspend fun toggleExtremeBatteryMode(isEnabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXTREME_BATTERY_MODE_KEY] = isEnabled
        }
    }
}
