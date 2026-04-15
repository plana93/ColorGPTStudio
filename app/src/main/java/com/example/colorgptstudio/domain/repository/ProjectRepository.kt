package com.example.colorgptstudio.domain.repository

import com.example.colorgptstudio.domain.model.ColorPoint
import com.example.colorgptstudio.domain.model.ColorPalette
import com.example.colorgptstudio.domain.model.Project
import com.example.colorgptstudio.domain.model.ProjectImage
import kotlinx.coroutines.flow.Flow

/**
 * Contratto del repository per la gestione dei Progetti.
 * Il domain layer conosce solo questa interfaccia, non l'implementazione concreta.
 */
interface ProjectRepository {

    /** Stream reattivo di tutti i progetti, ordinati per data aggiornamento decrescente */
    fun observeProjects(): Flow<List<Project>>

    /** Recupera un progetto per ID con tutte le sue immagini e punti colore */
    suspend fun getProjectById(id: Long): Project?

    /** Crea un nuovo progetto (crea anche la cartella sul filesystem) */
    suspend fun createProject(name: String, description: String): Project

    /** Aggiorna i metadati di un progetto esistente */
    suspend fun updateProject(project: Project)

    /** Rinomina un progetto */
    suspend fun renameProject(id: Long, newName: String)

    /** Elimina un progetto (elimina anche la cartella e tutti i file dal filesystem) */
    suspend fun deleteProject(id: Long)

    /** Aggiunge un'immagine a un progetto (copia il file nella cartella del progetto) */
    suspend fun addImage(projectId: Long, sourceUri: String, label: String): ProjectImage

    /** Aggiorna i metadati di un'immagine (label, notes) */
    suspend fun updateImage(image: ProjectImage)

    /** Elimina un'immagine (elimina anche il file fisico) */
    suspend fun deleteImage(imageId: Long)

    /** Aggiunge o aggiorna un punto colore su un'immagine */
    suspend fun saveColorPoint(colorPoint: ColorPoint): ColorPoint

    /** Elimina un punto colore */
    suspend fun deleteColorPoint(colorPointId: Long)

    /** Salva la palette estratta per un'immagine */
    suspend fun savePalette(palette: ColorPalette)

    /** Serializza e scrive il file project_data.json nella cartella del progetto */
    suspend fun exportProjectJson(projectId: Long)
}
