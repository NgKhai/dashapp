package com.example.customerdashapp.data.remote.api

import com.example.customerdashapp.data.remote.dto.OsrmRouteResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface OsrmApi {

    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates", encoded = true) coordinates: String,
        @retrofit2.http.Query("overview") overview: String = "full",
        @retrofit2.http.Query("geometries") geometries: String = "geojson"
    ): OsrmRouteResponse
}
