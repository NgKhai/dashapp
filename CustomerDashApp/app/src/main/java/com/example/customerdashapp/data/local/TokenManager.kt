package com.example.customerdashapp.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val CUSTOMER_ID_KEY = stringPreferencesKey("customer_id")
    }

    val accessToken: Flow<String?> = dataStore.data.map { it[ACCESS_TOKEN_KEY] }
    val refreshToken: Flow<String?> = dataStore.data.map { it[REFRESH_TOKEN_KEY] }
    val userPhone: Flow<String?> = dataStore.data.map { it[USER_PHONE_KEY] }
    val userName: Flow<String?> = dataStore.data.map { it[USER_NAME_KEY] }

    suspend fun getAccessToken(): String? {
        return dataStore.data.first()[ACCESS_TOKEN_KEY]
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun saveUserInfo(phone: String, name: String?, userId: String?, customerId: String?) {
        dataStore.edit { prefs ->
            prefs[USER_PHONE_KEY] = phone
            name?.let { prefs[USER_NAME_KEY] = it }
            userId?.let { prefs[USER_ID_KEY] = it }
            customerId?.let { prefs[CUSTOMER_ID_KEY] = it }
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    suspend fun getCustomerName(): String? {
        return dataStore.data.first()[USER_NAME_KEY]
    }

    suspend fun isLoggedIn(): Boolean {
        return getAccessToken() != null
    }

    suspend fun getRefreshToken(): String? {
        return dataStore.data.first()[REFRESH_TOKEN_KEY]
    }
}
