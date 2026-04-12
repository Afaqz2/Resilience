package com.resilience.app.data.network.dto

import com.google.gson.annotations.SerializedName

data class UsgsFeatureCollection(
    @SerializedName("features") val features: List<UsgsFeature> = emptyList()
)

data class UsgsFeature(
    @SerializedName("id") val id: String,
    @SerializedName("properties") val properties: UsgsProperties,
    @SerializedName("geometry") val geometry: UsgsGeometry
)

data class UsgsProperties(
    @SerializedName("mag") val mag: Double?,
    @SerializedName("place") val place: String?,
    @SerializedName("time") val time: Long?,
    @SerializedName("title") val title: String?,
    @SerializedName("alert") val alert: String?,  // green, yellow, orange, red
    @SerializedName("sig") val sig: Int?           // significance 0–1000
)

data class UsgsGeometry(
    @SerializedName("coordinates") val coordinates: List<Double> = emptyList()  // [lon, lat, depth]
)
