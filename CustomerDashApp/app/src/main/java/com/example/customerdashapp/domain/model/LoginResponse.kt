package com.example.customerdashapp.domain.model

/**
 * Represents the result of the unified login call.
 * Maps directly to the 3 possible backend responses.
 */
sealed class LoginResponse {
    /** User has a PIN set - frontend should show PIN input */
    data object RequirePin : LoginResponse()
    
    /** User has no PIN - OTP was sent, frontend should show OTP input */
    data class RequireOtp(val phone: String) : LoginResponse()
    
    /** PIN was correct - user is logged in */
    data class Success(val customer: Customer) : LoginResponse()
}
