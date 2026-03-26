package com.example.customerdashapp.domain.model

/**
 * Encapsulates all parameters needed to create a delivery.
 * Replaces the 9-parameter createDelivery() function signature.
 */
data class CreateDeliveryParams(
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropOffAddress: String,
    val dropOffLat: Double,
    val dropOffLng: Double,
    val vehicleType: String = "MOTORCYCLE",
    val notes: String? = null,
    val items: List<String>? = null,
    val requiresLoadingHelp: Boolean = false,
    val routeEncoded: String? = null,
    val distanceKm: Double? = null,
    val itemsPhotoUrls: List<String>? = null
)
