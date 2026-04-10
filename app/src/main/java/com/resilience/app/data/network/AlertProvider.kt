package com.resilience.app.data.network

import com.resilience.app.data.model.*
import com.resilience.app.data.network.api.GdacsApiService
import com.resilience.app.data.network.api.UsgsApiService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates threat alerts from multiple online sources (GDACS, USGS) and
 * normalises them into the common [ThreatAlert] domain model.
 */
@Singleton
class AlertProvider @Inject constructor(
    private val gdacsApiService: GdacsApiService,
    private val usgsApiService: UsgsApiService
) {
    suspend fun fetchAllAlerts(): List<ThreatAlert> {
        val alerts = mutableListOf<ThreatAlert>()

        // ── GDACS: natural disasters (earthquakes, floods, cyclones, etc.) ──
        try {
            val response = gdacsApiService.getAlerts()
            response.features?.forEach { feature ->
                val props = feature.properties
                val coords = feature.geometry?.coordinates
                if (coords != null && coords.size >= 2) {
                    alerts += ThreatAlert(
                        id = "gdacs_${props.eventId}_${props.episodeId}",
                        title = props.eventName ?: "Unknown Event",
                        description = buildString {
                            if (!props.country.isNullOrBlank()) append(props.country)
                            if (!props.description.isNullOrBlank()) {
                                if (isNotEmpty()) append(" — ")
                                append(props.description)
                            }
                        },
                        severity = when (props.alertLevel?.lowercase()) {
                            "red"    -> AlertSeverity.EXTREME
                            "orange" -> AlertSeverity.SEVERE
                            "green"  -> AlertSeverity.MINOR
                            else     -> AlertSeverity.UNKNOWN
                        },
                        source = AlertSource.GDACS,
                        type = when (props.eventType?.uppercase()) {
                            "EQ" -> AlertType.EARTHQUAKE
                            "FL" -> AlertType.FLOOD
                            "TC" -> AlertType.CYCLONE
                            "VO" -> AlertType.VOLCANO
                            "WF" -> AlertType.WILDFIRE
                            else -> AlertType.OTHER
                        },
                        lat = coords[1],
                        lon = coords[0]
                    )
                }
            }
        } catch (_: Exception) {
            // GDACS unavailable — continue with other sources
        }

        // ── USGS: significant earthquakes (past 7 days) ──────────────────
        try {
            val response = usgsApiService.getSignificantEarthquakes()
            response.features.forEach { feature ->
                val props = feature.properties
                val coords = feature.geometry.coordinates
                if (coords.size >= 2) {
                    alerts += ThreatAlert(
                        id = "usgs_${feature.id}",
                        title = props.title ?: "M${props.mag} Earthquake",
                        description = props.place ?: "",
                        severity = when (props.alert?.lowercase()) {
                            "red"    -> AlertSeverity.EXTREME
                            "orange" -> AlertSeverity.SEVERE
                            "yellow" -> AlertSeverity.MODERATE
                            "green"  -> AlertSeverity.MINOR
                            else     -> when {
                                (props.sig ?: 0) >= 700 -> AlertSeverity.EXTREME
                                (props.sig ?: 0) >= 500 -> AlertSeverity.SEVERE
                                (props.sig ?: 0) >= 300 -> AlertSeverity.MODERATE
                                else                    -> AlertSeverity.MINOR
                            }
                        },
                        source = AlertSource.USGS,
                        type = AlertType.EARTHQUAKE,
                        lat = coords[1],
                        lon = coords[0]
                    )
                }
            }
        } catch (_: Exception) {
            // USGS unavailable
        }

        return alerts.sortedByDescending { it.severity.priority }
    }
}
