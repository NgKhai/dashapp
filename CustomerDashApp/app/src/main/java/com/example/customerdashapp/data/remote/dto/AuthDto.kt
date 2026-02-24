package com.example.customerdashapp.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============================================
// REQUEST DTOs
// ============================================

data class RegisterRequest(
    val phone: String,
    val name: String,
    val email: String? = null
)

/**
 * Unified login request - used for both PIN login and OTP request.
 * Send phone only → backend checks PIN status and returns require_pin or require_otp.
 * Send phone + pin → backend verifies PIN and logs in.
 */
data class LoginRequest(
    val phone: String,
    val pin: String? = null
)

data class VerifyOtpRequest(
    val phone: String,
    val otp: String,
    val name: String? = null,
    val email: String? = null
)

data class SetPinRequest(
    val pin: String,
    @SerializedName("user_type")
    val userType: String = "customer",
    val name: String? = null
)

// ============================================
// RESPONSE DTOs
// ============================================

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

/**
 * Unified login response - covers all 3 cases:
 * 1. require_pin = true → user has PIN, ask for it
 * 2. require_otp = true → user has no PIN, OTP was sent
 * 3. customer != null → PIN login successful
 */
data class LoginResponseData(
    @SerializedName("require_pin")
    val requirePin: Boolean? = null,
    @SerializedName("require_otp")
    val requireOtp: Boolean? = null,
    val phone: String? = null,
    val customer: CustomerData? = null,
    @SerializedName("auth_user_id")
    val authUserId: String? = null,
    val session: SessionData? = null,
    val message: String? = null
)

data class VerifyOtpData(
    val user: UserData? = null,
    val customer: CustomerData? = null,
    val session: SessionData? = null
)

data class UserData(
    val id: String,
    val phone: String
)

data class CustomerData(
    @SerializedName("customer_id")
    val customerId: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null
)

data class SessionData(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("expires_at")
    val expiresAt: Long? = null
)

data class SetPinData(
    val name: String? = null
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)
