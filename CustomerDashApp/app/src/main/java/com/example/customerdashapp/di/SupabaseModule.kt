package com.example.customerdashapp.di

import com.example.customerdashapp.BuildConfig
import com.example.customerdashapp.data.repository.PhotoUploadRepositoryImpl
import com.example.customerdashapp.domain.repository.PhotoUploadRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Realtime)
        install(Storage)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoUploadModule {
    @Binds
    @Singleton
    abstract fun bindPhotoUploadRepository(
        impl: PhotoUploadRepositoryImpl
    ): PhotoUploadRepository
}
