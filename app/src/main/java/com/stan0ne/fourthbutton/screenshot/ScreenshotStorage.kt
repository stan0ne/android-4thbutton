package com.stan0ne.fourthbutton.screenshot

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.hardware.HardwareBuffer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.stan0ne.fourthbutton.util.LogUtil

/**
 * Persists a captured screenshot into the app's Pictures directory using the
 * modern scoped-storage MediaStore API. No storage permission is required on
 * Android 10+; API 28-29 fall back to MediaStore with an explicit data path.
 */
object ScreenshotStorage {

    private const val TAG = "SCREENSHOT"
    private const val DIRECTORY = "AssistivePower"

    /**
     * Wraps the [HardwareBuffer] into a [Bitmap] and writes it to the gallery.
     * Bounded [width]/[height] passed as a safety hint is ignored in favour of
     * the buffer's own dimensions.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun save(context: Context, buffer: HardwareBuffer): Result<Uri> {
        return runCatching {
            val bitmap = createBitmap(buffer)
            try {
                val uri = writeBitmap(context, bitmap)
                return@runCatching uri
            } finally {
                bitmap.recycle()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createBitmap(buffer: HardwareBuffer): Bitmap {
        val bitmap = Bitmap.wrapHardwareBuffer(buffer, ColorSpace.get(ColorSpace.Named.SRGB))
            ?: throw IllegalStateException("Failed to wrap hardware buffer")
        return bitmap
    }

    private fun writeBitmap(context: Context, bitmap: Bitmap): Uri {
        val resolver = context.contentResolver
        val name = ScreenshotFileNameGenerator.generate()

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + DIRECTORY
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("MediaStore insert returned null")

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IllegalStateException("PNG compression failed")
                }
            } ?: throw IllegalStateException("Cannot open output stream")
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pending = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, pending, null, null)
        }
        return uri
    }
}