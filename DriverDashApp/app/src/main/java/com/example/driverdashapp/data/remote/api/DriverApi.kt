package com.example.driverdashapp.data.remote.api

import com.example.driverdashapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface DriverApi {

    // ============================================
    // PROFILE
    // ============================================

    @GET("drivers/profile")
    suspend fun getProfile(): Response<ApiResponse<DriverData>>

    @PUT("drivers/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<DriverData>>

    // ============================================
    // STATUS & LOCATION
    // ============================================

    @PUT("drivers/status")
    suspend fun updateStatus(@Body request: UpdateStatusRequest): Response<ApiResponse<DriverData>>

    @PUT("drivers/location")
    suspend fun updateLocation(@Body request: UpdateLocationRequest): Response<ApiResponse<Any>>

    // ============================================
    // VEHICLES
    // ============================================

    @GET("drivers/vehicles")
    suspend fun getVehicles(): Response<ApiResponse<List<VehicleAssignmentData>>>

    @PUT("drivers/vehicles/{id}/primary")
    suspend fun setPrimaryVehicle(@Path("id") assignmentId: String): Response<ApiResponse<VehicleAssignmentData>>

    // ============================================
    // DELIVERIES
    // ============================================

    @GET("drivers/pending")
    suspend fun getPendingDeliveries(@Query("limit") limit: Int = 10): Response<ApiResponse<List<DeliveryData>>>

    @GET("drivers/deliveries")
    suspend fun getMyDeliveries(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<List<DeliveryData>>>

    @GET("drivers/earnings")
    suspend fun getEarnings(): Response<ApiResponse<EarningsData>>
}
