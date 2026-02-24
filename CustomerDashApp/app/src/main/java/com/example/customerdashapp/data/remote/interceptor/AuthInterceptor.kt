package com.example.customerdashapp.data.remote.interceptor

import com.example.customerdashapp.BuildConfig
import com.example.customerdashapp.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val requestBuilder = originalRequest.newBuilder()
            // Always add Vercel deployment protection bypass header
            .header("x-vercel-protection-bypass", BuildConfig.VERCEL_BYPASS_SECRET)
        
        // Skip adding auth token for login/register/verify-otp endpoints
        val path = originalRequest.url.encodedPath
        if (!path.contains("/login") && !path.contains("/register") && !path.contains("/verify-otp")) {
            val token = runBlocking { tokenManager.getAccessToken() }
            if (token != null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
