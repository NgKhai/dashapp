package com.example.driverdashapp.data.repository

import com.example.driverdashapp.data.local.TokenManager
import com.example.driverdashapp.data.remote.api.AuthApi
import com.example.driverdashapp.data.remote.dto.*
import com.example.driverdashapp.domain.model.*
import com.example.driverdashapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Response
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun register(phone: String, name: String): AppResult<String> = safeApiCall {
        val response = authApi.register(RegisterRequest(phone, name))
        response.successOrThrow()
        response.body()?.message ?: "OTP đã được gửi"
    }

    override suspend fun login(phone: String, pin: String?): AppResult<LoginResponse> = safeApiCall {
        val response = authApi.login(LoginRequest(phone, pin))
        response.successOrThrow()
        val data = response.body()?.data ?: throw Exception("Phản hồi không hợp lệ")
        when {
            data.requirePin == true -> LoginResponse.RequirePin
            data.requireOtp == true -> LoginResponse.RequireOtp(phone)
            data.driver != null -> {
                data.session?.let { tokenManager.saveTokens(it.accessToken, it.refreshToken) }
                val d = data.driver
                tokenManager.saveDriverInfo(d.name, d.driverId)
                LoginResponse.Success(
                    Driver(d.driverId, d.name, d.phone, d.email, d.isVerified, d.isOnline, d.rating)
                )
            }
            else -> throw Exception("Phản hồi không hợp lệ")
        }
    }

    override suspend fun verifyOtp(phone: String, otp: String, name: String?): AppResult<Driver> = safeApiCall {
        val response = authApi.verifyOtp(VerifyOtpRequest(phone, otp, name))
        response.successOrThrow()
        val data = response.body()?.data
        data?.session?.let { tokenManager.saveTokens(it.accessToken, it.refreshToken) }
        val d = data?.driver ?: throw Exception("Không tìm thấy thông tin tài xế")
        tokenManager.saveDriverInfo(d.name, d.driverId)
        Driver(d.driverId, d.name, d.phone, d.email, d.isVerified, d.isOnline, d.rating)
    }

    override suspend fun setPin(pin: String, name: String?): AppResult<Unit> = safeApiCall {
        val response = authApi.setPin(SetPinRequest(pin = pin, userType = "driver", name = name))
        response.successOrThrow()
    }

    override suspend fun logout() {
        tokenManager.clearAll()
    }

    override fun isLoggedIn(): Flow<Boolean> = tokenManager.accessToken.map { it != null }

    override suspend fun getDriverName(): String? = tokenManager.getDriverName()

    // ── Helpers ──────────────────────────────────────────────────────────

    private inline fun <T> safeApiCall(block: () -> T): AppResult<T> {
        return try {
            AppResult.Success(block())
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi kết nối mạng")
        }
    }

    /** Throws if the response indicates failure */
    private fun <T> Response<ApiResponse<T>>.successOrThrow() {
        if (!isSuccessful || body()?.success != true) {
            throw Exception(body()?.message ?: "Thao tác thất bại")
        }
    }
}
