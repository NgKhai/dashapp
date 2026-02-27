package com.example.customerdashapp.domain.model

data class Delivery(
    val deliveryId: String,
    val status: DeliveryStatus,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropOffAddress: String,
    val dropOffLat: Double,
    val dropOffLng: Double,
    val totalPrice: Double,
    val distanceKm: Double,
    val vehicleType: String = "MOTORCYCLE",
    val notes: String? = null,
    val items: List<String> = emptyList(),
    val requiresLoadingHelp: Boolean = false,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val createdAt: String? = null,
    val acceptedAt: String? = null,
    val pickedUpAt: String? = null,
    val deliveredAt: String? = null,
    val cancelledAt: String? = null,
    val cancelledBy: String? = null,
    val cancellationReason: String? = null,
    val routeEncoded: String? = null    // Pre-computed OSRM encoded polyline; null = straight line fallback
)

enum class DeliveryStatus {
    PENDING,
    ACCEPTED,
    PICKED_UP,
    DELIVERING,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromString(value: String): DeliveryStatus {
            return entries.find { it.name == value } ?: PENDING
        }
    }
}
