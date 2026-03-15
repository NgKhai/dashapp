package com.example.driverdashapp.data.repository

import android.util.Log
import com.example.driverdashapp.data.remote.api.DeliveryApi
import com.example.driverdashapp.data.remote.api.DriverApi
import com.example.driverdashapp.data.remote.api.OsrmApi
import com.example.driverdashapp.data.remote.dto.*
import com.example.driverdashapp.domain.model.*
import com.example.driverdashapp.domain.repository.DriverRepository
import com.google.gson.Gson
import retrofit2.Response
import javax.inject.Inject

class DriverRepositoryImpl @Inject constructor(
    private val driverApi: DriverApi,
    private val deliveryApi: DeliveryApi,
    private val osrmApi: OsrmApi
) : DriverRepository {

    override suspend fun getProfile(): AppResult<Driver> = apiCall {
        driverApi.getProfile().dataOrThrow().toDomain()
    }

    override suspend fun updateProfile(name: String?, email: String?): AppResult<Driver> = apiCall {
        driverApi.updateProfile(UpdateProfileRequest(name = name, email = email)).dataOrThrow().toDomain()
    }

    override suspend fun updateStatus(isOnline: Boolean): AppResult<Driver> = apiCall {
        driverApi.updateStatus(UpdateStatusRequest(isOnline)).dataOrThrow().toDomain()
    }

    override suspend fun updateLocation(lat: Double, lng: Double, deliveryId: String?): AppResult<Unit> = apiCall {
        driverApi.updateLocation(UpdateLocationRequest(lat, lng, deliveryId))
    }

    override suspend fun getVehicles(): AppResult<List<VehicleAssignment>> = apiCall {
        driverApi.getVehicles().dataOrThrow().map { it.toDomain() }
    }

    override suspend fun setPrimaryVehicle(assignmentId: String): AppResult<VehicleAssignment> = apiCall {
        driverApi.setPrimaryVehicle(assignmentId).dataOrThrow().toDomain()
    }

    override suspend fun getPendingDeliveries(limit: Int): AppResult<List<Delivery>> = apiCall {
        driverApi.getPendingDeliveries(limit).dataOrThrow().map { it.toDomain() }
    }

    override suspend fun getMyDeliveries(status: String?, limit: Int, offset: Int): AppResult<List<Delivery>> = apiCall {
        driverApi.getMyDeliveries(status, limit, offset).dataOrThrow().map { it.toDomain() }
    }

    override suspend fun getEarnings(): AppResult<Earnings> = apiCall {
        driverApi.getEarnings().dataOrThrow().toDomain()
    }

    override suspend fun getDelivery(deliveryId: String): AppResult<Delivery> = apiCall {
        deliveryApi.getDelivery(deliveryId).dataOrThrow().toDomain()
    }

    override suspend fun acceptDelivery(deliveryId: String): AppResult<Delivery> = apiCall {
        deliveryApi.acceptDelivery(deliveryId).dataOrThrow().toDomain()
    }

    override suspend fun pickupDelivery(deliveryId: String): AppResult<Delivery> = apiCall {
        deliveryApi.pickupDelivery(deliveryId).dataOrThrow().toDomain()
    }

    override suspend fun startDelivering(deliveryId: String): AppResult<Delivery> = apiCall {
        deliveryApi.deliveringDelivery(deliveryId).dataOrThrow().toDomain()
    }

    override suspend fun completeDelivery(deliveryId: String): AppResult<Delivery> = apiCall {
        deliveryApi.completeDelivery(deliveryId).dataOrThrow().toDomain()
    }

    override suspend fun cancelDelivery(deliveryId: String, reason: String?): AppResult<Delivery> = apiCall {
        deliveryApi.cancelDelivery(deliveryId, CancelDeliveryRequest(reason)).dataOrThrow().toDomain()
    }

    override suspend fun rateCustomer(deliveryId: String, rating: Int, review: String?): AppResult<Unit> = apiCall {
        deliveryApi.rateDelivery(deliveryId, RateDeliveryRequest(rating, review))
    }

    override suspend fun getRoute(
        startLat: Double, startLng: Double, endLat: Double, endLng: Double
    ): AppResult<RouteInfo> = apiCall {
        // OSRM expects coordinates as lng,lat;lng,lat
        val coordinates = "$startLng,$startLat;$endLng,$endLat"
        val response = osrmApi.getRoute(coordinates = coordinates)

        if (response.code != "Ok" || response.routes.isEmpty()) {
            throw Exception("Không tìm thấy tuyến đường")
        }

        val route = response.routes.first()
        val points = route.geometry.coordinates.map { coord ->
            // GeoJSON: [lng, lat]
            LatLng(coord[1], coord[0])
        }

        RouteInfo(
            points = points,
            distanceKm = route.distance / 1000.0,
            durationMinutes = route.duration / 60.0
        )
    }

    private inline fun <T> apiCall(block: () -> T): AppResult<T> {
        return try {
            AppResult.Success(block())
        } catch (e: Exception) {
            Log.e("DriverRepo", "API call failed: ${e.message}", e)
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
    }

    /** Extract body or throw with server error message */
    private fun <T> Response<ApiResponse<T>>.dataOrThrow(): T {
        if (!isSuccessful) {
            // Try to parse error body for message
            val errMsg = try {
                val errJson = errorBody()?.string()
                Gson().fromJson(errJson, ApiResponse::class.java)?.message
            } catch (_: Exception) { null }
            throw Exception(errMsg ?: "Lỗi server: HTTP ${code()}")
        }
        val body = body() ?: throw Exception("Empty response body")
        if (!body.success) throw Exception(body.message ?: "Request failed")
        return body.data ?: throw Exception("No data in response")
    }
}
