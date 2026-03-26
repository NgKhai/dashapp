package com.example.customerdashapp.data.repository

import android.content.Context
import android.net.Uri
import com.example.customerdashapp.BuildConfig
import com.example.customerdashapp.data.util.ImageCompressor
import com.example.customerdashapp.domain.model.AppResult
import com.example.customerdashapp.domain.repository.PhotoUploadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import javax.inject.Inject
import java.util.UUID

/**
 * Uploads delivery item photos to Supabase Storage bucket "delivery-photos".
 * 
 * Each photo is:
 * 1. Compressed to JPEG (max 1024px, 80% quality) via ImageCompressor
 * 2. Uploaded to Supabase Storage with a unique filename
 * 3. Returns the public URL
 */
class PhotoUploadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseClient: SupabaseClient
) : PhotoUploadRepository {

    companion object {
        private const val BUCKET_NAME = "delivery-photos"
        private const val MAX_PHOTOS = 5
    }

    override suspend fun uploadDeliveryPhotos(photoUris: List<Uri>): AppResult<List<String>> {
        return try {
            val urisToUpload = photoUris.take(MAX_PHOTOS)
            val urls = mutableListOf<String>()

            val bucket = supabaseClient.storage.from(BUCKET_NAME)

            for (uri in urisToUpload) {
                val bytes = ImageCompressor.compressFromUri(context, uri)
                    ?: continue // Skip photos that can't be read

                val fileName = "items/${UUID.randomUUID()}.jpg"
                bucket.upload(fileName, bytes) {
                    upsert = false
                }

                // Build public URL
                val publicUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$BUCKET_NAME/$fileName"
                urls.add(publicUrl)
            }

            if (urls.isEmpty() && urisToUpload.isNotEmpty()) {
                AppResult.Error("Không thể tải ảnh lên")
            } else {
                AppResult.Success(urls)
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Lỗi tải ảnh lên")
        }
    }
}
