package com.resilience.app.di

import android.content.Context
import com.resilience.app.data.datastore.CrisisModeDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCrisisModeDataStore(@ApplicationContext context: Context): CrisisModeDataStore {
        return CrisisModeDataStore(context)
    }
}
