package com.classroom.quizmaster.ui.materials.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.LearningMaterial
import com.classroom.quizmaster.domain.repository.LearningMaterialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MaterialDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val learningMaterialRepository: LearningMaterialRepository
) : ViewModel() {

    private val materialId: String = savedStateHandle.get<String>("materialId").orEmpty()
    private val _uiState = MutableStateFlow(MaterialDetailUiState(isLoading = true))
    val uiState: StateFlow<MaterialDetailUiState> = _uiState

    private val _effects = MutableStateFlow<MaterialDetailEffect?>(null)
    val effects: StateFlow<MaterialDetailEffect?> = _effects

    init {
        viewModelScope.launch {
            learningMaterialRepository.observeMaterial(materialId)
                .collect { material ->
                    _uiState.value = MaterialDetailUiState(material = material, isLoading = false)
                }
        }
    }

    fun clearEffect() {
        _effects.value = null
    }

    fun shareWithStudents() {
        val material = _uiState.value.material ?: return
        viewModelScope.launch {
            runCatching { learningMaterialRepository.shareSnapshotForClassroom(material.classroomId) }
                .onSuccess { _effects.value = MaterialDetailEffect.Message("Shared with nearby students") }
                .onFailure { err ->
                    _effects.value = MaterialDetailEffect.Message(err.message ?: "Unable to share materials")
                }
        }
    }

    fun archiveMaterial() {
        val material = _uiState.value.material ?: return
        viewModelScope.launch {
            runCatching { learningMaterialRepository.archive(material.id) }
                .onSuccess { _effects.value = MaterialDetailEffect.Archived }
                .onFailure { err ->
                    _effects.value = MaterialDetailEffect.Message(err.message ?: "Unable to archive material")
                }
        }
    }
}

data class MaterialDetailUiState(
    val material: LearningMaterial? = null,
    val isLoading: Boolean = false
)

sealed interface MaterialDetailEffect {
    data class Message(val text: String) : MaterialDetailEffect
    data object Archived : MaterialDetailEffect
}
