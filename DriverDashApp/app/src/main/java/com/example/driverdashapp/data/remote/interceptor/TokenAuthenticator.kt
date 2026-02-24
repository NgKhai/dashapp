package com.example.driverdashapp.data.remote.interceptor

import com.example.driverdashapp.data.local.TokenManager
import com.example.driverdashapp.data.remote.api.AuthApi
import com.example.driverdashapp.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiProvider: dagger.Lazy<AuthApi>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loops
        if (response.request.header("X-Retry-Auth") != null) {
            return null
        }

        return runBlocking {
            val refreshToken = tokenManager.getRefreshToken() ?: run {
                tokenManager.clearAll()
                return@runBlocking null
            }

            try {
                val refreshResponse = authApiProvider.get().refreshToken(
                    RefreshTokenRequest(refreshToken)
                )

                if (refreshResponse.isSuccessful && refreshResponse.body()?.success == true) {
                    val session = refreshResponse.body()?.data!!
                    tokenManager.saveTokens(session.accessToken, session.refreshToken)

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${session.accessToken}")
                        .header("X-Retry-Auth", "true")
                        .build()
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
