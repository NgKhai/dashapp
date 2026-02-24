package com.example.customerdashapp.data.remote.interceptor

import com.example.customerdashapp.BuildConfig
import com.example.customerdashapp.data.local.TokenManager
import com.example.customerdashapp.data.remote.dto.RefreshTokenRequest
import com.example.customerdashapp.data.remote.api.AuthApi
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * OkHttp Authenticator that automatically refreshes expired tokens.
 * When a 401 is received:
 * 1. Read refresh token from DataStore
 * 2. Call /auth/refresh
 * 3. Save new tokens and retry the original request
 * 4. If refresh fails → clear all tokens (triggers auto-logout)
 */
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiProvider: dagger.Lazy<AuthApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if already attempted refresh (avoid infinite loop)
        if (response.request.header("X-Token-Refreshed") != null) {
            // Refresh already attempted and still got 401 → clear tokens
            runBlocking { tokenManager.clearAll() }
            return null
        }

        // Don't try to refresh for auth endpoints
        val path = response.request.url.encodedPath
        if (path.contains("/login") || path.contains("/register") ||
            path.contains("/verify-otp") || path.contains("/refresh")) {
            return null
        }

        return runBlocking {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                tokenManager.clearAll()
                return@runBlocking null
            }

            try {
                val refreshResponse = authApiProvider.get().refreshToken(
                    RefreshTokenRequest(refreshToken)
                )

                if (refreshResponse.isSuccessful && refreshResponse.body()?.success == true) {
                    val session = refreshResponse.body()?.data
                    if (session != null) {
                        tokenManager.saveTokens(session.accessToken, session.refreshToken)

                        // Retry the original request with the new token
                        response.request.newBuilder()
                            .header("Authorization", "Bearer ${session.accessToken}")
                            .header("X-Token-Refreshed", "true")
                            .build()
                    } else {
                        tokenManager.clearAll()
                        null
                    }
                } else {
                    tokenManager.clearAll()
                    null
                }
            } catch (e: Exception) {
                tokenManager.clearAll()
                null
            }
        }
    }
}
