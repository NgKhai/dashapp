package com.example.customerdashapp.domain.model

data class RouteInfo(
    val points: List<LatLng>,
    val distanceKm: Double,
    val durationMinutes: Double,
    val routeEncoded: String? = null
)
