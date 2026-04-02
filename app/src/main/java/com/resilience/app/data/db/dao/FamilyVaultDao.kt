package com.resilience.app.data.db.dao

import androidx.room.*
import com.resilience.app.data.db.entity.FamilyMemberEntity
import com.resilience.app.data.db.entity.MeetingPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyVaultDao {

    /* ---------- Family Members ---------- */

    @Query("SELECT * FROM family_members ORDER BY name")
    fun getAllMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: Long): FamilyMemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMember(member: FamilyMemberEntity): Long

    @Delete
    suspend fun deleteMember(member: FamilyMemberEntity)

    /* ---------- Meeting Points ---------- */

    @Query("SELECT * FROM meeting_points ORDER BY priority")
    fun getAllMeetingPoints(): Flow<List<MeetingPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeetingPoint(point: MeetingPointEntity): Long

    @Delete
    suspend fun deleteMeetingPoint(point: MeetingPointEntity)
}
