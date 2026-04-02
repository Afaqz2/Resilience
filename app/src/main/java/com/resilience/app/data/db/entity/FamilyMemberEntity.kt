package com.resilience.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sprint 2 — Family Safety Vault: individual family member profile.
 *
 * Stored in the encrypted SQLCipher-backed Room database.
 * Blood group, allergies, and medication fields are considered
 * sensitive; the entire DB is encrypted at rest.
 */
@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String,          // e.g. "Spouse", "Child", "Parent"
    val bloodGroup: String,            // e.g. "O+"
    val allergies: String,             // comma-separated list
    val medications: String,           // comma-separated list
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val notes: String = ""
)
