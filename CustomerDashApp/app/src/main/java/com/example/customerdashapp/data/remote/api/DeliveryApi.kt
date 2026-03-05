package com.example.customerdashapp.data.remote.api

import com.example.customerdashapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface DeliveryApi {

    // ============================================
    // DELIVERY ENDPOINTS
    // ============================================

    @POST("/deliveries")
    suspend fun createDelivery(
        @Body request: CreateDeliveryRequest
    ): Response<ApiResponse<DeliveryData>>

    @GET("/deliveries/{id}")
    suspend fun getDelivery(
        @Path("id") deliveryId: String
    ): Response<ApiResponse<DeliveryData>>

    @GET("/deliveries/{id}/track")
    suspend fun trackDelivery(
        @Path("id") deliveryId: String
    ): Response<ApiResponse<TrackingData>>

    @PUT("/deliveries/{id}/cancel")
    suspend fun cancelDelivery(
        @Path("id") deliveryId: String,
        @Body request: CancelDeliveryRequest
    ): Response<ApiResponse<DeliveryData>>

    @POST("/deliveries/{id}/rate")
    suspend fun rateDelivery(
        @Path("id") deliveryId: String,
        @Body request: RateDeliveryRequest
    ): Response<ApiResponse<RatingData>>

    // ============================================
    // CUSTOMER ENDPOINTS
    // ============================================

    @GET("/customers/deliveries")
    suspend fun getMyDeliveries(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<DeliveryData>>>

    @GET("/customers/addresses")
    suspend fun getAddresses(): Response<ApiResponse<List<AddressData>>>

    @POST("/customers/addresses")
    suspend fun addAddress(
        @Body request: AddAddressRequest
    ): Response<ApiResponse<AddressData>>

    // ============================================
    // PRICING ENDPOINT
    // ============================================

    @GET("/pricing")
    suspend fun getPricing(): Response<ApiResponse<List<PricingData>>>
}
