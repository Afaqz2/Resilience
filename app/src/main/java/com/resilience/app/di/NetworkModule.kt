package com.resilience.app.di

import com.resilience.app.data.network.api.GdacsApiService
import com.resilience.app.data.network.api.UsgsApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    @Provides
    @Singleton
    @Named("gdacs")
    fun provideGdacsRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://www.gdacs.org/gdacsapi/")
        .client(baseClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    @Named("usgs")
    fun provideUsgsRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://earthquake.usgs.gov/")
        .client(baseClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideGdacsApiService(@Named("gdacs") retrofit: Retrofit): GdacsApiService =
        retrofit.create(GdacsApiService::class.java)

    @Provides
    @Singleton
    fun provideUsgsApiService(@Named("usgs") retrofit: Retrofit): UsgsApiService =
        retrofit.create(UsgsApiService::class.java)
}
