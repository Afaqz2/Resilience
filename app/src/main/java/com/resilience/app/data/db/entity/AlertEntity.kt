package com.resilience.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val severity: String,   // AlertSeverity.name
    val source: String,     // AlertSource.name
    val type: String,       // AlertType.name
    val lat: Double,
    val lon: Double,
    val fetchedAt: Long
)
