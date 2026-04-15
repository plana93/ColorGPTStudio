package com.example.colorgptstudio.domain.model

/**
 * Palette di colori dominanti estratta automaticamente da un'immagine
 * tramite K-means clustering.
 *
 * @param id        ID univoco (0 se non ancora persistita)
 * @param projectId ID del progetto a cui appartiene
 * @param imageId   ID dell'immagine da cui è stata estratta (null = globale del progetto)
 * @param colors    Lista dei colori dominanti, ordinata per frequenza decrescente
 * @param label     Etichetta opzionale (es. "Palette automatica", "Versione A")
 */
data class ColorPalette(
    val id: Long = 0L,
    val projectId: Long,
    val imageId: Long? = null,
    val colors: List<ColorData>,
    val label: String = "Palette automatica"
)
