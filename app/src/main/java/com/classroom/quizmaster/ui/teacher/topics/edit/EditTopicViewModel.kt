package com.classroom.quizmaster.ui.teacher.topics.edit

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

@HiltViewModel
class EditTopicViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classroomId: String = savedStateHandle[CLASSROOM_ID_KEY]
        ?: throw IllegalArgumentException("classroomId missing")
    private val topicId: String = savedStateHandle[TOPIC_ID_KEY]
        ?: throw IllegalArgumentException("topicId missing")

    private val _uiState = MutableStateFlow(EditTopicUiState())
    val uiState: StateFlow<EditTopicUiState> = _uiState

    private var loadedTopic: Topic? = null

    init {
        loadTopic()
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun save() {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Topic name is required") }
            return
        }
        val currentTopic = loadedTopic
        if (currentTopic == null) {
            _uiState.update { it.copy(errorMessage = "Topic not available") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val now = Clock.System.now()
            val updatedTopic = currentTopic.copy(
                name = name,
                description = _uiState.value.description.trim(),
                updatedAt = now
            )
            runCatching { classroomRepository.upsertTopic(updatedTopic) }
                .onSuccess { id ->
                    loadedTopic = updatedTopic.copy(id = id)
                    _uiState.update { it.copy(isSaving = false, success = true) }
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

    fun archive() {
        val resolvedId = loadedTopic?.id ?: topicId
        viewModelScope.launch {
            _uiState.update { it.copy(isArchiving = true, errorMessage = null) }
            val archivedAt = Clock.System.now()
            runCatching { classroomRepository.archiveTopic(resolvedId, archivedAt) }
                .onSuccess {
                    _uiState.update { it.copy(isArchiving = false, archived = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Unable to archive topic",
                            isArchiving = false
                        )
                    }
                }
        }
    }

    private fun loadTopic() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { classroomRepository.getTopic(topicId) }
                .onSuccess { topic ->
                    if (topic == null || topic.classroomId != classroomId) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Topic not available"
                            )
                        }
                    } else {
                        loadedTopic = topic
                        _uiState.update {
                            it.copy(
                                name = topic.name,
                                description = topic.description,
                                isLoading = false
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = error.message ?: "Unable to load topic",
                            isLoading = false
                        )
                    }
                }
        }
    }

    companion object {
        const val CLASSROOM_ID_KEY = "classroomId"
        const val TOPIC_ID_KEY = "topicId"
    }
}

data class EditTopicUiState(
    val name: String = "",
    val description: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isArchiving: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false,
    val archived: Boolean = false
)
