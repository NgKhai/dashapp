package com.example.customerdashapp.data.repository

import com.example.customerdashapp.data.remote.api.NominatimApi
import com.example.customerdashapp.data.remote.api.OsrmApi
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.RouteInfo
import com.example.customerdashapp.domain.model.SearchResult
import com.example.customerdashapp.domain.repository.MapRepository
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(
    private val nominatimApi: NominatimApi,
    private val osrmApi: OsrmApi
) : MapRepository {

    override suspend fun searchAddress(query: String): AppResult<List<SearchResult>> {
        return try {
            val results = nominatimApi.search(query = query)
            val searchResults = results.map { result ->
                SearchResult(
                    displayName = result.displayName,
                    lat = result.lat.toDoubleOrNull() ?: 0.0,
                    lng = result.lon.toDoubleOrNull() ?: 0.0
                )
            }
            AppResult.Success(searchResults)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi tìm kiếm địa chỉ")
        }
    }

    override suspend fun reverseGeocode(lat: Double, lng: Double): AppResult<SearchResult> {
        return try {
            val result = nominatimApi.reverse(lat = lat, lon = lng)
            AppResult.Success(
                SearchResult(
                    displayName = result.displayName,
                    lat = result.lat.toDoubleOrNull() ?: lat,
                    lng = result.lon.toDoubleOrNull() ?: lng
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi tìm địa chỉ")
        }
    }

    override suspend fun getRoute(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): AppResult<RouteInfo> {
        return try {
            // OSRM expects coordinates as lng,lat;lng,lat
            val coordinates = "$startLng,$startLat;$endLng,$endLat"
            val response = osrmApi.getRoute(coordinates = coordinates)

            if (response.code != "Ok" || response.routes.isEmpty()) {
                return AppResult.Error("Không tìm thấy tuyến đường")
            }

            val route = response.routes.first()
            val points = route.geometry.coordinates.map { coord ->
                // GeoJSON: [lng, lat]
                GeoPoint(coord[1], coord[0])
            }

            AppResult.Success(
                RouteInfo(
                    points = points,
                    distanceKm = route.distance / 1000.0,
                    durationMinutes = route.duration / 60.0
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi tìm tuyến đường")
        }
    }
}
