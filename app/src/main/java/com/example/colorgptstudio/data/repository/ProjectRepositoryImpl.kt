package com.example.colorgptstudio.data.repository

import android.content.Context
import android.net.Uri
import com.example.colorgptstudio.data.db.dao.ColorPointDao
import com.example.colorgptstudio.data.db.dao.ImageDao
import com.example.colorgptstudio.data.db.dao.PaletteColorDao
import com.example.colorgptstudio.data.db.dao.ProjectDao
import com.example.colorgptstudio.data.db.entity.ProjectEntity
import com.example.colorgptstudio.data.filesystem.ProjectFileManager
import com.example.colorgptstudio.domain.model.*
import com.example.colorgptstudio.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class ProjectRepositoryImpl(
    private val context: Context,
    private val projectDao: ProjectDao,
    private val imageDao: ImageDao,
    private val colorPointDao: ColorPointDao,
    private val paletteColorDao: PaletteColorDao,
    private val fileManager: ProjectFileManager
) : ProjectRepository {

    override fun observeProjects(): Flow<List<Project>> =
        projectDao.observeAll().flatMapLatest { entities ->
            if (entities.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                // Combina un Flow per ogni progetto che reagisce ai cambiamenti di immagini/punti
                val projectFlows = entities.map { entity ->
                    observeProjectImages(entity.id).map { images ->
                        entity.toDomain(images = images)
                    }
                }
                combine(projectFlows) { it.toList() }
            }
        }

    /** Flow reattivo delle immagini di un progetto (con colorPoints e palette). */
    private fun observeProjectImages(projectId: Long): Flow<List<ProjectImage>> =
        imageDao.observeByProject(projectId).flatMapLatest { imageEntities ->
            if (imageEntities.isEmpty()) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                val imageFlows = imageEntities.map { imageEntity ->
                    combine(
                        colorPointDao.observeByImage(imageEntity.id),
                        paletteColorDao.observeByImage(imageEntity.id)
                    ) { points, paletteColors ->
                        val palette = if (paletteColors.isNotEmpty()) {
                            ColorPalette(
                                projectId = projectId,
                                imageId = imageEntity.id,
                                colors = paletteColors.map { it.toColorData() },
                                label = paletteColors.first().paletteLabel
                            )
                        } else null
                        imageEntity.toDomain(colorPoints = points.map { it.toDomain() }, palette = palette)
                    }
                }
                combine(imageFlows) { it.toList() }
            }
        }

    override suspend fun getProjectById(id: Long): Project? {
        val entity = projectDao.getById(id) ?: return null
        val images = getImagesForProject(id)
        return entity.toDomain(images = images)
    }

    override suspend fun createProject(name: String, description: String): Project {
        // 1. Inserisce il record nel DB (ottiene l'ID generato)
        val entity = ProjectEntity(name = name, description = description)
        val newId = projectDao.insert(entity)

        // 2. Crea la cartella sul filesystem usando l'ID reale
        val folderPath = fileManager.createProjectFolder(newId, name)

        // 3. Aggiorna il record con il folderPath
        val updatedEntity = entity.copy(id = newId, folderPath = folderPath)
        projectDao.update(updatedEntity)

        Timber.d("ProjectRepository: Progetto creato → id=$newId, folder=$folderPath")
        return updatedEntity.toDomain()
    }

    override suspend fun updateProject(project: Project) {
        projectDao.update(project.toEntity())
    }

    override suspend fun deleteProject(id: Long) {
        val project = projectDao.getById(id)
        project?.folderPath?.let { fileManager.deleteProjectFolder(it) }
        projectDao.deleteById(id)
        Timber.d("ProjectRepository: Progetto eliminato → id=$id")
    }

    override suspend fun addImage(projectId: Long, sourceUri: String, label: String): ProjectImage {
        val project = projectDao.getById(projectId) ?: error("Progetto $projectId non trovato")
        val fileName = fileManager.generateImageFileName()
        val localPath = fileManager.copyImageToProject(
            sourceUri = Uri.parse(sourceUri),
            projectFolderPath = project.folderPath,
            fileName = fileName
        )
        val imageEntity = com.example.colorgptstudio.data.db.entity.ImageEntity(
            projectId = projectId,
            fileName = fileName,
            localPath = localPath,
            label = label
        )
        val newId = imageDao.insert(imageEntity)
        return imageEntity.copy(id = newId).toDomain()
    }

    override suspend fun updateImage(image: ProjectImage) {
        imageDao.update(image.toEntity())
    }

    override suspend fun deleteImage(imageId: Long) {
        val image = imageDao.getById(imageId)
        image?.localPath?.let { fileManager.deleteImageFile(it) }
        imageDao.deleteById(imageId)
    }

    override suspend fun saveColorPoint(colorPoint: ColorPoint): ColorPoint {
        val entity = colorPoint.toEntity()
        val id = colorPointDao.insert(entity)
        return colorPoint.copy(id = id)
    }

    override suspend fun deleteColorPoint(colorPointId: Long) {
        colorPointDao.deleteById(colorPointId)
    }

    override suspend fun savePalette(palette: ColorPalette) {
        if (palette.imageId != null) {
            paletteColorDao.deleteForImage(palette.projectId, palette.imageId)
        } else {
            paletteColorDao.deleteGlobalPalette(palette.projectId)
        }
        paletteColorDao.insertAll(palette.toEntities())
    }

    override suspend fun exportProjectJson(projectId: Long) {
        val project = getProjectById(projectId) ?: return
        val projectFolder = project.folderPath
        // La serializzazione richiede che i modelli di dominio siano @Serializable
        // Per ora scriviamo un JSON minimale come placeholder
        val jsonContent = """
            {
              "id": ${project.id},
              "name": "${project.name}",
              "description": "${project.description}",
              "images_count": ${project.images.size}
            }
        """.trimIndent()
        fileManager.writeProjectJson(projectFolder, jsonContent)
    }

    // ─── Helpers privati ──────────────────────────────────────────────────────

    private suspend fun getImagesForProject(projectId: Long): List<ProjectImage> {
        val imageEntities = imageDao.observeByProject(projectId).first()
        return imageEntities.map { imageEntity ->
            val colorPoints = colorPointDao.observeByImage(imageEntity.id).first()
                .map { it.toDomain() }
            val paletteColors = paletteColorDao.observeByImage(imageEntity.id).first()
            val palette = if (paletteColors.isNotEmpty()) {
                ColorPalette(
                    projectId = projectId,
                    imageId = imageEntity.id,
                    colors = paletteColors.map { it.toColorData() },
                    label = paletteColors.first().paletteLabel
                )
            } else null
            imageEntity.toDomain(colorPoints = colorPoints, palette = palette)
        }
    }
}