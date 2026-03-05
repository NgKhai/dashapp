package com.example.customerdashapp.data.remote.dto

import com.example.customerdashapp.domain.model.PricingConfig
import com.google.gson.annotations.SerializedName

data class PricingData(
    @SerializedName("vehicle_type") val vehicleType: String,
    @SerializedName("base_fare") val baseFare: Long,
    @SerializedName("per_km") val perKm: Long,
    @SerializedName("loading_help_fee") val loadingHelpFee: Long
)

fun PricingData.toDomain() = PricingConfig(
    vehicleType = vehicleType,
    baseFare = baseFare,
    perKm = perKm,
    loadingHelpFee = loadingHelpFee
)
