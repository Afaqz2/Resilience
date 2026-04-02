package com.resilience.app.di

import android.content.Context
import com.resilience.app.data.db.ResilienceDatabase
import com.resilience.app.data.db.dao.FamilyVaultDao
import com.resilience.app.data.db.dao.OfflineRegionDao
import com.resilience.app.data.db.dao.PlaybookDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Sprint 2/3 — Provides the Room database and its DAOs to the Hilt graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ResilienceDatabase =
        ResilienceDatabase.create(context)

    @Provides
    @Singleton
    fun providePlaybookDao(db: ResilienceDatabase): PlaybookDao = db.playbookDao()

    @Provides
    @Singleton
    fun provideFamilyVaultDao(db: ResilienceDatabase): FamilyVaultDao = db.familyVaultDao()

    @Provides
    @Singleton
    fun provideOfflineRegionDao(db: ResilienceDatabase): OfflineRegionDao = db.offlineRegionDao()
}
