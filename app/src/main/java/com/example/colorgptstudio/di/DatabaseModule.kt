package com.example.colorgptstudio.di

import com.example.colorgptstudio.data.db.AppDatabase
import com.example.colorgptstudio.data.filesystem.ProjectFileManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { AppDatabase.create(androidContext()) }
    single { get<AppDatabase>().projectDao() }
    single { get<AppDatabase>().imageDao() }
    single { get<AppDatabase>().colorPointDao() }
    single { get<AppDatabase>().paletteColorDao() }
    single { ProjectFileManager(androidContext()) }
}
