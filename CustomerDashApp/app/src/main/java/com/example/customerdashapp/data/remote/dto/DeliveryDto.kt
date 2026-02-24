package com.example.customerdashapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============================================
// REQUEST DTOs
// ============================================

data class CreateDeliveryRequest(
    @SerializedName("pickup_address") val pickupAddress: String,
    @SerializedName("pickup_lat") val pickupLat: Double,
    @SerializedName("pickup_lng") val pickupLng: Double,
    @SerializedName("drop_off_address") val dropOffAddress: String,
    @SerializedName("drop_off_lat") val dropOffLat: Double,
    @SerializedName("drop_off_lng") val dropOffLng: Double,
    @SerializedName("vehicle_type") val vehicleType: String = "MOTORCYCLE",
    val notes: String? = null,
    val items: List<String>? = null,
    @SerializedName("items_photo_url") val itemsPhotoUrl: String? = null,
    @SerializedName("requires_loading_help") val requiresLoadingHelp: Boolean = false
)

data class CancelDeliveryRequest(
    val reason: String? = null
)

data class RateDeliveryRequest(
    val rating: Int,
    val review: String? = null
)

data class AddAddressRequest(
    val label: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    @SerializedName("is_default") val isDefault: Boolean = false
)

// ============================================
// RESPONSE DTOs
// ============================================

data class DeliveryData(
    @SerializedName("delivery_id") val deliveryId: String? = null,
    val status: String? = null,
    @SerializedName("pickup_address") val pickupAddress: String? = null,
    @SerializedName("pickup_lat") val pickupLat: Double? = null,
    @SerializedName("pickup_lng") val pickupLng: Double? = null,
    @SerializedName("drop_off_address") val dropOffAddress: String? = null,
    @SerializedName("drop_off_lat") val dropOffLat: Double? = null,
    @SerializedName("drop_off_lng") val dropOffLng: Double? = null,
    @SerializedName("total_price") val totalPrice: Double? = null,
    @SerializedName("distance_km") val distanceKm: Double? = null,
    @SerializedName("vehicle_type") val vehicleType: String? = null,
    val notes: String? = null,
    val items: Any? = null,
    @SerializedName("requires_loading_help") val requiresLoadingHelp: Boolean? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("accepted_at") val acceptedAt: String? = null,
    @SerializedName("picked_up_at") val pickedUpAt: String? = null,
    @SerializedName("delivered_at") val deliveredAt: String? = null,
    @SerializedName("cancelled_at") val cancelledAt: String? = null,
    @SerializedName("cancelled_by") val cancelledBy: String? = null,
    @SerializedName("cancellation_reason") val cancellationReason: String? = null,
    // Nested driver info from Supabase join
    val driver: DriverInfo? = null
)

data class DriverInfo(
    @SerializedName("name") val name: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("current_lat") val currentLat: Double? = null,
    @SerializedName("current_lng") val currentLng: Double? = null,
    @SerializedName("last_location_update") val lastLocationUpdate: String? = null
)

data class TrackingData(
    val delivery: DeliveryData? = null,
    @SerializedName("location_history") val locationHistory: List<LocationPoint>? = null
)

data class LocationPoint(
    val lat: Double,
    val lng: Double,
    @SerializedName("recorded_at") val recordedAt: String? = null
)

data class AddressData(
    @SerializedName("address_id") val addressId: String? = null,
    val label: String? = null,
    val address: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerializedName("is_default") val isDefault: Boolean? = null
)

data class RatingData(
    @SerializedName("rating_id") val ratingId: String? = null,
    @SerializedName("customer_rating") val customerRating: Int? = null,
    @SerializedName("customer_review") val customerReview: String? = null
)

// ============================================
// CUSTOMER PROFILE DTOs
// ============================================

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null
)
