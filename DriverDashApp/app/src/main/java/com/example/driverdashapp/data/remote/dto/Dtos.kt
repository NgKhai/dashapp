package com.example.driverdashapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Generic API response wrapper matching backend JSON structure.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)

// ============================================
// AUTH DTOs
// ============================================

data class LoginRequest(
    val phone: String,
    val pin: String? = null
)

data class RegisterRequest(
    val phone: String,
    val name: String
)

data class VerifyOtpRequest(
    val phone: String,
    val otp: String,
    val name: String? = null
)

data class SetPinRequest(
    val pin: String,
    @SerializedName("user_type")
    val userType: String = "driver",
    val name: String? = null
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

/**
 * Login response: may contain require_pin, require_otp, or driver+session
 */
data class LoginData(
    @SerializedName("require_pin")
    val requirePin: Boolean? = null,
    @SerializedName("require_otp")
    val requireOtp: Boolean? = null,
    val driver: DriverData? = null,
    val session: SessionData? = null,
    val phone: String? = null
)

data class SessionData(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_at")
    val expiresAt: Long? = null
)

data class VerifyOtpData(
    val driver: DriverData?,
    val session: SessionData?
)

data class SetPinData(
    val name: String?
)

// ============================================
// DRIVER DTOs
// ============================================

data class DriverData(
    @SerializedName("driver_id")
    val driverId: String,
    val name: String,
    val phone: String?,
    val email: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null,
    @SerializedName("is_verified")
    val isVerified: Boolean = false,
    @SerializedName("is_online")
    val isOnline: Boolean = false,
    val rating: Double? = null,
    @SerializedName("total_ratings")
    val totalRatings: Int = 0,
    @SerializedName("total_deliveries")
    val totalDeliveries: Int = 0,
    @SerializedName("current_lat")
    val currentLat: Double? = null,
    @SerializedName("current_lng")
    val currentLng: Double? = null
)

data class UpdateStatusRequest(
    @SerializedName("is_online")
    val isOnline: Boolean
)

data class UpdateLocationRequest(
    val lat: Double,
    val lng: Double,
    @SerializedName("delivery_id")
    val deliveryId: String? = null
)

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null
)

data class EarningsData(
    @SerializedName("total_deliveries")
    val totalDeliveries: Int,
    @SerializedName("total_earnings")
    val totalEarnings: Double,
    @SerializedName("today_earnings")
    val todayEarnings: Double,
    val rating: Double?,
    @SerializedName("total_ratings")
    val totalRatings: Int
)

data class VehicleAssignmentData(
    val id: String,
    @SerializedName("is_primary")
    val isPrimary: Boolean,
    @SerializedName("assigned_at")
    val assignedAt: String?,
    val vehicle: VehicleData?
)

data class VehicleData(
    @SerializedName("vehicle_id")
    val vehicleId: String,
    @SerializedName("type")
    val vehicleType: String,
    @SerializedName("plate_number")
    val licensePlate: String,
    val brand: String? = null,
    @SerializedName("model_name")
    val model: String?,
    val color: String?,
    val year: Int? = null
)

// ============================================
// DELIVERY DTOs
// ============================================

data class DeliveryData(
    @SerializedName("delivery_id")
    val deliveryId: String,
    val status: String,
    @SerializedName("pickup_address")
    val pickupAddress: String?,
    @SerializedName("pickup_lat")
    val pickupLat: Double,
    @SerializedName("pickup_lng")
    val pickupLng: Double,
    @SerializedName("drop_off_address")
    val dropOffAddress: String?,
    @SerializedName("drop_off_lat")
    val dropOffLat: Double,
    @SerializedName("drop_off_lng")
    val dropOffLng: Double,
    @SerializedName("total_price")
    val totalPrice: Double,
    @SerializedName("distance_km")
    val distanceKm: Double,
    @SerializedName("vehicle_type")
    val vehicleType: String? = "MOTORCYCLE",
    val notes: String? = null,
    val items: List<String>? = null,
    @SerializedName("requires_loading_help")
    val requiresLoadingHelp: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("accepted_at")
    val acceptedAt: String? = null,
    @SerializedName("picked_up_at")
    val pickedUpAt: String? = null,
    @SerializedName("delivered_at")
    val deliveredAt: String? = null,
    @SerializedName("cancelled_at")
    val cancelledAt: String? = null,
    @SerializedName("cancelled_by")
    val cancelledBy: String? = null,
    @SerializedName("cancellation_reason")
    val cancellationReason: String? = null,
    // Nested customer info
    val customer: CustomerData? = null
) {
    fun toDomain(): com.example.driverdashapp.domain.model.Delivery {
        return com.example.driverdashapp.domain.model.Delivery(
            deliveryId = deliveryId,
            status = com.example.driverdashapp.domain.model.DeliveryStatus.fromString(status),
            pickupAddress = pickupAddress ?: "",
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            dropOffAddress = dropOffAddress ?: "",
            dropOffLat = dropOffLat,
            dropOffLng = dropOffLng,
            totalPrice = totalPrice,
            distanceKm = distanceKm,
            vehicleType = vehicleType ?: "MOTORCYCLE",
            notes = notes,
            items = items ?: emptyList(),
            requiresLoadingHelp = requiresLoadingHelp,
            customerName = customer?.name,
            customerPhone = customer?.phone,
            createdAt = createdAt,
            acceptedAt = acceptedAt,
            pickedUpAt = pickedUpAt,
            deliveredAt = deliveredAt,
            cancelledAt = cancelledAt,
            cancelledBy = cancelledBy,
            cancellationReason = cancellationReason
        )
    }
}

data class CustomerData(
    @SerializedName("customer_id")
    val customerId: String,
    val name: String?,
    val phone: String?,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null
)

data class CancelDeliveryRequest(
    val reason: String? = null
)

data class RateDeliveryRequest(
    val rating: Int,
    val review: String? = null
)

data class TrackingData(
    val delivery: DeliveryData?,
    @SerializedName("location_history")
    val locationHistory: List<LocationPoint>?
)

data class LocationPoint(
    val lat: Double,
    val lng: Double,
    @SerializedName("recorded_at")
    val recordedAt: String?
)
