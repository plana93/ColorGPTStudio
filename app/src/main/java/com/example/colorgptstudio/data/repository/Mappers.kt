package com.example.colorgptstudio.data.repository

import com.example.colorgptstudio.data.db.entity.*
import com.example.colorgptstudio.domain.model.*

// ─── Entity → Domain Model ────────────────────────────────────────────────────

fun ProjectEntity.toDomain(
    images: List<ProjectImage> = emptyList(),
    palette: ColorPalette? = null
): Project = Project(
    id = id,
    name = name,
    description = description,
    folderPath = folderPath,
    images = images,
    palette = palette
)

fun ImageEntity.toDomain(
    colorPoints: List<ColorPoint> = emptyList(),
    palette: ColorPalette? = null
): ProjectImage = ProjectImage(
    id = id,
    projectId = projectId,
    fileName = fileName,
    localPath = localPath,
    label = label,
    notes = notes,
    colorPoints = colorPoints,
    palette = palette
)

fun ColorPointEntity.toDomain(): ColorPoint = ColorPoint(
    id = id,
    imageId = imageId,
    xRatio = xRatio,
    yRatio = yRatio,
    color = ColorData(
        hex = hexCode,
        r = r, g = g, b = b,
        h = h, s = s, l = l
    ),
    label = label,
    note = note,
    materialInfo = materialInfo,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }
)

fun PaletteColorEntity.toColorData(): ColorData = ColorData(
    hex = hexCode,
    r = r, g = g, b = b,
    h = h, s = s, l = l
)

// ─── Domain Model → Entity ────────────────────────────────────────────────────

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    name = name,
    description = description,
    folderPath = folderPath
)

fun ProjectImage.toEntity(): ImageEntity = ImageEntity(
    id = id,
    projectId = projectId,
    fileName = fileName,
    localPath = localPath,
    label = label,
    notes = notes
)

fun ColorPoint.toEntity(): ColorPointEntity = ColorPointEntity(
    id = id,
    imageId = imageId,
    xRatio = xRatio,
    yRatio = yRatio,
    hexCode = color.hex,
    r = color.r, g = color.g, b = color.b,
    h = color.h, s = color.s, l = color.l,
    label = label,
    note = note,
    materialInfo = materialInfo,
    tags = tags.joinToString(",")
)

fun ColorPalette.toEntities(): List<PaletteColorEntity> =
    colors.mapIndexed { index, colorData ->
        PaletteColorEntity(
            projectId = projectId,
            imageId = imageId,
            hexCode = colorData.hex,
            r = colorData.r, g = colorData.g, b = colorData.b,
            h = colorData.h, s = colorData.s, l = colorData.l,
            sortOrder = index,
            paletteLabel = label
        )
    }
