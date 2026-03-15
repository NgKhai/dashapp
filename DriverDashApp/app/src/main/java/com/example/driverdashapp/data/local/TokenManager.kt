package com.example.driverdashapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Top-level extension — used by AppModule to provide the DataStore instance. */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "driver_prefs")

@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        private val DRIVER_NAME_KEY = stringPreferencesKey("driver_name")
        private val DRIVER_ID_KEY = stringPreferencesKey("driver_id")
    }

    val accessToken: Flow<String?> = dataStore.data.map { it[ACCESS_TOKEN_KEY] }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun getAccessToken(): String? = dataStore.data.first()[ACCESS_TOKEN_KEY]

    suspend fun getRefreshToken(): String? = dataStore.data.first()[REFRESH_TOKEN_KEY]

    suspend fun saveDriverInfo(name: String, driverId: String) {
        dataStore.edit { prefs ->
            prefs[DRIVER_NAME_KEY] = name
            prefs[DRIVER_ID_KEY] = driverId
        }
    }

    suspend fun getDriverName(): String? = dataStore.data.first()[DRIVER_NAME_KEY]

    suspend fun isLoggedIn(): Boolean = getAccessToken() != null

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}

