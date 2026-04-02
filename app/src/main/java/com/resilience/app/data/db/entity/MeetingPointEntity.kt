package com.resilience.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sprint 2 — Family Safety Vault: pre-agreed rendezvous points.
 *
 * Supports Primary / Secondary / Tertiary priority so the family
 * always has a fallback location if a site is inaccessible.
 */
@Entity(tableName = "meeting_points")
data class MeetingPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,                 // e.g. "Primary — School Gate"
    val latitude: Double,
    val longitude: Double,
    /** Plain-text navigation instructions — works with no map display */
    val plainTextDirections: String,
    /** 1 = Primary, 2 = Secondary, 3 = Tertiary */
    val priority: Int = 1
)
