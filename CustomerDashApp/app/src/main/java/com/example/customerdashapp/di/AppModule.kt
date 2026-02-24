package com.example.customerdashapp.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.customerdashapp.BuildConfig
import com.example.customerdashapp.data.local.TokenManager
import com.example.customerdashapp.data.remote.api.AuthApi
import com.example.customerdashapp.data.remote.api.CustomerApi
import com.example.customerdashapp.data.remote.api.DeliveryApi
import com.example.customerdashapp.data.remote.api.NominatimApi
import com.example.customerdashapp.data.remote.api.OsrmApi
import com.example.customerdashapp.data.remote.interceptor.AuthInterceptor
import com.example.customerdashapp.data.remote.interceptor.TokenAuthenticator
import com.example.customerdashapp.data.repository.AuthRepositoryImpl
import com.example.customerdashapp.data.repository.CustomerRepositoryImpl
import com.example.customerdashapp.data.repository.DeliveryRepositoryImpl
import com.example.customerdashapp.data.repository.MapRepositoryImpl
import com.example.customerdashapp.domain.repository.AuthRepository
import com.example.customerdashapp.domain.repository.CustomerRepository
import com.example.customerdashapp.domain.repository.DeliveryRepository
import com.example.customerdashapp.domain.repository.MapRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dashapp_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideTokenManager(dataStore: DataStore<Preferences>): TokenManager {
        return TokenManager(dataStore)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenManager: TokenManager): AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideTokenAuthenticator(
        tokenManager: TokenManager,
        authApi: dagger.Lazy<AuthApi>
    ): TokenAuthenticator {
        return TokenAuthenticator(tokenManager, authApi)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("map")
    fun provideMapOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "DashApp/1.0 (Customer Android App)")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("nominatim")
    fun provideNominatimRetrofit(@Named("map") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("osrm")
    fun provideOsrmRetrofit(@Named("map") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDeliveryApi(retrofit: Retrofit): DeliveryApi {
        return retrofit.create(DeliveryApi::class.java)
    }

    @Provides
    @Singleton
    fun provideCustomerApi(retrofit: Retrofit): CustomerApi {
        return retrofit.create(CustomerApi::class.java)
    }

    @Provides
    @Singleton
    fun provideNominatimApi(@Named("nominatim") retrofit: Retrofit): NominatimApi {
        return retrofit.create(NominatimApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOsrmApi(@Named("osrm") retrofit: Retrofit): OsrmApi {
        return retrofit.create(OsrmApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        authApi: AuthApi,
        tokenManager: TokenManager
    ): AuthRepository {
        return AuthRepositoryImpl(authApi, tokenManager)
    }

    @Provides
    @Singleton
    fun provideDeliveryRepository(
        deliveryApi: DeliveryApi
    ): DeliveryRepository {
        return DeliveryRepositoryImpl(deliveryApi)
    }

    @Provides
    @Singleton
    fun provideCustomerRepository(
        customerApi: CustomerApi
    ): CustomerRepository {
        return CustomerRepositoryImpl(customerApi)
    }

    @Provides
    @Singleton
    fun provideMapRepository(
        nominatimApi: NominatimApi,
        osrmApi: OsrmApi
    ): MapRepository {
        return MapRepositoryImpl(nominatimApi, osrmApi)
    }
}
