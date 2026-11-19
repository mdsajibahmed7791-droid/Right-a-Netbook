package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.min

object ImageUtils {
    private const val TARGET_SIZE = 200
    private const val JPEG_QUALITY = 76

    private val bitmapCache = object : LruCache<Int, Bitmap>(64) {}

    fun encodeUriToBase64Thumbnail(context: Context, uri: Uri): String? {
        return try {
            val sourceBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val minSide = min(info.size.width, info.size.height)
                    if (minSide > TARGET_SIZE * 2) {
                        val sample = minSide / (TARGET_SIZE * 2)
                        if (sample > 1) decoder.setTargetSampleSize(sample)
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } ?: return null

            val cropped = centerCropSquare(sourceBitmap, TARGET_SIZE)
            val out = ByteArrayOutputStream()
            cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            if (cropped !== sourceBitmap) cropped.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun centerCropSquare(src: Bitmap, targetSize: Int): Bitmap {
        val width = src.width
        val height = src.height
        val cropSize = min(width, height).coerceAtLeast(1)
        val xOffset = (width - cropSize) / 2
        val yOffset = (height - cropSize) / 2
        val square = Bitmap.createBitmap(src, xOffset, yOffset, cropSize, cropSize)
        val scaled = Bitmap.createScaledBitmap(square, targetSize, targetSize, true)
        if (square !== src && square !== scaled) square.recycle()
        return scaled
    }

    fun decodeBase64ToBitmap(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        val key = base64.hashCode()
        bitmapCache.get(key)?.let { return it }
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) bitmapCache.put(key, bmp)
            bmp
        } catch (_: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        return decodeBase64ToBitmap(base64)?.asImageBitmap()
    }
}
