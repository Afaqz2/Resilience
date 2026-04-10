package com.resilience.app.data.network.api

import com.resilience.app.data.network.dto.UsgsFeatureCollection
import retrofit2.http.GET

interface UsgsApiService {
    @GET("earthquakes/feed/v1.0/summary/significant_week.geojson")
    suspend fun getSignificantEarthquakes(): UsgsFeatureCollection
}
