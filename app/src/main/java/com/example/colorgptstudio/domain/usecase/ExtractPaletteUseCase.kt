package com.example.colorgptstudio.domain.usecase

import android.graphics.Bitmap
import com.example.colorgptstudio.domain.model.ColorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * UseCase per l'estrazione della palette di colori dominanti tramite K-means clustering.
 *
 * Implementazione pura in Kotlin, senza dipendenze esterne per il clustering di base.
 * Non richiede OpenCV per la versione MVP; OpenCV può sostituire questo algoritmo
 * per performance migliori su immagini grandi in una versione futura.
 *
 * Parametri di filtraggio pre-clustering (configurabili):
 * - Esclude pixel con luminosità < [minLightness] o > [maxLightness]
 * - Esclude pixel con saturazione < [minSaturation]
 * Questo evita che i "bianchi sporchi" e i "neri" dominino la palette.
 */
class ExtractPaletteUseCase(
    private val minLightness: Float = 0.08f,
    private val maxLightness: Float = 0.95f,
    private val minSaturation: Float = 0.08f
) {
    /**
     * Estrae i [k] colori dominanti dal [bitmap].
     *
     * @param bitmap    Immagine sorgente (verrà campionata internamente per performance)
     * @param k         Numero di colori dominanti da estrarre (default: 5)
     * @param maxPixels Numero massimo di pixel da analizzare (downsampling automatico)
     * @return Lista di [ColorData] ordinata per frequenza decrescente
     */
    suspend operator fun invoke(
        bitmap: Bitmap,
        k: Int = 5,
        maxPixels: Int = 10_000
    ): List<ColorData> = withContext(Dispatchers.Default) {

        // ─── 1. Campionamento: riduce il bitmap per performance ───────────────
        val sampledBitmap = sampleBitmap(bitmap, maxPixels)

        // ─── 2. Raccolta pixel + filtro colori "sporchi" ──────────────────────
        val pixels = collectFilteredPixels(sampledBitmap)
        if (pixels.size < k) return@withContext emptyList()

        // ─── 3. K-means clustering ────────────────────────────────────────────
        val centroids = kMeans(pixels, k, maxIterations = 20)

        // ─── 4. Calcola la dimensione di ogni cluster (per ordinare per frequenza)
        val clusterSizes = IntArray(k)
        pixels.forEach { pixel ->
            val nearest = nearestCentroid(pixel, centroids)
            clusterSizes[nearest]++
        }

        // ─── 5. Ordina i centroidi per frequenza decrescente e crea ColorData ─
        centroids
            .mapIndexed { idx, centroid -> Pair(clusterSizes[idx], centroid) }
            .sortedByDescending { it.first }
            .map { (_, centroid) ->
                val r = centroid[0].toInt().coerceIn(0, 255)
                val g = centroid[1].toInt().coerceIn(0, 255)
                val b = centroid[2].toInt().coerceIn(0, 255)
                val argb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                ColorData.fromArgb(argb)
            }
    }

    // ─── Helpers privati ──────────────────────────────────────────────────────

    private fun sampleBitmap(bitmap: Bitmap, maxPixels: Int): Bitmap {
        val totalPixels = bitmap.width * bitmap.height
        if (totalPixels <= maxPixels) return bitmap
        val scale = sqrt(maxPixels.toFloat() / totalPixels)
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun collectFilteredPixels(bitmap: Bitmap): List<FloatArray> {
        val pixels = mutableListOf<FloatArray>()
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val argb = bitmap.getPixel(x, y)
                val r = ((argb shr 16) and 0xFF) / 255f
                val g = ((argb shr 8) and 0xFF) / 255f
                val b = (argb and 0xFF) / 255f

                // Calcola luminosità e saturazione per il filtro
                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                val l = (max + min) / 2f
                val s = if (max == min) 0f else {
                    val delta = max - min
                    delta / (1f - abs(2f * l - 1f))
                }

                if (l >= minLightness && l <= maxLightness && s >= minSaturation) {
                    pixels.add(floatArrayOf(r * 255f, g * 255f, b * 255f))
                }
            }
        }
        return pixels
    }

    private fun kMeans(
        pixels: List<FloatArray>,
        k: Int,
        maxIterations: Int
    ): List<FloatArray> {
        // Inizializzazione: seleziona k pixel casuali come centroidi iniziali
        val indices = pixels.indices.shuffled().take(k)
        val centroids = indices.map { pixels[it].copyOf() }.toMutableList()

        repeat(maxIterations) {
            val clusterSums = Array(k) { FloatArray(3) }
            val clusterCounts = IntArray(k)

            // Assegna ogni pixel al centroide più vicino
            pixels.forEach { pixel ->
                val nearest = nearestCentroid(pixel, centroids)
                clusterSums[nearest][0] += pixel[0]
                clusterSums[nearest][1] += pixel[1]
                clusterSums[nearest][2] += pixel[2]
                clusterCounts[nearest]++
            }

            // Aggiorna i centroidi con la media del cluster
            var changed = false
            for (i in 0 until k) {
                if (clusterCounts[i] > 0) {
                    val newCentroid = floatArrayOf(
                        clusterSums[i][0] / clusterCounts[i],
                        clusterSums[i][1] / clusterCounts[i],
                        clusterSums[i][2] / clusterCounts[i]
                    )
                    if (!newCentroid.contentEquals(centroids[i])) {
                        centroids[i] = newCentroid
                        changed = true
                    }
                }
            }
            // Early exit se i centroidi non cambiano
            if (!changed) return centroids
        }
        return centroids
    }

    private fun nearestCentroid(pixel: FloatArray, centroids: List<FloatArray>): Int {
        var minDist = Float.MAX_VALUE
        var nearest = 0
        centroids.forEachIndexed { idx, centroid ->
            val dr = pixel[0] - centroid[0]
            val dg = pixel[1] - centroid[1]
            val db = pixel[2] - centroid[2]
            val dist = dr * dr + dg * dg + db * db // distanza euclidea al quadrato
            if (dist < minDist) {
                minDist = dist
                nearest = idx
            }
        }
        return nearest
    }
}
