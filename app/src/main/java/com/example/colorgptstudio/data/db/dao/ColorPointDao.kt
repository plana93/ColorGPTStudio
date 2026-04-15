package com.example.colorgptstudio.data.db.dao

import androidx.room.*
import com.example.colorgptstudio.data.db.entity.ColorPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ColorPointDao {

    @Query("SELECT * FROM color_points WHERE imageId = :imageId ORDER BY id ASC")
    fun observeByImage(imageId: Long): Flow<List<ColorPointEntity>>

    @Query("SELECT * FROM color_points WHERE id = :id")
    suspend fun getById(id: Long): ColorPointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(colorPoint: ColorPointEntity): Long

    @Update
    suspend fun update(colorPoint: ColorPointEntity)

    @Delete
    suspend fun delete(colorPoint: ColorPointEntity)

    @Query("DELETE FROM color_points WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM color_points WHERE imageId = :imageId")
    suspend fun deleteAllForImage(imageId: Long)
}
