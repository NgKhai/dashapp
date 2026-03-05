package com.example.customerdashapp.data.remote

import com.example.customerdashapp.data.remote.dto.ApiResponse
import com.example.customerdashapp.domain.model.AppResult
import retrofit2.Response

/**
 * Wraps a Retrofit call with standardized error handling.
 *
 * Eliminates the repeated try-catch / isSuccessful / .body()?.success pattern
 * found in every repository method.
 *
 * @param errorMessage Fallback message when the API returns no message.
 * @param call The suspend lambda that makes the Retrofit call.
 * @return [AppResult.Success] with the extracted [T], or [AppResult.Error].
 */
suspend fun <T> safeApiCall(
    errorMessage: String = "Request failed",
    call: suspend () -> Response<ApiResponse<T>>
): AppResult<T> {
    return try {
        val response = call()
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            val data = body.data
            if (data != null) {
                AppResult.Success(data)
            } else {
                AppResult.Error(body.message.ifBlank { errorMessage })
            }
        } else {
            AppResult.Error(body?.message ?: errorMessage)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: errorMessage)
    }
}

/**
 * Maps the data inside [AppResult.Success] while preserving [AppResult.Error].
 * Enables chaining: `safeApiCall { ... }.mapSuccess { it.toDomain() }`
 */
inline fun <T, R> AppResult<T>.mapSuccess(transform: (T) -> R): AppResult<R> {
    return when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Error -> this
        is AppResult.Loading -> AppResult.Loading
    }
}

/**
 * Variant for API calls whose response data is [Unit] (e.g. rate delivery).
 * Ignores the data field and returns [AppResult.Success] with [Unit].
 */
suspend fun <T> safeApiCallUnit(
    errorMessage: String = "Request failed",
    call: suspend () -> Response<ApiResponse<T>>
): AppResult<Unit> {
    return try {
        val response = call()
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            AppResult.Success(Unit)
        } else {
            AppResult.Error(body?.message ?: errorMessage)
        }
    } catch (e: Exception) {
        AppResult.Error(e.message ?: errorMessage)
    }
}
