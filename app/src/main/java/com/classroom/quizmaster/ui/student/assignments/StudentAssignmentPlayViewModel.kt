package com.classroom.quizmaster.ui.student.assignments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.usecase.SubmitAssignmentAttemptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AssignmentPlayUiState(
    val assignmentId: String = "",
    val question: Question? = null,
    val index: Int = 0,
    val total: Int = 0,
    val selected: Set<String> = emptySet(),
    val freeResponse: String = "",
    val finished: Boolean = false,
    val score: Int = 0,
    val error: String? = null,
    val loading: Boolean = true
)

@HiltViewModel
class StudentAssignmentPlayViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val quizRepository: QuizRepository,
    private val assignmentRepository: AssignmentRepository,
    private val authRepository: AuthRepository,
    private val submitAssignmentAttemptUseCase: SubmitAssignmentAttemptUseCase
) : ViewModel() {

    private val assignmentId: String = savedStateHandle["assignmentId"] ?: ""
    private val currentIndex = MutableStateFlow(0)
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    private val freeResponse = MutableStateFlow("")
    private val finished = MutableStateFlow(false)
    private val score = MutableStateFlow(0)
    private val error = MutableStateFlow<String?>(null)

    private val quizFlow = assignmentRepository.assignments
        .combine(quizRepository.quizzes) { assignments, quizzes ->
            val assignment = assignments.firstOrNull { it.id == assignmentId }
            val quiz = quizzes.firstOrNull { it.id == assignment?.quizId }
            quiz?.questions.orEmpty()
        }

    val uiState: StateFlow<AssignmentPlayUiState> =
        combine(
            quizFlow,
            currentIndex,
            selected,
            freeResponse,
            finished,
            score,
            error
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val questions = values[0] as List<Question>
            val idx = values[1] as Int
            val selectedChoices = when (val raw = values[2]) {
                is Set<*> -> raw.filterIsInstance<String>().toSet()
                else -> emptySet()
            }
            val freeText = values[3] as String
            val done = values[4] as Boolean
            val points = values[5] as Int
            val err = values[6] as String?
            val safeIndex = idx.coerceIn(0, (questions.size - 1).coerceAtLeast(0))
            AssignmentPlayUiState(
                assignmentId = assignmentId,
                question = questions.getOrNull(safeIndex),
                index = safeIndex + 1,
                total = questions.size,
                selected = selectedChoices,
                freeResponse = freeText,
                finished = done,
                score = points,
                error = err,
                loading = questions.isEmpty()
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AssignmentPlayUiState(assignmentId = assignmentId)
        )

    fun toggleChoice(choice: String) {
        val question = uiState.value.question
        if (question?.type == QuestionType.FILL_IN) return
        selected.value = if (selected.value.contains(choice)) {
            selected.value - choice
        } else {
            selected.value + choice
        }
    }

    fun updateFreeResponse(value: String) {
        freeResponse.value = value
    }

    fun submitAndNext() {
        viewModelScope.launch {
            val state = uiState.value
            val question = state.question ?: return@launch
            val correct = question.answerKey.map { it.trim().lowercase() }.toSet()
            val chosen = selected.value
            val userAnswerIsCorrect = when (question.type) {
                QuestionType.MCQ, QuestionType.TF -> chosen.map { it.trim().lowercase() }.toSet() == correct
                QuestionType.FILL_IN -> {
                    val response = freeResponse.value.trim().lowercase()
                    response.isNotEmpty() && correct.contains(response)
                }
                else -> false
            }
            if (userAnswerIsCorrect) score.value = score.value + 1
            val nextIndex = state.index
            val total = state.total
            if (nextIndex >= total) {
                finish()
            } else {
                currentIndex.value = nextIndex
                selected.value = emptySet()
                freeResponse.value = ""
            }
        }
    }

    private suspend fun finish() {
        finished.value = true
        val userId = authRepository.authState.first().userId ?: return
        runCatching { submitAssignmentAttemptUseCase(assignmentId, userId, score.value) }
            .onFailure { error.value = it.message ?: "Unable to submit score" }
    }
}
