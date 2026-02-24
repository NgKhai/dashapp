package com.example.customerdashapp.domain.model

data class Address(
    val addressId: String,
    val label: String,
    val address: String,
    val lat: Double,
    val lng: Double,
    val isDefault: Boolean = false
)
