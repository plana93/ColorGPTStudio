package com.example.colorgptstudio.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.colorgptstudio.domain.model.ColorData

/**
 * Estrae il colore dal pixel corrispondente alle coordinate normalizzate [0..1]
 * all'interno dell'immagine puntata da [uri].
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
