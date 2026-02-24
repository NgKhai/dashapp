package com.example.customerdashapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NominatimResult(
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("address") val address: NominatimAddress? = null
)

data class NominatimAddress(
    val road: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    @SerializedName("city_district") val cityDistrict: String? = null,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
    @SerializedName("house_number") val houseNumber: String? = null
)
