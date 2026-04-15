package com.example.colorgptstudio.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "palette_colors",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class PaletteColorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long,
    /** ID dell'immagine sorgente (null = palette globale di progetto) */
    val imageId: Long? = null,
    val hexCode: String,
    val r: Int,
    val g: Int,
    val b: Int,
    val h: Float,
    val s: Float,
    val l: Float,
    /** Posizione ordinale nella palette (0 = più dominante) */
    val sortOrder: Int,
    val paletteLabel: String = "Palette automatica"
)
