package com.example.customerdashapp.data.repository

import com.example.customerdashapp.data.remote.api.DeliveryApi
import com.example.customerdashapp.data.remote.dto.*
import com.example.customerdashapp.data.remote.safeApiCall
import com.example.customerdashapp.data.remote.safeApiCallUnit
import com.example.customerdashapp.data.remote.mapSuccess
import com.example.customerdashapp.domain.model.*
import com.example.customerdashapp.domain.repository.DeliveryRepository
import javax.inject.Inject

class DeliveryRepositoryImpl @Inject constructor(
    private val deliveryApi: DeliveryApi
) : DeliveryRepository {

    override suspend fun createDelivery(params: CreateDeliveryParams): AppResult<Delivery> {
        return safeApiCall(errorMessage = "Không thể tạo đơn giao hàng") {
            deliveryApi.createDelivery(
                CreateDeliveryRequest(
                    pickupAddress = params.pickupAddress,
                    pickupLat = params.pickupLat,
                    pickupLng = params.pickupLng,
                    dropOffAddress = params.dropOffAddress,
                    dropOffLat = params.dropOffLat,
                    dropOffLng = params.dropOffLng,
                    vehicleType = params.vehicleType,
                    notes = params.notes,
                    items = params.items,
                    itemsPhotoUrl = params.itemsPhotoUrls?.let {
                        if (it.isNotEmpty()) com.google.gson.Gson().toJson(it) else null
                    },
                    requiresLoadingHelp = params.requiresLoadingHelp,
                    routeEncoded = params.routeEncoded,
                    distanceKm = params.distanceKm
                )
            )
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun getDelivery(deliveryId: String): AppResult<Delivery> {
        return safeApiCall(errorMessage = "Không tìm thấy đơn hàng") {
            deliveryApi.getDelivery(deliveryId)
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun trackDelivery(deliveryId: String): AppResult<TrackingInfo> {
        return safeApiCall(errorMessage = "Không có dữ liệu theo dõi") {
            deliveryApi.trackDelivery(deliveryId)
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun getMyDeliveries(
        status: String?,
        limit: Int,
        offset: Int
    ): AppResult<List<Delivery>> {
        return try {
            val response = deliveryApi.getMyDeliveries(status, limit, offset)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data ?: emptyList()
                AppResult.Success(data.map { it.toDomain() })
            } else {
                AppResult.Error(response.body()?.message ?: "Lấy danh sách thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
    }

    override suspend fun cancelDelivery(
        deliveryId: String,
        reason: String?
    ): AppResult<Delivery> {
        return safeApiCall(errorMessage = "Hủy đơn thất bại") {
            deliveryApi.cancelDelivery(deliveryId, CancelDeliveryRequest(reason))
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun rateDelivery(
        deliveryId: String,
        rating: Int,
        review: String?
    ): AppResult<Unit> {
        return safeApiCallUnit(errorMessage = "Đánh giá thất bại") {
            deliveryApi.rateDelivery(deliveryId, RateDeliveryRequest(rating, review))
        }
    }

    override suspend fun getAddresses(): AppResult<List<Address>> {
        return try {
            val response = deliveryApi.getAddresses()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data ?: emptyList()
                AppResult.Success(data.map { it.toDomain() })
            } else {
                AppResult.Error(response.body()?.message ?: "Lấy địa chỉ thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
    }

    override suspend fun addAddress(
        label: String,
        address: String,
        lat: Double,
        lng: Double,
        isDefault: Boolean
    ): AppResult<Address> {
        return safeApiCall(errorMessage = "Thêm địa chỉ thất bại") {
            deliveryApi.addAddress(AddAddressRequest(label, address, lat, lng, isDefault))
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun getPricing(): AppResult<List<PricingConfig>> {
        return safeApiCall(errorMessage = "Lấy giá thất bại") {
            deliveryApi.getPricing()
        }.mapSuccess { list -> list.map { it.toDomain() } }
    }
}
