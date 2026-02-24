package com.example.customerdashapp.domain.model

data class Customer(
    val customerId: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val avatarUrl: String? = null
)
