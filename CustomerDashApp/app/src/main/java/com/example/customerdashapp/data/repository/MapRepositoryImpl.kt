package com.example.customerdashapp.data.repository

import com.example.customerdashapp.data.remote.api.NominatimApi
import com.example.customerdashapp.data.remote.api.RouteApi
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.RouteInfo
import com.example.customerdashapp.domain.model.SearchResult
import com.example.customerdashapp.domain.repository.MapRepository
import com.example.customerdashapp.util.PolylineDecoder
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(
    private val nominatimApi: NominatimApi,
    private val routeApi: RouteApi
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
            val response = routeApi.getRoute(
                pickupLat  = startLat,
                pickupLng  = startLng,
                dropoffLat = endLat,
                dropoffLng = endLng
            )

            val data = response.data
                ?: return AppResult.Error("Không tìm thấy tuyến đường")

            val points: List<GeoPoint> = if (!data.routeEncoded.isNullOrEmpty()) {
                PolylineDecoder.decode(data.routeEncoded)
                    .takeIf { it.isNotEmpty() }
                    ?: listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng))
            } else {
                listOf(GeoPoint(startLat, startLng), GeoPoint(endLat, endLng))
            }

            AppResult.Success(
                RouteInfo(
                    points          = points,
                    distanceKm      = data.distanceKm,
                    durationMinutes = data.durationMinutes,
                    routeEncoded    = data.routeEncoded
                )
            )
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi tìm tuyến đường")
        }
    }
}
