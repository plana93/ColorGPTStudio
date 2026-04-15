package com.example.colorgptstudio.data.filesystem

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

/**
 * Gestisce tutte le operazioni sul filesystem per i progetti.
 *
 * Struttura cartelle:
 * ```
 * filesDir/
 *   projects/
 *     {projectId}_{projectName}/
 *       img_001.jpg
 *       img_002.jpg
 *       project_data.json
 * ```
 */
class ProjectFileManager(private val context: Context) {

    private val projectsRoot: File
        get() = File(context.filesDir, "projects").also { it.mkdirs() }

    /**
     * Crea la cartella dedicata per un nuovo progetto.
     * @return Path assoluto della cartella creata
     */
    suspend fun createProjectFolder(projectId: Long, projectName: String): String =
        withContext(Dispatchers.IO) {
            val sanitizedName = projectName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val folder = File(projectsRoot, "${projectId}_$sanitizedName")
            folder.mkdirs()
            Timber.d("ProjectFileManager: Cartella progetto creata → ${folder.absolutePath}")
            folder.absolutePath
        }

    /**
     * Copia un'immagine dall'URI sorgente nella cartella del progetto.
     * @return Path assoluto del file copiato
     */
    suspend fun copyImageToProject(
        sourceUri: Uri,
        projectFolderPath: String,
        fileName: String
    ): String = withContext(Dispatchers.IO) {
        val destFile = File(projectFolderPath, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Impossibile aprire l'URI sorgente: $sourceUri")
        Timber.d("ProjectFileManager: Immagine copiata → ${destFile.absolutePath}")
        destFile.absolutePath
    }

    /**
     * Scrive il contenuto JSON nel file project_data.json della cartella del progetto.
     */
    suspend fun writeProjectJson(projectFolderPath: String, jsonContent: String) =
        withContext(Dispatchers.IO) {
            val jsonFile = File(projectFolderPath, "project_data.json")
            jsonFile.writeText(jsonContent)
            Timber.d("ProjectFileManager: JSON progetto scritto → ${jsonFile.absolutePath}")
        }

    /**
     * Legge il contenuto del file project_data.json.
     * @return Il contenuto JSON come stringa, o null se il file non esiste
     */
    suspend fun readProjectJson(projectFolderPath: String): String? =
        withContext(Dispatchers.IO) {
            val jsonFile = File(projectFolderPath, "project_data.json")
            if (jsonFile.exists()) jsonFile.readText() else null
        }

    /**
     * Elimina un singolo file immagine dalla cartella del progetto.
     */
    suspend fun deleteImageFile(filePath: String) =
        withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
                Timber.d("ProjectFileManager: File eliminato → $filePath")
            }
        }

    /**
     * Elimina l'intera cartella di un progetto (immagini + JSON).
     */
    suspend fun deleteProjectFolder(projectFolderPath: String) =
        withContext(Dispatchers.IO) {
            val folder = File(projectFolderPath)
            if (folder.exists()) {
                folder.deleteRecursively()
                Timber.d("ProjectFileManager: Cartella progetto eliminata → $projectFolderPath")
            }
        }

    /**
     * Genera un nome file univoco per una nuova immagine nel progetto.
     */
    fun generateImageFileName(extension: String = "jpg"): String {
        val timestamp = System.currentTimeMillis()
        return "img_${timestamp}.$extension"
    }
}
