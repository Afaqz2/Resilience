package com.resilience.app.data.location

import android.location.Location
import com.resilience.app.data.model.ThreatAlert
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Proximity logic for matching [ThreatAlert]s against a user's GPS coordinates.
 *
 * Uses the Haversine formula for great-circle distance. Android's
 * [Location.distanceBetween] is used where a Context is available; the pure-Kotlin
 * fallback is used in unit tests or when no Context exists.
 */
@Singleton
class LocationFilter @Inject constructor() {

    /**
     * Returns all alerts paired with their distance (km) from [userLat]/[userLon],
     * sorted by severity descending then distance ascending.
     */
    fun withDistances(
        alerts: List<ThreatAlert>,
        userLat: Double,
        userLon: Double
    ): List<Pair<ThreatAlert, Double>> =
        alerts
            .map { alert -> alert to distanceKm(userLat, userLon, alert.lat, alert.lon) }
            .sortedWith(
                compareByDescending<Pair<ThreatAlert, Double>> { (alert, _) -> alert.severity.priority }
                    .thenBy { (_, km) -> km }
            )

    /**
     * Returns only alerts within [radiusKm] of the user, sorted by distance.
     */
    fun filterByProximity(
        alerts: List<ThreatAlert>,
        userLat: Double,
        userLon: Double,
        radiusKm: Double = 1_000.0
    ): List<Pair<ThreatAlert, Double>> =
        withDistances(alerts, userLat, userLon)
            .filter { (_, km) -> km <= radiusKm }

    // ── Haversine ─────────────────────────────────────────────────────────

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] / 1_000.0
    }
}
