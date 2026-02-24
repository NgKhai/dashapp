package com.example.driverdashapp.data.remote.api

import com.example.driverdashapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface DeliveryApi {

    @GET("deliveries/{id}")
    suspend fun getDelivery(@Path("id") deliveryId: String): Response<ApiResponse<DeliveryData>>

    @PUT("deliveries/{id}/accept")
    suspend fun acceptDelivery(@Path("id") deliveryId: String): Response<ApiResponse<DeliveryData>>

    @PUT("deliveries/{id}/pickup")
    suspend fun pickupDelivery(@Path("id") deliveryId: String): Response<ApiResponse<DeliveryData>>

    @PUT("deliveries/{id}/delivering")
    suspend fun deliveringDelivery(@Path("id") deliveryId: String): Response<ApiResponse<DeliveryData>>

    @PUT("deliveries/{id}/complete")
    suspend fun completeDelivery(@Path("id") deliveryId: String): Response<ApiResponse<DeliveryData>>

    @PUT("deliveries/{id}/cancel")
    suspend fun cancelDelivery(
        @Path("id") deliveryId: String,
        @Body request: CancelDeliveryRequest
    ): Response<ApiResponse<DeliveryData>>

    @POST("deliveries/{id}/rate")
    suspend fun rateDelivery(
        @Path("id") deliveryId: String,
        @Body request: RateDeliveryRequest
    ): Response<ApiResponse<Any>>

    @GET("deliveries/{id}/track")
    suspend fun getTracking(
        @Path("id") deliveryId: String
    ): Response<ApiResponse<TrackingData>>
}
