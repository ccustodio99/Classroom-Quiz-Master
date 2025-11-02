package com.classroom.quizmaster.ui.feature.classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.domain.model.ClassroomStatus
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClassroomViewModel(
    private val container: AppContainer,
    private val teacherId: String
) : ViewModel() {

    private val classroomAgent = container.classroomAgent

    private val _uiState = MutableStateFlow(ClassroomUiState())
    val uiState: StateFlow<ClassroomUiState> = _uiState.asStateFlow()

    private var classroomsJob: Job? = null

    init {
        observeClassrooms(includeArchived = false)
    }

    private fun observeClassrooms(includeArchived: Boolean) {
        classroomsJob?.cancel()
        classroomsJob = viewModelScope.launch {
            classroomAgent.observeClassrooms(includeArchived).collectLatest { profiles ->
                val filtered = profiles.filter { it.ownerId == teacherId }
                _uiState.update { state ->
                    state.copy(
                        classrooms = filtered,
                        showArchived = includeArchived,
                        message = if (filtered.isEmpty() && !includeArchived) {
                            "Create your first classroom to get started."
                        } else state.message
                    )
                }
            }
        }
    }

    fun toggleArchived(showArchived: Boolean) {
        if (showArchived == _uiState.value.showArchived) return
        observeClassrooms(showArchived)
    }

    fun startCreate() {
        _uiState.update {
            it.copy(
                editor = ClassroomEditorState(
                    id = UUID.randomUUID().toString(),
                    ownerId = teacherId
                )
            )
        }
    }

    fun startEdit(profile: ClassroomProfile) {
        _uiState.update {
            it.copy(
                editor = ClassroomEditorState(
                    id = profile.id,
                    ownerId = profile.ownerId ?: teacherId,
                    name = profile.name,
                    description = profile.description,
                    gradeLevel = profile.gradeLevel,
                    section = profile.section,
                    subject = profile.subject,
                    archived = profile.status == ClassroomStatus.Archived
                )
            )
        }
    }

    fun updateEditor(state: ClassroomEditorState) {
        _uiState.update { current ->
            if (current.editor == null) current else current.copy(editor = state)
        }
    }

    fun saveClassroom() {
        val editor = _uiState.value.editor ?: return
        val profile = ClassroomProfile(
            id = editor.id,
            name = editor.name.trim(),
            subject = editor.subject.trim().ifBlank { "Subject" },
            description = editor.description.trim(),
            gradeLevel = editor.gradeLevel.trim().ifBlank { "Grade 11" },
            section = editor.section.trim(),
            ownerId = editor.ownerId.ifBlank { teacherId },
            status = if (editor.archived) ClassroomStatus.Archived else ClassroomStatus.Active
        )
        viewModelScope.launch {
            val result = classroomAgent.createOrUpdate(profile)
            _uiState.update { state ->
                state.copy(
                    editor = null,
                    message = result.fold(
                        onSuccess = { "Saved ${profile.name}" },
                        onFailure = { it.localizedMessage ?: "Unable to save" }
                    )
                )
            }
        }
    }

    fun setArchived(classroomId: String, archived: Boolean) {
        viewModelScope.launch {
            val result = classroomAgent.setArchived(classroomId, archived)
            _uiState.update { state ->
                state.copy(
                    message = result.fold(
                        onSuccess = { if (archived) "Class archived" else "Class restored" },
                        onFailure = { it.localizedMessage ?: "Update failed" }
                    )
                )
            }
        }
    }

    fun dismissEditor() {
        _uiState.update { it.copy(editor = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class ClassroomUiState(
    val classrooms: List<ClassroomProfile> = emptyList(),
    val showArchived: Boolean = false,
    val editor: ClassroomEditorState? = null,
    val message: String? = null
)

data class ClassroomEditorState(
    val id: String,
    val ownerId: String,
    val name: String = "",
    val description: String = "",
    val gradeLevel: String = "",
    val section: String = "",
    val subject: String = "",
    val archived: Boolean = false
)
