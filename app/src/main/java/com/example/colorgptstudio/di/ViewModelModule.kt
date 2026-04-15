package com.example.colorgptstudio.di

import com.example.colorgptstudio.data.tags.TagRepository
import com.example.colorgptstudio.domain.usecase.ExtractPaletteUseCase
import com.example.colorgptstudio.ui.analysis.AnalysisViewModel
import com.example.colorgptstudio.ui.home.HomeViewModel
import com.example.colorgptstudio.ui.project.ProjectDetailViewModel
import com.example.colorgptstudio.ui.project.ProjectListViewModel
import com.example.colorgptstudio.ui.quickanalysis.QuickAnalysisViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    // UseCases e Repository
    single { ExtractPaletteUseCase() }
    single { TagRepository(androidContext()) }

    // ViewModels
    viewModel { HomeViewModel() }
    viewModel { ProjectListViewModel(get()) }
    viewModel { QuickAnalysisViewModel() }
    viewModel { (projectId: Long) -> ProjectDetailViewModel(projectId, get()) }
    viewModel { (projectId: Long, imageId: Long) -> AnalysisViewModel(projectId, imageId, get(), get(), get()) }
}
