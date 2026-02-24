package com.example.driverdashapp.data.repository

import com.example.driverdashapp.data.local.TokenManager
import com.example.driverdashapp.data.remote.api.AuthApi
import com.example.driverdashapp.data.remote.dto.*
import com.example.driverdashapp.domain.model.*
import com.example.driverdashapp.domain.repository.AuthRepository
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

    override suspend fun login(phone: String, pin: String?): AppResult<LoginResponse> {
        return try {
            val response = authApi.login(LoginRequest(phone, pin))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                when {
                    data?.requirePin == true -> AppResult.Success(LoginResponse.RequirePin)
                    data?.requireOtp == true -> AppResult.Success(LoginResponse.RequireOtp(phone))
                    data?.driver != null -> {
                        // PIN login success — generate token on backend
                        data.session?.let {
                            tokenManager.saveTokens(it.accessToken, it.refreshToken)
                        }
                        val d = data.driver
                        tokenManager.saveDriverInfo(d.name, d.driverId)
                        AppResult.Success(LoginResponse.Success(
                            Driver(d.driverId, d.name, d.phone, d.email, d.isVerified, d.isOnline, d.rating)
                        ))
                    }
                    else -> AppResult.Error("Phản hồi không hợp lệ")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Đăng nhập thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối mạng")
        }
    }

    override suspend fun verifyOtp(phone: String, otp: String, name: String?): AppResult<Driver> {
        return try {
            val response = authApi.verifyOtp(VerifyOtpRequest(phone, otp, name))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data
                data?.session?.let { tokenManager.saveTokens(it.accessToken, it.refreshToken) }
                val d = data?.driver
                if (d != null) {
                    tokenManager.saveDriverInfo(d.name, d.driverId)
                    AppResult.Success(Driver(d.driverId, d.name, d.phone, d.email, d.isVerified, d.isOnline, d.rating))
                } else {
                    AppResult.Error("Không tìm thấy thông tin tài xế")
                }
            } else {
                AppResult.Error(response.body()?.message ?: "Xác thực thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối mạng")
        }
    }

    override suspend fun setPin(pin: String, name: String?): AppResult<Unit> {
        return try {
            val response = authApi.setPin(SetPinRequest(pin = pin, userType = "driver", name = name))
            if (response.isSuccessful && response.body()?.success == true) {
                AppResult.Success(Unit)
            } else {
                AppResult.Error(response.body()?.message ?: "Đặt PIN thất bại")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối mạng")
        }
    }

    override suspend fun logout() {
        tokenManager.clearAll()
    }

    override fun isLoggedIn(): Flow<Boolean> = tokenManager.accessToken.map { it != null }

    override suspend fun getDriverName(): String? = tokenManager.getDriverName()
}
