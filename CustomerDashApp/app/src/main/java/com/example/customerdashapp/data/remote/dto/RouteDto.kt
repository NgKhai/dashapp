package com.example.customerdashapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RouteResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data")    val data: RouteData?
)

data class RouteData(
    @SerializedName("route_encoded")    val routeEncoded: String?,
    @SerializedName("distance_km")      val distanceKm: Double,
    @SerializedName("duration_minutes") val durationMinutes: Double
)
