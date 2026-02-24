package com.example.customerdashapp.domain.model

data class TrackingInfo(
    val delivery: Delivery,
    val driverLat: Double?,
    val driverLng: Double?,
    val lastLocationUpdate: String?,
    val locationHistory: List<LocationPoint> = emptyList()
)

data class LocationPoint(
    val lat: Double,
    val lng: Double,
    val recordedAt: String? = null
)
