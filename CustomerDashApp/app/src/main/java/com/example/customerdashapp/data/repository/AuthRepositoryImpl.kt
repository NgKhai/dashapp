package com.example.customerdashapp.data.repository

import com.example.customerdashapp.data.local.TokenManager
import com.example.customerdashapp.data.remote.api.AuthApi
import com.example.customerdashapp.data.remote.dto.*
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.model.Customer
import com.example.customerdashapp.domain.model.LoginResponse
import com.example.customerdashapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun register(phone: String, name: String): AppResult<String> {
        return try {
            val response = authApi.register(RegisterRequest(phone, name))
            if (response.isSuccessful && response.body()?.success == true) {
                AppResult.Success(response.body()?.message ?: "OTP đã được gửi")
            } else {
                AppResult.Error(response.body()?.message ?: "Đăng ký thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối mạng")
        }
    }

    /**
     * Unified login - handles both PIN check and PIN login in a single call.
     *
     * Backend responses:
     * - require_pin = true → user has PIN, ask for it
     * - require_otp = true → user has no PIN, OTP was sent
     * - customer != null → PIN login successful
     *
     * NOTE: This method has multi-branch response logic, so we keep
     * the try-catch here rather than using safeApiCall.
     */
    override suspend fun login(phone: String, pin: String?): AppResult<LoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(phone, pin))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data

                when {
                    data?.requirePin == true -> {
                        AppResult.Success(LoginResponse.RequirePin)
                    }
                    data?.requireOtp == true -> {
                        AppResult.Success(LoginResponse.RequireOtp(data.phone ?: phone))
                    }
                    data?.customer != null -> {
                        val customerData = data.customer

                        // Save session tokens if available
                        data.session?.let { session ->
                            tokenManager.saveTokens(session.accessToken, session.refreshToken)
                        }

                        tokenManager.saveUserInfo(
                            phone = customerData.phone,
                            name = customerData.name,
                            userId = data.authUserId,
                            customerId = customerData.customerId
                        )

                        AppResult.Success(LoginResponse.Success(customerData.toDomain()))
                    }
                    else -> {
                        AppResult.Error(response.body()?.message ?: "Đăng nhập thất bại")
                    }
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Đăng nhập thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối mạng")
        }
    }

    /**
     * NOTE: verifyOtp has side-effects (saving tokens/user info) that
     * depend on nested response fields, so we keep the try-catch here.
     */
    override suspend fun verifyOtp(phone: String, otp: String, name: String?): AppResult<Customer> {
        return try {
            val response = authApi.verifyOtp(VerifyOtpRequest(phone, otp, name))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data

                data?.session?.let { session ->
                    tokenManager.saveTokens(session.accessToken, session.refreshToken)
                }

                val customerData = data?.customer
                if (customerData != null) {
                    tokenManager.saveUserInfo(
                        phone = customerData.phone,
                        name = customerData.name,
                        userId = data.user?.id,
                        customerId = customerData.customerId
                    )
                    AppResult.Success(customerData.toDomain())
                } else {
                    AppResult.Error("Không thể lấy thông tin người dùng")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Xác thực OTP thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối mạng")
        }
    }

    override suspend fun setPin(pin: String, name: String?): AppResult<Unit> {
        return try {
            val response = authApi.setPin(SetPinRequest(pin, "customer", name))
            if (response.isSuccessful && response.body()?.success == true) {
                // Update name locally if the backend returned it
                response.body()?.data?.name?.let { updatedName ->
                    val currentPhone = tokenManager.userPhone.toString()
                    tokenManager.saveUserInfo(currentPhone, updatedName, null, null)
                }
                AppResult.Success(Unit)
            } else {
                AppResult.Error(response.body()?.message ?: "Đặt mã PIN thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối mạng")
        }
    }

    override suspend fun logout() {
        tokenManager.clearAll()
    }

    override suspend fun getCustomerName(): String? {
        return tokenManager.getCustomerName()
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return tokenManager.accessToken.map { it != null }
    }

    override fun getCurrentUser(): Flow<Customer?> {
        return tokenManager.userName.map { name ->
            if (name != null) {
                Customer(customerId = "", name = name, phone = "")
            } else null
        }
    }
}
