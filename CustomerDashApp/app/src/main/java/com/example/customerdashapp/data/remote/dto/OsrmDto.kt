package com.example.customerdashapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OsrmRouteResponse(
    val code: String,
    val routes: List<OsrmRoute>
)

data class OsrmRoute(
    val geometry: OsrmGeometry,
    val distance: Double,   // meters
    val duration: Double    // seconds
)

data class OsrmGeometry(
    val type: String,
    val coordinates: List<List<Double>>  // [[lng, lat], [lng, lat], ...]
)
