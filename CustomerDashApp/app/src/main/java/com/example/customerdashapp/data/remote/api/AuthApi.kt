package com.example.customerdashapp.data.remote.api

import com.example.customerdashapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/customer/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<LoginResponseData>>

    /**
     * Unified login endpoint.
     * - Phone only → returns require_pin or require_otp
     * - Phone + PIN → returns customer data (login success)
     */
    @POST("auth/customer/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponseData>>

    @POST("auth/customer/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<VerifyOtpData>>

    @POST("auth/set-pin")
    suspend fun setPin(@Body request: SetPinRequest): Response<ApiResponse<SetPinData>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<SessionData>>
}
