package com.classroom.quizmaster.ui.teacher.assignments.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.QuizCategory
import com.classroom.quizmaster.domain.model.ScoringMode
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.usecase.UpsertAssignmentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

@HiltViewModel
class AssignmentEditorViewModel @Inject constructor(
    private val assignmentRepository: AssignmentRepository,
    private val quizRepository: QuizRepository,
    private val upsertAssignmentUseCase: UpsertAssignmentUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val assignmentId: String? = savedStateHandle[ASSIGNMENT_ID_KEY]
    private val initialClassroomId: String? = savedStateHandle[CLASSROOM_ID_KEY]
    private val initialTopicId: String? = savedStateHandle[TOPIC_ID_KEY]

    private val mode = if (assignmentId.isNullOrBlank()) EditorMode.Create else EditorMode.Edit

    private val classroomId = MutableStateFlow(initialClassroomId)
    private val topicId = MutableStateFlow(initialTopicId)
    private var existingCreatedAt: Instant? = null

    private val _uiState = MutableStateFlow(
        AssignmentEditorUiState(
            mode = mode,
            isLoading = true,
            canArchive = mode == EditorMode.Edit
        )
    )
    val uiState: StateFlow<AssignmentEditorUiState> = _uiState

    init {
        when (mode) {
            EditorMode.Create -> {
                require(!initialClassroomId.isNullOrBlank() && !initialTopicId.isNullOrBlank()) {
                    "classroomId and topicId required for creation"
                }
            }
            EditorMode.Edit -> require(!assignmentId.isNullOrBlank()) { "assignmentId required for editing" }
        }
        viewModelScope.launch { observeQuizzes() }
        viewModelScope.launch { loadAssignmentIfNeeded() }
    }

    private suspend fun observeQuizzes() {
        combine(quizRepository.quizzes, classroomId, topicId) { quizzes, classroomId, topicId ->
            if (classroomId.isNullOrBlank() || topicId.isNullOrBlank()) {
                emptyList()
            } else {
                quizzes
                    .filter { quiz ->
                        !quiz.isArchived &&
                            quiz.classroomId == classroomId &&
                            quiz.topicId == topicId &&
                            quiz.category == QuizCategory.STANDARD
                    }
                    .sortedByDescending { it.updatedAt }
                    .map { quiz ->
                        QuizOptionUi(
                            id = quiz.id,
                            title = quiz.title.ifBlank { "Untitled quiz" },
                            subtitle = "${quiz.questionCount} questions"
                        )
                    }
            }
        }.collect { options ->
            _uiState.update { state ->
                val resolvedSelection = when {
                    options.any { it.id == state.selectedQuizId } -> state.selectedQuizId
                    state.selectedQuizId.isNotBlank() -> state.selectedQuizId
                    else -> options.firstOrNull()?.id.orEmpty()
                }
                state.copy(quizOptions = options, selectedQuizId = resolvedSelection)
            }
        }
    }

    private suspend fun loadAssignmentIfNeeded() {
        if (mode == EditorMode.Edit) {
            val assignment = assignmentRepository.getAssignment(assignmentId!!)
            if (assignment == null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Assignment not available") }
                return
            }
            existingCreatedAt = assignment.createdAt
            classroomId.value = assignment.classroomId
            topicId.value = assignment.topicId
            _uiState.update {
                it.copy(
                    selectedQuizId = assignment.quizId,
                    openAt = assignment.openAt,
                    closeAt = assignment.closeAt,
                    attemptsAllowed = assignment.attemptsAllowed.toString(),
                    scoringMode = assignment.scoringMode,
                    revealAfterSubmit = assignment.revealAfterSubmit,
                    isLoading = false,
                    errorMessage = null
                )
            }
        } else {
            setDefaultDates()
        }
    }

    private suspend fun setDefaultDates() {
        val now = Clock.System.now()
        _uiState.update {
            it.copy(
                openAt = now,
                closeAt = now + 7.days,
                isLoading = false
            )
        }
    }

    fun updateQuiz(quizId: String) {
        _uiState.update { it.copy(selectedQuizId = quizId, errorMessage = null) }
    }

    fun updateAttempts(value: String) {
        _uiState.update { it.copy(attemptsAllowed = value.filter { ch -> ch.isDigit() }, errorMessage = null) }
    }

    fun updateScoringMode(mode: ScoringMode) {
        _uiState.update { it.copy(scoringMode = mode, errorMessage = null) }
    }

    fun updateRevealAfterSubmit(reveal: Boolean) {
        _uiState.update { it.copy(revealAfterSubmit = reveal) }
    }

    fun updateOpenAt(instant: Instant) {
        _uiState.update { it.copy(openAt = instant, errorMessage = null) }
    }

    fun updateCloseAt(instant: Instant) {
        _uiState.update { it.copy(closeAt = instant, errorMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        val quizId = state.selectedQuizId
        val openAt = state.openAt
        val closeAt = state.closeAt
        val attempts = state.attemptsAllowed.toIntOrNull() ?: 0
        val classroomId = classroomId.value
        val topicId = topicId.value
        when {
            quizId.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "Select a quiz to assign") }
                return
            }
            attempts <= 0 -> {
                _uiState.update { it.copy(errorMessage = "Attempts allowed must be at least 1") }
                return
            }
            openAt == null || closeAt == null -> {
                _uiState.update { it.copy(errorMessage = "Provide both open and close times") }
                return
            }
            classroomId.isNullOrBlank() || topicId.isNullOrBlank() -> {
                _uiState.update { it.copy(errorMessage = "Missing classroom or topic context") }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val params = UpsertAssignmentUseCase.Params(
                quizId = quizId,
                classroomId = classroomId,
                topicId = topicId,
                openAt = openAt,
                closeAt = closeAt,
                attemptsAllowed = attempts,
                scoringMode = state.scoringMode,
                revealAfterSubmit = state.revealAfterSubmit,
                assignmentId = assignmentId,
                createdAt = existingCreatedAt
            )
            runCatching { upsertAssignmentUseCase(params) }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Unable to save assignment"
                        )
                    }
                }
        }
    }

    fun archive() {
        val targetId = assignmentId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching { assignmentRepository.archiveAssignment(targetId) }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, archiveSuccess = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Unable to archive assignment"
                        )
                    }
                }
        }
    }

    companion object {
        const val ASSIGNMENT_ID_KEY = "assignmentId"
        const val CLASSROOM_ID_KEY = "classroomId"
        const val TOPIC_ID_KEY = "topicId"
    }
}

enum class EditorMode { Create, Edit }

data class QuizOptionUi(
    val id: String,
    val title: String,
    val subtitle: String
)

data class AssignmentEditorUiState(
    val mode: EditorMode = EditorMode.Create,
    val quizOptions: List<QuizOptionUi> = emptyList(),
    val selectedQuizId: String = "",
    val openAt: Instant? = null,
    val closeAt: Instant? = null,
    val attemptsAllowed: String = "1",
    val scoringMode: ScoringMode = ScoringMode.BEST,
    val revealAfterSubmit: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val archiveSuccess: Boolean = false,
    val canArchive: Boolean = false
)
