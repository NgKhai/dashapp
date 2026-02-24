package com.example.driverdashapp.di

import android.content.Context
import com.example.driverdashapp.BuildConfig
import com.example.driverdashapp.data.local.TokenManager
import com.example.driverdashapp.data.remote.api.AuthApi
import com.example.driverdashapp.data.remote.api.DeliveryApi
import com.example.driverdashapp.data.remote.api.DriverApi
import com.example.driverdashapp.data.remote.api.OsrmApi
import com.example.driverdashapp.data.remote.interceptor.AuthInterceptor
import com.example.driverdashapp.data.remote.interceptor.TokenAuthenticator
import com.example.driverdashapp.data.repository.AuthRepositoryImpl
import com.example.driverdashapp.data.repository.DriverRepositoryImpl
import com.example.driverdashapp.domain.repository.AuthRepository
import com.example.driverdashapp.domain.repository.DriverRepository
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

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext context: Context): TokenManager {
        return TokenManager(context)
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
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideDriverApi(retrofit: Retrofit): DriverApi = retrofit.create(DriverApi::class.java)

    @Provides
    @Singleton
    fun provideDeliveryApi(retrofit: Retrofit): DeliveryApi = retrofit.create(DeliveryApi::class.java)

    // ── OSRM (route calculation) ──

    @Provides
    @Singleton
    @Named("osrm")
    fun provideOsrmRetrofit(): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOsrmApi(@Named("osrm") retrofit: Retrofit): OsrmApi {
        return retrofit.create(OsrmApi::class.java)
    }

    // ── Repositories ──

    @Provides
    @Singleton
    fun provideAuthRepository(authApi: AuthApi, tokenManager: TokenManager): AuthRepository {
        return AuthRepositoryImpl(authApi, tokenManager)
    }

    @Provides
    @Singleton
    fun provideDriverRepository(
        driverApi: DriverApi,
        deliveryApi: DeliveryApi,
        osrmApi: OsrmApi
    ): DriverRepository {
        return DriverRepositoryImpl(driverApi, deliveryApi, osrmApi)
    }
}
