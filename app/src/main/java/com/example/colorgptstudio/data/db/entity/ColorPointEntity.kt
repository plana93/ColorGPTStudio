package com.example.colorgptstudio.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "color_points",
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["id"],
            childColumns = ["imageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("imageId")]
)
data class ColorPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val imageId: Long,
    val xRatio: Float,
    val yRatio: Float,
    val hexCode: String,
    val r: Int,
    val g: Int,
    val b: Int,
    val h: Float,
    val s: Float,
    val l: Float,
    val label: String = "",
    val note: String = "",
    val materialInfo: String = "",
    /** Tags serializzati come stringa separata da virgola per semplicità */
    val tags: String = ""
)
