package com.example.colorgptstudio.domain.model

/**
 * Punto colore scelto manualmente dall'utente su un'immagine.
 *
 * @param id         ID univoco (0 se non ancora persistito)
 * @param imageId    ID dell'[ProjectImage] a cui appartiene
 * @param xRatio     Posizione X normalizzata [0..1] rispetto alla larghezza dell'immagine
 * @param yRatio     Posizione Y normalizzata [0..1] rispetto all'altezza dell'immagine
 * @param color      Dati cromatici estratti nel punto
 * @param label      Etichetta opzionale (es. "Parete principale")
 * @param note       Nota semantica libera (es. "luce fredda, mattina")
 * @param materialInfo Prodotto / codice vernice / fornitore
 * @param tags       Lista di tag "da cantiere" (es. ["pensile", "laccato"])
 */
data class ColorPoint(
    val id: Long = 0L,
    val imageId: Long,
    val xRatio: Float,
    val yRatio: Float,
    val color: ColorData,
    val label: String = "",
    val note: String = "",
    val materialInfo: String = "",
    val tags: List<String> = emptyList()
)
