package com.example.colorgptstudio.di

import com.example.colorgptstudio.data.repository.ProjectRepositoryImpl
import com.example.colorgptstudio.domain.repository.ProjectRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<ProjectRepository> {
        ProjectRepositoryImpl(
            context = androidContext(),
            projectDao = get(),
            imageDao = get(),
            colorPointDao = get(),
            paletteColorDao = get(),
            fileManager = get()
        )
    }
}
