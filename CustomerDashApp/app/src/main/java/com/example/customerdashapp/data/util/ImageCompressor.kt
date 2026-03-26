package com.example.customerdashapp.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * Utility to compress images before upload to Supabase Storage.
 * Reduces size to max 1024px width and compresses to 80% JPEG quality.
 */
object ImageCompressor {

    private const val MAX_WIDTH = 1024
    private const val JPEG_QUALITY = 80

    /**
     * Compress a content URI to JPEG bytes suitable for upload.
     * @return compressed JPEG byte array, or null if the URI can't be read.
     */
    fun compressFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (original == null) return null

            val scaled = scaleDown(original)
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)

            if (scaled !== original) scaled.recycle()
            original.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_WIDTH) return bitmap

        val ratio = MAX_WIDTH.toFloat() / bitmap.width
        val newWidth = MAX_WIDTH
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
