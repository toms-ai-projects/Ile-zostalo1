package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun saveAndScaleImage(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            var width = options.outWidth
            var height = options.outHeight
            val maxWidth = 1080

            var inSampleSize = 1
            if (width > maxWidth) {
                inSampleSize = Math.round(width.toFloat() / maxWidth.toFloat())
            }

            val options2 = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val inputStream2 = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, options2)
            inputStream2?.close()

            if (bitmap != null) {
                val file = File(context.filesDir, "event_image_${System.currentTimeMillis()}.jpg")
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                outputStream.close()
                file.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
