package com.resilience.app.data.repository

import com.resilience.app.data.db.dao.FamilyVaultDao
import com.resilience.app.data.db.entity.FamilyMemberEntity
import com.resilience.app.data.db.entity.MeetingPointEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for Family Safety Vault data.
 *
 * All data lives in the SQLCipher-encrypted Room database — no
 * network access ever occurs for this repository.
 */
@Singleton
class FamilyVaultRepository @Inject constructor(
    private val dao: FamilyVaultDao
) {

    /* ---------- Family Members ---------- */

    fun getAllMembers(): Flow<List<FamilyMemberEntity>> = dao.getAllMembers()

    suspend fun getMemberById(id: Long): FamilyMemberEntity? = dao.getMemberById(id)

    suspend fun saveMember(member: FamilyMemberEntity): Long = dao.upsertMember(member)

    suspend fun deleteMember(member: FamilyMemberEntity) = dao.deleteMember(member)

    /* ---------- Meeting Points ---------- */

    fun getAllMeetingPoints(): Flow<List<MeetingPointEntity>> = dao.getAllMeetingPoints()

    suspend fun saveMeetingPoint(point: MeetingPointEntity): Long = dao.upsertMeetingPoint(point)

    suspend fun deleteMeetingPoint(point: MeetingPointEntity) = dao.deleteMeetingPoint(point)
}
