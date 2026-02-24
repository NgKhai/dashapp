package com.example.customerdashapp.data.repository

import com.example.customerdashapp.data.remote.api.CustomerApi
import com.example.customerdashapp.data.remote.dto.UpdateProfileRequest
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.Customer
import com.example.customerdashapp.domain.repository.CustomerRepository
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val customerApi: CustomerApi
) : CustomerRepository {

    override suspend fun getProfile(): AppResult<Customer> {
        return try {
            val response = customerApi.getProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    AppResult.Success(
                        Customer(
                            customerId = data.customerId,
                            name = data.name,
                            phone = data.phone ?: "",
                            email = data.email,
                            avatarUrl = data.avatarUrl
                        )
                    )
                } else {
                    AppResult.Error("Không có dữ liệu hồ sơ")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Tải hồ sơ thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
    }

    override suspend fun updateProfile(name: String?, email: String?): AppResult<Customer> {
        return try {
            val response = customerApi.updateProfile(UpdateProfileRequest(name = name, email = email))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                if (data != null) {
                    AppResult.Success(
                        Customer(
                            customerId = data.customerId,
                            name = data.name,
                            phone = data.phone ?: "",
                            email = data.email,
                            avatarUrl = data.avatarUrl
                        )
                    )
                } else {
                    AppResult.Error("Không có dữ liệu hồ sơ")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Cập nhật thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối")
        }
    }
}
