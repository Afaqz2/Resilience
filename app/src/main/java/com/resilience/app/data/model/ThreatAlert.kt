package com.resilience.app.data.model

data class ThreatAlert(
    val id: String,
    val title: String,
    val description: String,
    val severity: AlertSeverity,
    val source: AlertSource,
    val type: AlertType,
    val lat: Double,
    val lon: Double,
    val fetchedAt: Long = System.currentTimeMillis()
)

enum class AlertSeverity(val label: String, val priority: Int) {
    EXTREME("EXTREME", 4),
    SEVERE("SEVERE", 3),
    MODERATE("MODERATE", 2),
    MINOR("MINOR", 1),
    UNKNOWN("UNKNOWN", 0)
}

enum class AlertSource(val label: String) {
    GDACS("GDACS"),
    USGS("USGS"),
    NWS("NWS")
}

enum class AlertType(val label: String) {
    EARTHQUAKE("EARTHQUAKE"),
    FLOOD("FLOOD"),
    CYCLONE("CYCLONE"),
    WILDFIRE("WILDFIRE"),
    VOLCANO("VOLCANO"),
    OTHER("OTHER")
}
