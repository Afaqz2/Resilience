package com.resilience.app.data.network.dto

import com.google.gson.annotations.SerializedName

data class GdacsFeatureCollection(
    @SerializedName("features") val features: List<GdacsFeature>? = null
)

data class GdacsFeature(
    @SerializedName("properties") val properties: GdacsProperties,
    @SerializedName("geometry") val geometry: GdacsGeometry?
)

data class GdacsProperties(
    @SerializedName("eventid") val eventId: Long?,
    @SerializedName("episodeid") val episodeId: Long?,
    @SerializedName("eventtype") val eventType: String?,    // EQ, FL, TC, VO, DR, WF
    @SerializedName("alertlevel") val alertLevel: String?,  // Red, Orange, Green
    @SerializedName("eventname") val eventName: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("fromdate") val fromDate: String?,
    @SerializedName("description") val description: String?
)

data class GdacsGeometry(
    @SerializedName("type") val type: String?,
    @SerializedName("coordinates") val coordinates: List<Double>?  // [lon, lat]
)
