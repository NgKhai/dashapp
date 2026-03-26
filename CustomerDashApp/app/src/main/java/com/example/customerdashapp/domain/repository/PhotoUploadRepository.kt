package com.example.customerdashapp.domain.repository

import android.net.Uri
import com.example.customerdashapp.domain.model.AppResult

/**
 * Repository for uploading delivery item photos to Supabase Storage.
 */
interface PhotoUploadRepository {
    /**
     * Upload multiple photos to Supabase Storage.
     * @param photoUris list of content URIs for photos to upload (max 5)
     * @return list of public URLs for the uploaded photos
     */
    suspend fun uploadDeliveryPhotos(photoUris: List<Uri>): AppResult<List<String>>
}
