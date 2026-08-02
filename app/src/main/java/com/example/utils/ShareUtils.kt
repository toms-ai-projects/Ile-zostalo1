package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun shareBitmap(context: Context, bitmap: Bitmap) {
    try {
        var outBitmap = bitmap
        val minWidth = 1080
        if (bitmap.width < minWidth) {
            val scale = minWidth.toFloat() / bitmap.width.toFloat()
            val newHeight = (bitmap.height * scale).toInt()
            outBitmap = Bitmap.createScaledBitmap(bitmap, minWidth, newHeight, true)
        }

        val cachePath = File(context.cacheDir, "shared_images")
        cachePath.mkdirs() // don't forget to make the directory
        val file = File(cachePath, "event_share.png")
        val stream = FileOutputStream(file) // overwrites this image every time
        outBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/png"
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(shareIntent, "Udostępnij wydarzenie"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
