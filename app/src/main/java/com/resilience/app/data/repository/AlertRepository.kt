package com.resilience.app.data.repository

import com.resilience.app.data.db.dao.AlertDao
import com.resilience.app.data.db.entity.AlertEntity
import com.resilience.app.data.location.LocationFilter
import com.resilience.app.data.model.*
import com.resilience.app.data.network.AlertProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val alertProvider: AlertProvider,
    private val alertDao: AlertDao,
    private val locationFilter: LocationFilter
) {
    /** Observe cached alerts as domain objects, sorted by severity then fetch time. */
    fun observeCachedAlerts(): Flow<List<ThreatAlert>> =
        alertDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
                .sortedByDescending { it.severity.priority }
        }

    /** Observe cached alerts with distances from the given user coordinates. */
    fun observeAlertsWithDistance(
        userLat: Double,
        userLon: Double
    ): Flow<List<Pair<ThreatAlert, Double>>> =
        observeCachedAlerts().map { alerts ->
            locationFilter.withDistances(alerts, userLat, userLon)
        }

    /**
     * Fetches fresh alerts from all online sources, replaces the cache,
     * and returns the new list.
     */
    suspend fun refreshAlerts(): Result<List<ThreatAlert>> = runCatching {
        val fresh = alertProvider.fetchAllAlerts()
        alertDao.clearAll()
        alertDao.insertAll(fresh.map { it.toEntity() })
        fresh
    }

    // ── Mapping helpers ───────────────────────────────────────────────────

    private fun AlertEntity.toDomain() = ThreatAlert(
        id = id,
        title = title,
        description = description,
        severity = AlertSeverity.valueOf(severity),
        source = AlertSource.valueOf(source),
        type = AlertType.valueOf(type),
        lat = lat,
        lon = lon,
        fetchedAt = fetchedAt
    )

    private fun ThreatAlert.toEntity() = AlertEntity(
        id = id,
        title = title,
        description = description,
        severity = severity.name,
        source = source.name,
        type = type.name,
        lat = lat,
        lon = lon,
        fetchedAt = fetchedAt
    )
}
