package com.example.customerdashapp.domain.repository

import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.Customer

interface CustomerRepository {
    suspend fun getProfile(): AppResult<Customer>
    suspend fun updateProfile(name: String?, email: String?): AppResult<Customer>
}
