package com.example.customerdashapp.data.remote.api

import com.example.customerdashapp.data.remote.dto.NominatimResult
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("countrycodes") countryCodes: String = "vn",
        @Query("limit") limit: Int = 8,
        @Query("addressdetails") addressDetails: Int = 1
    ): List<NominatimResult>

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1
    ): NominatimResult
}
