package com.example.colorgptstudio.data.db.dao

import androidx.room.*
import com.example.colorgptstudio.data.db.entity.ImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {

    @Query("SELECT * FROM images WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun observeByProject(projectId: Long): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getById(id: Long): ImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageEntity): Long

    @Update
    suspend fun update(image: ImageEntity)

    @Delete
    suspend fun delete(image: ImageEntity)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteById(id: Long)
}
