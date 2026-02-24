package com.example.driverdashapp.data.remote.api

import com.example.driverdashapp.data.remote.dto.OsrmRouteResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OsrmApi {

    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson"
    ): OsrmRouteResponse
}
