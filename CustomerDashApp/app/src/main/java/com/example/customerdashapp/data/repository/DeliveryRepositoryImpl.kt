package com.example.customerdashapp.data.repository

import com.example.customerdashapp.data.remote.api.DeliveryApi
import com.example.customerdashapp.data.remote.dto.*
import com.example.customerdashapp.domain.model.*
import com.example.customerdashapp.domain.repository.DeliveryRepository
import javax.inject.Inject

class DeliveryRepositoryImpl @Inject constructor(
    private val deliveryApi: DeliveryApi
) : DeliveryRepository {

    override suspend fun createDelivery(params: CreateDeliveryParams): AppResult<Delivery> {
        return try {
            val response = deliveryApi.createDelivery(
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
                    requiresLoadingHelp = params.requiresLoadingHelp
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    AppResult.Success(data.toDomain())
                } else {
                    AppResult.Error("Không thể tạo đơn giao hàng")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Tạo đơn thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
    }

    override suspend fun getDelivery(deliveryId: String): AppResult<Delivery> {
        return try {
            val response = deliveryApi.getDelivery(deliveryId)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    AppResult.Success(data.toDomain())
                } else {
                    AppResult.Error("Không tìm thấy đơn hàng")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Lấy thông tin thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
    }

    override suspend fun trackDelivery(deliveryId: String): AppResult<TrackingInfo> {
        return try {
            val response = deliveryApi.trackDelivery(deliveryId)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    AppResult.Success(data.toDomain())
                } else {
                    AppResult.Error("Không có dữ liệu theo dõi")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Theo dõi thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
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
        return try {
            val response = deliveryApi.cancelDelivery(
                deliveryId,
                CancelDeliveryRequest(reason)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    AppResult.Success(data.toDomain())
                } else {
                    AppResult.Error("Không thể hủy đơn")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Hủy đơn thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
    }

    override suspend fun rateDelivery(
        deliveryId: String,
        rating: Int,
        review: String?
    ): AppResult<Unit> {
        return try {
            val response = deliveryApi.rateDelivery(
                deliveryId,
                RateDeliveryRequest(rating, review)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error(response.body()?.message ?: "Đánh giá thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
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
        return try {
            val response = deliveryApi.addAddress(
                AddAddressRequest(label, address, lat, lng, isDefault)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    AppResult.Success(data.toDomain())
                } else {
                    AppResult.Error("Không thể thêm địa chỉ")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Thêm địa chỉ thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
    }
}
