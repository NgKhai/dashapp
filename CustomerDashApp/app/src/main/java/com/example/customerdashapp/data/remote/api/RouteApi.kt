package com.example.customerdashapp.data.remote.api

import com.example.customerdashapp.data.remote.dto.RouteResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RouteApi {

    @GET("/routes")
    suspend fun getRoute(
        @Query("pickup_lat")  pickupLat: Double,
        @Query("pickup_lng")  pickupLng: Double,
        @Query("dropoff_lat") dropoffLat: Double,
        @Query("dropoff_lng") dropoffLng: Double
    ): RouteResponse
}
