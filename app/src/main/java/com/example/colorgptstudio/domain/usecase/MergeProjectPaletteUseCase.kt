package com.example.colorgptstudio.domain.usecase

import com.example.colorgptstudio.domain.model.ColorData
import com.example.colorgptstudio.domain.model.ColorPalette
import com.example.colorgptstudio.domain.model.ProjectImage
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Calcola la palette globale di un progetto unendo i colori da tutte le immagini.
 *
 * Strategia:
 * 1. Raccoglie tutti i [ColorData] presenti nelle palette per immagine.
 *    I colori provenienti da [ColorPoint] con tag hanno priorità (duplicati).
 * 2. Raggruppa colori "simili" per distanza euclidea in spazio RGB.
 * 3. Per ogni cluster calcola il centroide → colore rappresentativo.
 * 4. Restituisce al massimo [maxColors] colori, ordinati per popolarità del cluster.
 */
class MergeProjectPaletteUseCase {

    /**
     * @param images      Lista di immagini con palette e colorPoints già caricati
     * @param projectId   ID del progetto
     * @param maxColors   Numero massimo di colori nella palette globale (default 8)
     * @param threshold   Distanza RGB sotto cui due colori sono considerati "simili" (default 40)
     */
    operator fun invoke(
        images: List<ProjectImage>,
        projectId: Long,
        maxColors: Int = 8,
        threshold: Float = 40f
    ): ColorPalette? {
        val allColors = mutableListOf<ColorData>()

        images.forEach { image ->
            // Aggiungi colori dalla palette estratta automaticamente
            image.palette?.colors?.forEach { allColors.add(it) }

            // I colorPoint con tag hanno peso doppio (priorità utente)
            image.colorPoints.forEach { point ->
                allColors.add(point.color)
                if (point.tags.isNotEmpty() || point.label.isNotBlank()) {
                    allColors.add(point.color) // peso extra
                }
            }
        }

        if (allColors.isEmpty()) return null

        // Clustering greedy: unisce colori simili
        val clusters = mutableListOf<MutableList<ColorData>>()

        for (color in allColors) {
            val nearest = clusters.firstOrNull { cluster ->
                val centroid = cluster.centroid()
                rgbDistance(color, centroid) < threshold
            }
            if (nearest != null) {
                nearest.add(color)
            } else {
                clusters.add(mutableListOf(color))
            }
        }

        // Ordina cluster per dimensione (i più frequenti prima)
        val topColors = clusters
            .sortedByDescending { it.size }
            .take(maxColors)
            .map { it.centroid() }

        return ColorPalette(
            projectId = projectId,
            imageId = null,           // palette globale del progetto
            colors = topColors,
            label = "Palette Progetto"
        )
    }

    private fun List<ColorData>.centroid(): ColorData {
        val rAvg = map { it.r }.average().toInt()
        val gAvg = map { it.g }.average().toInt()
        val bAvg = map { it.b }.average().toInt()
        return ColorData.fromArgb(
            (0xFF shl 24) or (rAvg shl 16) or (gAvg shl 8) or bAvg
        )
    }

    private fun rgbDistance(a: ColorData, b: ColorData): Float {
        val dr = (a.r - b.r).toFloat()
        val dg = (a.g - b.g).toFloat()
        val db = (a.b - b.b).toFloat()
        return sqrt(dr * dr + dg * dg + db * db)
    }
}
