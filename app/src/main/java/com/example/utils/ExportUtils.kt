package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import com.example.data.ExportedEvent
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream

object ExportUtils {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(ExportedEvent::class.java)

    fun encodeEventToJson(event: ExportedEvent): String {
        return adapter.toJson(event)
    }

    fun decodeEventFromJson(json: String): ExportedEvent? {
        return try {
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun generateQrCode(text: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareEventAsFile(context: Context, eventJson: String, fileName: String = "wydarzenie.iledni") {
        try {
            val cachePath = File(context.cacheDir, "shared_events")
            cachePath.mkdirs()
            val file = File(cachePath, fileName)
            val stream = FileOutputStream(file)
            stream.write(eventJson.toByteArray())
            stream.close()

            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "application/json"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(shareIntent, "Udostępnij wydarzenie"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
