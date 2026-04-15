package com.example.colorgptstudio.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.colorgptstudio.domain.model.ColorData
import java.io.File

/**
 * Estrae il colore tramite URI (usato per immagini dalla galleria/camera).
 */
fun extractColorAtRatio(
    context: Context,
    uri: Uri,
    xRatio: Float,
    yRatio: Float
): ColorData? = try {
    val inputStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream?.close()
    if (bitmap != null) {
        val x = (xRatio * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (yRatio * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val argb = bitmap.getPixel(x, y)
        bitmap.recycle()
        ColorData.fromArgb(argb)
    } else null
} catch (e: Exception) {
    null
}

/**
 * Estrae il colore direttamente dal path del file su disco.
 * Necessario per i file in filesDir perché Uri.fromFile + contentResolver
 * viene bloccato da SELinux su alcuni dispositivi (path /data/user/0).
 * BitmapFactory.decodeFile accede direttamente tramite il filesystem
 * del processo, che ha sempre permesso sui propri filesDir.
 */
fun extractColorAtRatioFromFile(
    file: File,
    xRatio: Float,
    yRatio: Float
): ColorData? = try {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    if (bitmap != null) {
        val x = (xRatio * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val y = (yRatio * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val argb = bitmap.getPixel(x, y)
        bitmap.recycle()
        ColorData.fromArgb(argb)
    } else null
} catch (e: Exception) {
    null
}
