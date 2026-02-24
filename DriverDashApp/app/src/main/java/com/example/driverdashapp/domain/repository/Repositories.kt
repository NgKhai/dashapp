package com.example.driverdashapp.domain.repository

import com.example.driverdashapp.domain.model.*
import com.example.driverdashapp.domain.model.RouteInfo
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(phone: String, name: String): AppResult<String>
    suspend fun login(phone: String, pin: String? = null): AppResult<LoginResponse>
    suspend fun verifyOtp(phone: String, otp: String, name: String?): AppResult<Driver>
    suspend fun setPin(pin: String, name: String?): AppResult<Unit>
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
    suspend fun getDriverName(): String?
}

interface DriverRepository {
    suspend fun getProfile(): AppResult<Driver>
    suspend fun updateProfile(name: String?, email: String?): AppResult<Driver>
    suspend fun updateStatus(isOnline: Boolean): AppResult<Driver>
    suspend fun updateLocation(lat: Double, lng: Double, deliveryId: String? = null): AppResult<Unit>
    suspend fun getVehicles(): AppResult<List<VehicleAssignment>>
    suspend fun setPrimaryVehicle(assignmentId: String): AppResult<VehicleAssignment>
    suspend fun getPendingDeliveries(limit: Int = 10): AppResult<List<Delivery>>
    suspend fun getMyDeliveries(status: String? = null, limit: Int = 20, offset: Int = 0): AppResult<List<Delivery>>
    suspend fun getEarnings(): AppResult<Earnings>
    suspend fun getDelivery(deliveryId: String): AppResult<Delivery>
    suspend fun acceptDelivery(deliveryId: String): AppResult<Delivery>
    suspend fun pickupDelivery(deliveryId: String): AppResult<Delivery>
    suspend fun startDelivering(deliveryId: String): AppResult<Delivery>
    suspend fun completeDelivery(deliveryId: String): AppResult<Delivery>
    suspend fun cancelDelivery(deliveryId: String, reason: String? = null): AppResult<Delivery>
    suspend fun rateCustomer(deliveryId: String, rating: Int, review: String? = null): AppResult<Unit>
    suspend fun getRoute(startLat: Double, startLng: Double, endLat: Double, endLng: Double): AppResult<RouteInfo>
}
