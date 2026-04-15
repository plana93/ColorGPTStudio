package com.example.colorgptstudio.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorgptstudio.domain.model.Project
import com.example.colorgptstudio.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProjectListUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ProjectListViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    val uiState: StateFlow<ProjectListUiState> = repository
        .observeProjects()
        .map { projects -> ProjectListUiState(projects = projects, isLoading = false) }
        .catch { e -> emit(ProjectListUiState(isLoading = false, error = e.message)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProjectListUiState()
        )

    fun createProject(name: String, description: String) {
        viewModelScope.launch {
            repository.createProject(name, description)
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
        }
    }
}
