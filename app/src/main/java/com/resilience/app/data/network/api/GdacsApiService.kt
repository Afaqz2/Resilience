package com.resilience.app.data.network.api

import com.resilience.app.data.network.dto.GdacsFeatureCollection
import retrofit2.http.GET
import retrofit2.http.Query

interface GdacsApiService {
    @GET("api/events/geteventlist/SEARCH")
    suspend fun getAlerts(
        @Query("eventlist") eventList: String = "EQ,FL,TC,VO,WF",
        @Query("alertlevel") alertLevel: String = "Red,Orange",
        @Query("limit") limit: Int = 20
    ): GdacsFeatureCollection
}
