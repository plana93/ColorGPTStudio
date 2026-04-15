package com.example.colorgptstudio.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorgptstudio.domain.model.ColorPalette
import com.example.colorgptstudio.domain.model.Project
import com.example.colorgptstudio.domain.repository.ProjectRepository
import com.example.colorgptstudio.domain.usecase.MergeProjectPaletteUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val project: Project? = null,
    val globalPalette: ColorPalette? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProjectDetailViewModel(
    private val projectId: Long,
    private val repository: ProjectRepository,
    private val mergeProjectPalette: MergeProjectPaletteUseCase = MergeProjectPaletteUseCase()
) : ViewModel() {

    val uiState: StateFlow<ProjectDetailUiState> = repository
        .observeProjects()
        .map { projects ->
            val project = projects.find { it.id == projectId }
            val globalPalette = project?.let {
                mergeProjectPalette(images = it.images, projectId = projectId)
            }
            ProjectDetailUiState(
                project = project,
                globalPalette = globalPalette,
                isLoading = false
            )
        }
        .catch { e -> emit(ProjectDetailUiState(isLoading = false, error = e.message)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProjectDetailUiState()
        )

    fun addImage(sourceUri: String, label: String = "") {
        viewModelScope.launch {
            repository.addImage(projectId, sourceUri, label)
        }
    }

    fun deleteImage(imageId: Long) {
        viewModelScope.launch {
            repository.deleteImage(imageId)
        }
    }

    fun updateProjectNotes(description: String) {
        viewModelScope.launch {
            val current = uiState.value.project ?: return@launch
            repository.updateProject(current.copy(description = description))
        }
    }
}
