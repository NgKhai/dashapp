package com.example.customerdashapp.domain.repository

import com.example.customerdashapp.domain.model.Address
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.CreateDeliveryParams
import com.example.customerdashapp.domain.model.Delivery
import com.example.customerdashapp.domain.model.PricingConfig
import com.example.customerdashapp.domain.model.TrackingInfo

interface DeliveryRepository {

    suspend fun createDelivery(params: CreateDeliveryParams): AppResult<Delivery>

    suspend fun getDelivery(deliveryId: String): AppResult<Delivery>

    suspend fun trackDelivery(deliveryId: String): AppResult<TrackingInfo>

    suspend fun getMyDeliveries(
        status: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): AppResult<List<Delivery>>

    suspend fun cancelDelivery(
        deliveryId: String,
        reason: String? = null
    ): AppResult<Delivery>

    suspend fun rateDelivery(
        deliveryId: String,
        rating: Int,
        review: String? = null
    ): AppResult<Unit>

    suspend fun getAddresses(): AppResult<List<Address>>

    suspend fun addAddress(
        label: String,
        address: String,
        lat: Double,
        lng: Double,
        isDefault: Boolean = false
    ): AppResult<Address>

    suspend fun getPricing(): AppResult<List<PricingConfig>>
}

