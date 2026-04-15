package com.example.colorgptstudio.domain.model

import java.time.LocalDateTime

/**
 * Contenitore logico di un lavoro reale (es. "Cucina Rossi").
 *
 * @param id          ID univoco (0 se non ancora persistito)
 * @param name        Nome del progetto
 * @param description Descrizione/note globali (es. stile, vincoli, cliente)
 * @param folderPath  Path assoluto della cartella dedicata del progetto
 * @param createdAt   Data e ora di creazione
 * @param updatedAt   Data e ora dell'ultimo aggiornamento
 * @param images      Lista delle immagini associate
 * @param palette     Palette globale del progetto (aggregato)
 */
data class Project(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val folderPath: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val images: List<ProjectImage> = emptyList(),
    val palette: ColorPalette? = null
) {
    /** Immagine di copertina (prima disponibile) per le anteprime nelle card */
    val coverImage: ProjectImage? get() = images.firstOrNull()
}
