package com.resilience.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.resilience.app.data.db.dao.FamilyVaultDao
import com.resilience.app.data.db.dao.PlaybookDao
import com.resilience.app.data.db.entity.FamilyMemberEntity
import com.resilience.app.data.db.entity.MeetingPointEntity
import com.resilience.app.data.db.entity.PlaybookEntity
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * Central Room database for the Resilience app.
 *
 * - Encrypted at rest via SQLCipher (SupportFactory).
 * - Version is bumped on every schema change; migration strategy TBD in Sprint 3+.
 *
 * Version history:
 *   1 → Sprint 2: playbooks, family_members, meeting_points
 */
@Database(
    entities = [
        PlaybookEntity::class,
        FamilyMemberEntity::class,
        MeetingPointEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class ResilienceDatabase : RoomDatabase() {

    abstract fun playbookDao(): PlaybookDao
    abstract fun familyVaultDao(): FamilyVaultDao

    companion object {
        private const val DB_NAME = "resilience.db"

        /**
         * Passphrase is currently the fixed string below.
         * Sprint 3+ will derive this from Android Keystore + user PIN.
         */
        private const val DB_PASSPHRASE = "resilience_secure_key_v1"

        fun create(context: Context): ResilienceDatabase {
            val passphrase = SQLiteDatabase.getBytes(DB_PASSPHRASE.toCharArray())
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                ResilienceDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration() // Safe for MVP; replace with real migrations in release
                .build()
        }
    }
}
