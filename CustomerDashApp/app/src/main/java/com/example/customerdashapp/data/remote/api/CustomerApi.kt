package com.example.customerdashapp.data.remote.api

import com.example.customerdashapp.data.remote.dto.ApiResponse
import com.example.customerdashapp.data.remote.dto.CustomerData
import com.example.customerdashapp.data.remote.dto.UpdateProfileRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface CustomerApi {

    @GET("/customers/profile")
    suspend fun getProfile(): Response<ApiResponse<CustomerData>>

    @PUT("/customers/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<CustomerData>>
}
