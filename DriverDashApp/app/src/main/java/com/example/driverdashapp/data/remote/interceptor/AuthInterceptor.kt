package com.example.driverdashapp.data.remote.interceptor

import com.example.driverdashapp.BuildConfig
import com.example.driverdashapp.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()

        // Add Vercel bypass header
        request.addHeader("x-vercel-protection-bypass", BuildConfig.VERCEL_BYPASS_SECRET)

        // Add auth token if available
        val token = runBlocking { tokenManager.getAccessToken() }
        if (token != null) {
            request.addHeader("Authorization", "Bearer $token")
        }

        return chain.proceed(request.build())
    }
}
