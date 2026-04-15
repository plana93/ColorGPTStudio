package com.example.colorgptstudio.ui.analysis

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorgptstudio.data.tags.TagRepository
import com.example.colorgptstudio.domain.model.ColorData
import com.example.colorgptstudio.domain.model.ColorPalette
import com.example.colorgptstudio.domain.model.ColorPoint
import com.example.colorgptstudio.domain.repository.ProjectRepository
import com.example.colorgptstudio.domain.usecase.ExtractPaletteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AnalysisUiState(
    val imageLocalPath: String = "",
    val colorPoints: List<ColorPoint> = emptyList(),
    val palette: ColorPalette? = null,
    val selectedPoint: ColorPoint? = null,
    val isExtractingPalette: Boolean = false,
    val isSaving: Boolean = false,
    val projectId: Long = 0L,
    val imageId: Long = 0L,
    val error: String? = null
)

class AnalysisViewModel(
    private val projectId: Long,
    private val imageId: Long,
    private val repository: ProjectRepository,
    private val extractPaletteUseCase: ExtractPaletteUseCase,
    val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalysisUiState(projectId = projectId, imageId = imageId))
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        loadImage()
    }

    private fun loadImage() {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId) ?: return@launch
            val image = project.images.find { it.id == imageId } ?: return@launch
            _uiState.update {
                it.copy(
                    imageLocalPath = image.localPath,
                    colorPoints = image.colorPoints,
                    palette = image.palette
                )
            }
        }
    }

    fun onColorPicked(xRatio: Float, yRatio: Float, colorData: ColorData) {
        viewModelScope.launch {
            val newPoint = ColorPoint(
                imageId = imageId,
                xRatio = xRatio,
                yRatio = yRatio,
                color = colorData
            )
            val saved = repository.saveColorPoint(newPoint)
            _uiState.update { state ->
                state.copy(
                    colorPoints = state.colorPoints + saved,
                    selectedPoint = saved
                )
            }
        }
    }

    fun selectPoint(point: ColorPoint) {
        _uiState.update { it.copy(selectedPoint = point) }
    }

    fun deselectPoint() {
        _uiState.update { it.copy(selectedPoint = null) }
    }

    fun updatePoint(updated: ColorPoint) {
        viewModelScope.launch {
            repository.saveColorPoint(updated)
            // Salva i nuovi tag custom nel repository
            updated.tags.forEach { tag -> tagRepository.addCustomTag(tag) }
            _uiState.update { state ->
                state.copy(
                    colorPoints = state.colorPoints.map { if (it.id == updated.id) updated else it },
                    selectedPoint = if (state.selectedPoint?.id == updated.id) updated else state.selectedPoint
                )
            }
        }
    }

    fun deletePoint(pointId: Long) {
        viewModelScope.launch {
            repository.deleteColorPoint(pointId)
            _uiState.update { state ->
                state.copy(
                    colorPoints = state.colorPoints.filter { it.id != pointId },
                    selectedPoint = if (state.selectedPoint?.id == pointId) null else state.selectedPoint
                )
            }
        }
    }

    fun extractPalette(context: android.content.Context) {
        val path = _uiState.value.imageLocalPath
        if (path.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExtractingPalette = true) }
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeFile(path)
                }
                if (bitmap != null) {
                    val colors = extractPaletteUseCase(bitmap, k = 5)
                    val palette = ColorPalette(
                        projectId = projectId,
                        imageId = imageId,
                        colors = colors,
                        label = "Palette automatica"
                    )
                    repository.savePalette(palette)
                    _uiState.update { it.copy(palette = palette, isExtractingPalette = false) }
                } else {
                    _uiState.update { it.copy(isExtractingPalette = false, error = "Impossibile leggere l'immagine") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExtractingPalette = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Builds a ready-to-paste prompt for any local or cloud LLM.
     * The prompt includes all color points with their full color vocabulary
     * (HEX, RAL, NCS, RGB, label, note, tags) and asks for professional advice.
     */
    fun generateLlmPrompt(): String {
        val points = _uiState.value.colorPoints
        if (points.isEmpty()) return ""

        val colorLines = points.mapIndexed { index, point ->
            buildString {
                val name = point.label.ifBlank { "Color ${index + 1}" }
                appendLine("${index + 1}. $name")
                appendLine("   HEX: #${point.color.hexClean}  |  RGB: ${point.color.r}, ${point.color.g}, ${point.color.b}")
                appendLine("   RAL: ${point.color.ralString}  |  NCS: ${point.color.ncsApprox}")
                appendLine("   CMYK: ${point.color.cmykString}  |  HSL: ${point.color.hslString}")
                if (point.note.isNotBlank()) appendLine("   Note: ${point.note}")
                if (point.materialInfo.isNotBlank()) appendLine("   Material / product code: ${point.materialInfo}")
                if (point.tags.isNotEmpty()) appendLine("   Tags: ${point.tags.joinToString(", ")}")
            }.trimEnd()
        }.joinToString("\n\n")

        return """
You are a professional color consultant specializing in interior finishing, woodworking and architectural painting.

I have sampled the following colors from a project image:

$colorLines

Please provide:
1. A brief description of the overall palette mood and style
2. Color harmony analysis (complementary, analogous, contrast, etc.)
3. Suggested compatible colors or RAL/NCS codes that would pair well
4. Recommended paint finish types and application notes for these surfaces
5. Any potential issues or improvements from a professional standpoint

Keep the response concise and practical — this is for an on-site craftsman.
        """.trimIndent()
    }
}
