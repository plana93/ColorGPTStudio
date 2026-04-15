package com.example.colorgptstudio.domain.model

/**
 * Immagine associata a un progetto.
 *
 * @param id          ID univoco (0 se non ancora persistita)
 * @param projectId   ID del [Project] a cui appartiene
 * @param fileName    Nome del file nella cartella del progetto (es. "img_001.jpg")
 * @param localPath   Path assoluto del file copiato nell'area privata dell'app
 * @param label       Etichetta opzionale (es. "Stato Attuale", "Proposta A")
 * @param notes       Note contestuali (es. "scattata in luce serale")
 * @param colorPoints Lista dei punti colore manuali
 * @param palette     Palette automatica estratta (null se non ancora generata)
 */
data class ProjectImage(
    val id: Long = 0L,
    val projectId: Long,
    val fileName: String,
    val localPath: String,
    val label: String = "",
    val notes: String = "",
    val colorPoints: List<ColorPoint> = emptyList(),
    val palette: ColorPalette? = null
)
