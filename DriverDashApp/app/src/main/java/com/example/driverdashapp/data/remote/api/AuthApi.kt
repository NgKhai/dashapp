package com.example.driverdashapp.data.remote.api

import com.example.driverdashapp.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {

    @POST("auth/driver/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("auth/driver/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<Any>>

    @POST("auth/driver/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<ApiResponse<VerifyOtpData>>

    @POST("auth/set-pin")
    suspend fun setPin(@Body request: SetPinRequest): Response<ApiResponse<SetPinData>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<SessionData>>
}
