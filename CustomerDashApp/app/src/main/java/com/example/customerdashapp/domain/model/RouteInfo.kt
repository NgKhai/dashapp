package com.example.customerdashapp.domain.model

import org.osmdroid.util.GeoPoint

data class RouteInfo(
    val points: List<GeoPoint>,
    val distanceKm: Double,
    val durationMinutes: Double,
    val routeEncoded: String? = null
)
