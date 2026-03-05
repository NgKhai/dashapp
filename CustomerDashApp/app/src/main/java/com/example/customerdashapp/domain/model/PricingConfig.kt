package com.example.customerdashapp.domain.model

/**
 * Delivery pricing parameters for a specific vehicle type, fetched from the backend.
 */
data class PricingConfig(
    val vehicleType: String,
    val baseFare: Long,
    val perKm: Long,
    val loadingHelpFee: Long
)
