package com.example.customerdashapp.domain.model

/**
 * Framework-agnostic latitude/longitude pair for the domain layer.
 * Keeps the domain free of osmdroid (GeoPoint) dependencies.
 */
data class LatLng(
    val lat: Double,
    val lng: Double
)
