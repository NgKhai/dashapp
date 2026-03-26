package com.example.customerdashapp.data.remote.dto

import com.example.customerdashapp.domain.model.Address
import com.example.customerdashapp.domain.model.Customer
import com.example.customerdashapp.domain.model.Delivery
import com.example.customerdashapp.domain.model.DeliveryStatus
import com.example.customerdashapp.domain.model.TrackingInfo
import com.example.customerdashapp.domain.model.LocationPoint

/**
 * Centralized DTO → Domain mappers.
 * Extracted from repository implementations to avoid duplication.
 */

fun DeliveryData.toDomain(): Delivery {
    return Delivery(
        deliveryId = deliveryId ?: "",
        status = DeliveryStatus.fromString(status ?: "PENDING"),
        pickupAddress = pickupAddress ?: "",
        pickupLat = pickupLat ?: 0.0,
        pickupLng = pickupLng ?: 0.0,
        dropOffAddress = dropOffAddress ?: "",
        dropOffLat = dropOffLat ?: 0.0,
        dropOffLng = dropOffLng ?: 0.0,
        totalPrice = totalPrice ?: 0.0,
        distanceKm = distanceKm ?: 0.0,
        vehicleType = vehicleType ?: "MOTORCYCLE",
        notes = notes,
        items = when (items) {
            is List<*> -> items.mapNotNull { it?.toString() }
            else -> emptyList()
        },
        itemsPhotoUrls = parsePhotoUrls(itemsPhotoUrl),
        requiresLoadingHelp = requiresLoadingHelp ?: false,
        driverName = driver?.name,
        driverPhone = driver?.phone,
        createdAt = createdAt,
        acceptedAt = acceptedAt,
        pickedUpAt = pickedUpAt,
        deliveredAt = deliveredAt,
        cancelledAt = cancelledAt,
        cancelledBy = cancelledBy,
        cancellationReason = cancellationReason,
        routeEncoded = routeEncoded
    )
}

/**
 * Parse items_photo_url from various possible formats:
 * - null -> emptyList()
 * - JSON array string: "[\"url1\",\"url2\"]" -> listOf("url1","url2")
 * - Plain URL string: "https://..." -> listOf("https://...")
 * - List<*> (already deserialized) -> list of strings
 */
private fun parsePhotoUrls(value: Any?): List<String> {
    if (value == null) return emptyList()

    return when (value) {
        is List<*> -> value.mapNotNull { it?.toString() }.filter { it.isNotBlank() }
        is String -> {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) return emptyList()
            if (trimmed.startsWith("[")) {
                // JSON array string: parse manually
                try {
                    com.google.gson.Gson().fromJson(
                        trimmed,
                        Array<String>::class.java
                    ).toList()
                } catch (_: Exception) {
                    listOf(trimmed)
                }
            } else {
                listOf(trimmed)
            }
        }
        else -> emptyList()
    }
}

fun AddressData.toDomain(): Address {
    return Address(
        addressId = addressId ?: "",
        label = label ?: "",
        address = address ?: "",
        lat = lat ?: 0.0,
        lng = lng ?: 0.0,
        isDefault = isDefault ?: false
    )
}

fun CustomerData.toDomain(): Customer {
    return Customer(
        customerId = customerId,
        name = name,
        phone = phone ?: "",
        email = email,
        avatarUrl = avatarUrl
    )
}

fun TrackingData.toDomain(): TrackingInfo {
    return TrackingInfo(
        delivery = delivery?.toDomain() ?: throw Exception("Missing delivery in tracking data"),
        driverLat = delivery.driver?.currentLat,
        driverLng = delivery.driver?.currentLng,
        lastLocationUpdate = delivery.driver?.lastLocationUpdate,
        locationHistory = locationHistory?.map { point ->
            LocationPoint(
                lat = point.lat,
                lng = point.lng,
                recordedAt = point.recordedAt
            )
        } ?: emptyList()
    )
}

