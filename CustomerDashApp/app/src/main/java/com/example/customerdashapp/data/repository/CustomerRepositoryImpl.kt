package com.example.customerdashapp.data.repository

import com.example.customerdashapp.data.remote.api.CustomerApi
import com.example.customerdashapp.data.remote.dto.UpdateProfileRequest
import com.example.customerdashapp.data.remote.dto.toDomain
import com.example.customerdashapp.data.remote.mapSuccess
import com.example.customerdashapp.data.remote.safeApiCall
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.Customer
import com.example.customerdashapp.domain.repository.CustomerRepository
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val customerApi: CustomerApi
) : CustomerRepository {

    override suspend fun getProfile(): AppResult<Customer> {
        return safeApiCall(errorMessage = "Không có dữ liệu hồ sơ") {
            customerApi.getProfile()
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun updateProfile(name: String?, email: String?): AppResult<Customer> {
        return safeApiCall(errorMessage = "Cập nhật thất bại") {
            customerApi.updateProfile(UpdateProfileRequest(name = name, email = email))
        }.mapSuccess { it.toDomain() }
    }
}
