package com.resilience.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sprint 2 — Survival Playbook entry stored in Room.
 *
 * All playbooks are shipped as bundled JSON assets and seeded into the
 * local Room database on first launch.  They are fully available offline.
 */
@Entity(tableName = "playbooks")
data class PlaybookEntity(
    @PrimaryKey val id: String,
    /** e.g. "First Aid", "Shelter", "Go-Bag", "Water Purification" */
    val category: String,
    val title: String,
    val summary: String,
    /** Full Markdown body — rendered in the detail screen */
    val contentMarkdown: String,
    /** Human-readable source, e.g. "WHO", "Red Cross", "Ready.gov" */
    val sourceOrganisation: String,
    /** ISO-8601 date string, e.g. "2024-01-15" */
    val lastVerifiedDate: String,
    /** Material icon name string (used to pick the icon at runtime) */
    val iconName: String = "HealthAndSafety",
    /** Accent colour hex string for the card, e.g. "#E53935" */
    val accentColor: String = "#E53935"
)
