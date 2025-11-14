package com.classroom.quizmaster.ui.teacher.topics.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class CreateTopicUiState(
    val name: String = "",
    val description: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class CreateTopicViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classroomId: String = savedStateHandle[CLASSROOM_ID_KEY]
        ?: throw IllegalArgumentException("classroomId missing")

    private val _uiState = MutableStateFlow(CreateTopicUiState())
    val uiState: StateFlow<CreateTopicUiState> = _uiState

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun save(onSuccess: () -> Unit) {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Topic name is required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val now = Clock.System.now()
            val topic = Topic(
                id = "",
                classroomId = classroomId,
                teacherId = "",
                name = name,
                description = _uiState.value.description.trim(),
                createdAt = now,
                updatedAt = now
            )
            runCatching { classroomRepository.upsertTopic(topic) }
                .onSuccess {
                    _uiState.update { it.copy(success = true, isSaving = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Unable to save topic",
                            isSaving = false
                        )
                    }
                }
        }
    }

    companion object {
        const val CLASSROOM_ID_KEY = "classroomId"
    }
}

