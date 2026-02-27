package com.example.driverdashapp.domain.model

data class Driver(
    val driverId: String,
    val name: String,
    val phone: String?,
    val email: String? = null,
    val isVerified: Boolean = false,
    val isOnline: Boolean = false,
    val rating: Double? = null,
    val totalRatings: Int = 0,
    val totalDeliveries: Int = 0
)

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
    val customerName: String? = null,
    val customerPhone: String? = null,
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
    PENDING, ACCEPTED, PICKED_UP, DELIVERING, COMPLETED, CANCELLED;

    companion object {
        fun fromString(value: String): DeliveryStatus =
            entries.find { it.name == value } ?: PENDING
    }
}

data class Earnings(
    val totalDeliveries: Int,
    val totalEarnings: Double,
    val todayEarnings: Double,
    val rating: Double?,
    val totalRatings: Int
)

data class Vehicle(
    val vehicleId: String,
    val vehicleType: String,
    val licensePlate: String,
    val brand: String?,
    val model: String?,
    val color: String?,
    val year: Int?
)

data class VehicleAssignment(
    val id: String,
    val isPrimary: Boolean,
    val vehicle: Vehicle?
)

sealed class LoginResponse {
    data object RequirePin : LoginResponse()
    data class RequireOtp(val phone: String) : LoginResponse()
    data class Success(val driver: Driver) : LoginResponse()
}
