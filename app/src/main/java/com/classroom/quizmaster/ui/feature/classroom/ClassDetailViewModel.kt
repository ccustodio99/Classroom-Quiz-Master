package com.classroom.quizmaster.ui.feature.classroom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.domain.model.ClassroomProfile
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.ui.feature.dashboard.ModuleSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClassDetailViewModel(
    private val container: AppContainer,
    private val classroomId: String,
    private val teacherId: String?
) : ViewModel() {

    private val classroomAgent = container.classroomAgent

    private val _uiState = MutableStateFlow(ClassDetailUiState(isLoading = true))
    val uiState: StateFlow<ClassDetailUiState> = _uiState.asStateFlow()

    init {
        observeModules()
        refreshClassroom()
    }

    fun refreshClassroom() {
        viewModelScope.launch {
            val profile = classroomAgent.fetchClassroom(classroomId)
            _uiState.update { state ->
                state.copy(
                    classroom = profile,
                    isTeacherOwner = profile?.ownerId == teacherId,
                    isLoading = false,
                    message = if (profile == null) "Classroom not found" else state.message
                )
            }
        }
    }

    private fun observeModules() {
        viewModelScope.launch {
            container.moduleRepository.observeModulesByClassroom(classroomId).collect { modules ->
                _uiState.update { state ->
                    state.copy(
                        modules = modules.map { it.toSummary() },
                        message = if (modules.isEmpty()) {
                            "No module posted yet"
                        } else state.message
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun Module.toSummary(): ModuleSummary = ModuleSummary(
        id = id,
        topic = topic,
        objectives = objectives,
        attempts = null,
        preAverage = null,
        postAverage = null
    )
}

data class ClassDetailUiState(
    val classroom: ClassroomProfile? = null,
    val modules: List<ModuleSummary> = emptyList(),
    val isTeacherOwner: Boolean = false,
    val isLoading: Boolean = true,
    val message: String? = null
)
