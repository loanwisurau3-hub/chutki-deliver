package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ImageCompressUtils {

    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

    suspend fun compressUriToDataUrl(
        context: Context,
        uri: Uri,
        maxDimension: Int = 640,
        quality: Int = 65
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return@runCatching null

            var sampleSize = 1
            while ((srcW / sampleSize) > maxDimension * 2 || (srcH / sampleSize) > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@runCatching null

            encodeScaledBitmap(decoded, maxDimension, quality)
        }.getOrNull()
    }

    suspend fun compressBitmapToDataUrl(
        bitmap: Bitmap,
        maxDimension: Int = 640,
        quality: Int = 68
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            encodeScaledBitmap(bitmap, maxDimension, quality)
        }.getOrNull()
    }

    fun decodeDataUrlToBitmap(dataUrl: String?): Bitmap? {
        if (dataUrl.isNullOrBlank()) return null
        return runCatching {
            val base64Part = if (dataUrl.contains(",")) {
                dataUrl.substringAfter(",")
            } else {
                dataUrl
            }
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun encodeScaledBitmap(source: Bitmap, maxDimension: Int, quality: Int): String {
        val largest = max(source.width, source.height)
        val scaled = if (largest > maxDimension) {
            val ratio = maxDimension.toFloat() / largest.toFloat()
            val targetW = (source.width * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (source.height * ratio).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(source, targetW, targetH, true)
        } else {
            source
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(30, 90), out)
        if (scaled !== source) {
            scaled.recycle()
        }
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return "$DATA_URI_PREFIX$b64"
    }
}
