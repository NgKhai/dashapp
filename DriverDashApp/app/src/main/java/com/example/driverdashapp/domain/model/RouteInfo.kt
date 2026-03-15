package com.example.driverdashapp.domain.model

data class RouteInfo(
    val points: List<LatLng>,
    val distanceKm: Double,
    val durationMinutes: Double
)
