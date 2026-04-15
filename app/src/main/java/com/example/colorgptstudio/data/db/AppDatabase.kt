package com.example.colorgptstudio.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.colorgptstudio.data.db.dao.ColorPointDao
import com.example.colorgptstudio.data.db.dao.ImageDao
import com.example.colorgptstudio.data.db.dao.PaletteColorDao
import com.example.colorgptstudio.data.db.dao.ProjectDao
import com.example.colorgptstudio.data.db.entity.ColorPointEntity
import com.example.colorgptstudio.data.db.entity.ImageEntity
import com.example.colorgptstudio.data.db.entity.PaletteColorEntity
import com.example.colorgptstudio.data.db.entity.ProjectEntity

@Database(
    entities = [
        ProjectEntity::class,
        ImageEntity::class,
        ColorPointEntity::class,
        PaletteColorEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun imageDao(): ImageDao
    abstract fun colorPointDao(): ColorPointDao
    abstract fun paletteColorDao(): PaletteColorDao

    companion object {
        private const val DATABASE_NAME = "colorgptstudio.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
