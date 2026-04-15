package com.example.colorgptstudio.ui.quickanalysis

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.colorgptstudio.domain.model.ColorData
import com.example.colorgptstudio.domain.model.ColorPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QuickAnalysisUiState(
    val imageUri: Uri? = null,
    val colorPoints: List<ColorPoint> = emptyList(),
    val selectedPoint: ColorPoint? = null,
    val selectedColor: ColorData? = null,
    val showColorDetail: Boolean = false
)

/**
 * ViewModel per la modalità "Analisi Rapida".
 * Completamente stateless rispetto alla persistenza: non salva nulla su DB.
 * Lo stato viene azzerato quando il ViewModel viene distrutto (navigazione indietro).
 */
class QuickAnalysisViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuickAnalysisUiState())
    val uiState: StateFlow<QuickAnalysisUiState> = _uiState.asStateFlow()

    fun onImageSelected(uri: Uri) {
        _uiState.update {
            it.copy(imageUri = uri, colorPoints = emptyList(), selectedColor = null)
        }
    }

    fun onColorPicked(xRatio: Float, yRatio: Float, colorData: ColorData) {
        val newPoint = ColorPoint(
            id = System.currentTimeMillis(),
            imageId = -1L,
            xRatio = xRatio,
            yRatio = yRatio,
            color = colorData
        )
        _uiState.update { state ->
            state.copy(
                colorPoints = state.colorPoints + newPoint,
                selectedPoint = newPoint,
                selectedColor = colorData,
                showColorDetail = true
            )
        }
    }

    fun selectPoint(point: ColorPoint) {
        _uiState.update { it.copy(selectedPoint = point, selectedColor = point.color) }
    }

    fun deselectPoint() {
        _uiState.update { it.copy(selectedPoint = null) }
    }

    fun removePoint(pointId: Long) {
        _uiState.update { state ->
            val updated = state.colorPoints.filter { it.id != pointId }
            state.copy(
                colorPoints = updated,
                selectedPoint = if (state.selectedPoint?.id == pointId) null else state.selectedPoint
            )
        }
    }

    fun updatePointNote(pointId: Long, note: String) {
        _uiState.update { state ->
            val updated = state.colorPoints.map {
                if (it.id == pointId) it.copy(note = note) else it
            }
            val updatedSelected = state.selectedPoint?.let {
                if (it.id == pointId) it.copy(note = note) else it
            }
            state.copy(colorPoints = updated, selectedPoint = updatedSelected)
        }
    }

    fun onColorDetailDismiss() {
        _uiState.update { it.copy(showColorDetail = false, selectedPoint = null) }
    }

    fun clearPoints() {
        _uiState.update { it.copy(colorPoints = emptyList(), selectedPoint = null, selectedColor = null) }
    }
}
