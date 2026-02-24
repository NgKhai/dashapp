package com.example.customerdashapp.domain.repository

import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.Customer
import com.example.customerdashapp.domain.model.LoginResponse
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    
    suspend fun register(phone: String, name: String): AppResult<String>
    
    /**
     * Unified login - handles both PIN check and PIN login.
     * @param phone User's phone number
     * @param pin Optional PIN. If null, backend checks PIN status. If provided, backend verifies PIN.
     * @return LoginResponse indicating next step (RequirePin, RequireOtp, or Success)
     */
    suspend fun login(phone: String, pin: String? = null): AppResult<LoginResponse>
    
    suspend fun verifyOtp(phone: String, otp: String, name: String?): AppResult<Customer>
    
    suspend fun setPin(pin: String, name: String?): AppResult<Unit>
    
    suspend fun logout()
    
    fun isLoggedIn(): Flow<Boolean>

    suspend fun getCustomerName(): String?
    
    fun getCurrentUser(): Flow<Customer?>
}
