package com.example.driverdashapp.data.remote.dto

/**
 * OSRM route response DTOs.
 * API: https://router.project-osrm.org/route/v1/driving/{lng},{lat};{lng},{lat}?overview=full&geometries=geojson
 */
data class OsrmRouteResponse(
    val code: String,
    val routes: List<OsrmRoute> = emptyList()
)

data class OsrmRoute(
    val distance: Double,   // meters
    val duration: Double,   // seconds
    val geometry: OsrmGeometry
)

data class OsrmGeometry(
    val type: String,                    // "LineString"
    val coordinates: List<List<Double>>  // [[lng, lat], ...]
)
