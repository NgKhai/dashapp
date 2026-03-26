package com.example.driverdashapp.data.remote.interceptor

import com.example.driverdashapp.data.local.TokenManager
import com.example.driverdashapp.data.remote.api.AuthApi
import com.example.driverdashapp.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * OkHttp Authenticator that automatically refreshes expired tokens.
 *
 * Uses a Mutex to prevent concurrent refresh attempts — if two 401s arrive
 * simultaneously (e.g. parallel API calls), only the first one actually
 * calls /auth/refresh. The second one just picks up the freshly saved token.
 */
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiProvider: dagger.Lazy<AuthApi>
) : Authenticator {

    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops
        if (response.request.header("X-Retry-Auth") != null) {
            runBlocking { tokenManager.clearAll() }
            return null
        }

        // Don't try to refresh for auth endpoints (would fail and clear tokens unnecessarily)
        val path = response.request.url.encodedPath
        if (path.contains("/login") || path.contains("/register") ||
            path.contains("/verify-otp") || path.contains("/refresh")) {
            return null
        }

        return runBlocking {
            refreshMutex.withLock {
                // After acquiring the lock, check if another thread already refreshed
                // by comparing the token that failed with the current stored token.
                val failedToken = response.request.header("Authorization")
                    ?.removePrefix("Bearer ")
                val currentToken = tokenManager.getAccessToken()

                if (currentToken != null && currentToken != failedToken) {
                    // Another thread already refreshed — just retry with the new token
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .header("X-Retry-Auth", "true")
                        .build()
                }

                // We need to actually refresh
                val refreshToken = tokenManager.getRefreshToken() ?: run {
                    tokenManager.clearAll()
                    return@withLock null
                }

                try {
                    val refreshResponse = authApiProvider.get().refreshToken(
                        RefreshTokenRequest(refreshToken)
                    )

                    if (refreshResponse.isSuccessful && refreshResponse.body()?.success == true) {
                        val session = refreshResponse.body()?.data
                        if (session != null) {
                            tokenManager.saveTokens(session.accessToken, session.refreshToken)

                            response.request.newBuilder()
                                .header("Authorization", "Bearer ${session.accessToken}")
                                .header("X-Retry-Auth", "true")
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
}
