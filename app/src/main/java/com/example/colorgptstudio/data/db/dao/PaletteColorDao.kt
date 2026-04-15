package com.example.colorgptstudio.data.db.dao

import androidx.room.*
import com.example.colorgptstudio.data.db.entity.PaletteColorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaletteColorDao {

    @Query("SELECT * FROM palette_colors WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun observeByProject(projectId: Long): Flow<List<PaletteColorEntity>>

    @Query("SELECT * FROM palette_colors WHERE imageId = :imageId ORDER BY sortOrder ASC")
    fun observeByImage(imageId: Long): Flow<List<PaletteColorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(colors: List<PaletteColorEntity>)

    @Query("DELETE FROM palette_colors WHERE projectId = :projectId AND imageId = :imageId")
    suspend fun deleteForImage(projectId: Long, imageId: Long)

    @Query("DELETE FROM palette_colors WHERE projectId = :projectId AND imageId IS NULL")
    suspend fun deleteGlobalPalette(projectId: Long)
}
