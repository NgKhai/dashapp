package com.example.driverdashapp.domain.model

/**
 * Framework-agnostic coordinate pair for the domain layer.
 * Map to org.osmdroid.util.GeoPoint at the presentation boundary.
 */
data class LatLng(
    val lat: Double,
    val lng: Double
)
