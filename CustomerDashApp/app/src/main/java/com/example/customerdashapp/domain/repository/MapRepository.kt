package com.example.customerdashapp.domain.repository

import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.RouteInfo
import com.example.customerdashapp.domain.model.SearchResult

interface MapRepository {
    suspend fun searchAddress(query: String): AppResult<List<SearchResult>>
    suspend fun reverseGeocode(lat: Double, lng: Double): AppResult<SearchResult>
    suspend fun getRoute(startLat: Double, startLng: Double, endLat: Double, endLng: Double): AppResult<RouteInfo>
}
