package com.classroom.quizmaster.ui.teacher.quiz_editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.model.SelectionOptionUi
import com.classroom.quizmaster.ui.state.QuizRepositoryUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class QuizEditorUiState(
    val quizId: String? = null,
    val classroomId: String = "",
    val topicId: String = "",
    val title: String = "",
    val grade: String = "",
    val subject: String = "",
    val questions: List<QuestionDraftUi> = emptyList(),
    val classroomOptions: List<SelectionOptionUi> = emptyList(),
    val topicsByClassroom: Map<String, List<SelectionOptionUi>> = emptyMap(),
    val timePerQuestionSeconds: Int = 45,
    val shuffleQuestions: Boolean = true,
    val lastSavedRelative: String = "Just now",
    val showSaveDialog: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val isNewQuiz: Boolean = true
)

@HiltViewModel
class QuizEditorViewModel @Inject constructor(
    private val quizRepositoryUi: QuizRepositoryUi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String? = savedStateHandle[QUIZ_ID_KEY]

    private val _uiState = MutableStateFlow(QuizEditorUiState(quizId = quizId))
    val uiState: StateFlow<QuizEditorUiState> = _uiState

    private var hasPrimedState = false

    init {
        viewModelScope.launch {
            quizRepositoryUi.quizEditorState(quizId).collect { draft ->
                _uiState.update { current ->
                    if (!hasPrimedState) {
                        hasPrimedState = true
                        draft
                    } else {
                        current.mergeHierarchy(draft)
                    }
                }
            }
        }
    }

    fun updateTitle(value: String) = update { it.copy(title = value) }
    fun updateClassroom(classroomId: String) = update {
        val topics = it.topicsByClassroom[classroomId].orEmpty()
        it.copy(
            classroomId = classroomId,
            topicId = topics.firstOrNull()?.id.orEmpty()
        )
    }
    fun updateTopic(topicId: String) = update { it.copy(topicId = topicId) }
    fun updateGrade(value: String) = update { it.copy(grade = value) }
    fun updateSubject(value: String) = update { it.copy(subject = value) }
    fun updateTimePerQuestion(value: Int) =
        update { it.copy(timePerQuestionSeconds = value.coerceIn(15, 120)) }

    fun toggleShuffle(value: Boolean) = update { it.copy(shuffleQuestions = value) }

    fun addQuestion(type: QuestionTypeUi) = update { state ->
        val newId = "q${Random.nextInt(1000, 9999)}"
        val baseAnswers = when (type) {
            QuestionTypeUi.MultipleChoice -> listOf("A", "B", "C", "D")
            QuestionTypeUi.TrueFalse -> listOf("True", "False")
            QuestionTypeUi.FillIn -> listOf("Answer")
            QuestionTypeUi.Match -> listOf("Pair 1", "Pair 2")
        }.mapIndexed { index, label ->
            AnswerOptionUi(
                id = "${newId}_$index",
                label = "${('A' + index)}",
                text = label,
                correct = index == 0
            )
        }
        val newQuestion = QuestionDraftUi(
            id = newId,
            stem = "New ${type.name} question",
            type = type,
            answers = baseAnswers,
            explanation = ""
        )
        state.copy(questions = state.questions + newQuestion)
    }

    fun reorderQuestion(fromIndex: Int, toIndex: Int) = update { state ->
        val questions = state.questions.toMutableList()
        if (fromIndex in questions.indices && toIndex in questions.indices) {
            val item = questions.removeAt(fromIndex)
            questions.add(toIndex, item)
        }
        state.copy(questions = questions)
    }

    fun updateQuestionStem(questionId: String, stem: String) = updateQuestions(questionId) { it.copy(stem = stem) }

    fun updateAnswerText(questionId: String, answerId: String, text: String) = updateQuestions(questionId) { q ->
        q.copy(answers = q.answers.map { ans ->
            if (ans.id == answerId) ans.copy(text = text) else ans
        })
    }

    fun toggleCorrectAnswer(questionId: String, answerId: String) =
        updateQuestions(questionId) { q ->
            val updated = q.answers.map { ans ->
                if (ans.id == answerId) {
                    ans.copy(correct = !ans.correct)
                } else if (q.type == QuestionTypeUi.TrueFalse || q.type == QuestionTypeUi.MultipleChoice) {
                    ans.copy(correct = false)
                } else ans
            }
            q.copy(answers = updated)
        }

    fun updateExplanation(questionId: String, explanation: String) =
        updateQuestions(questionId) { it.copy(explanation = explanation) }

    fun showSaveDialog(show: Boolean) = update { it.copy(showSaveDialog = show) }
    fun showDiscardDialog(show: Boolean) = update { it.copy(showDiscardDialog = show) }

    fun persist(onComplete: () -> Unit) {
        viewModelScope.launch {
            val current = _uiState.value
            quizRepositoryUi.persistDraft(current)
            _uiState.update {
                it.copy(
                    showSaveDialog = false,
                    lastSavedRelative = "Moments ago"
                )
            }
            onComplete()
        }
    }

    private fun update(reducer: (QuizEditorUiState) -> QuizEditorUiState) {
        _uiState.update(reducer)
    }

    private fun updateQuestions(
        questionId: String,
        transform: (QuestionDraftUi) -> QuestionDraftUi
    ) = update { state ->
        state.copy(
            questions = state.questions.map { question ->
                if (question.id == questionId) transform(question) else question
            }
        )
    }

    private fun QuizEditorUiState.mergeHierarchy(incoming: QuizEditorUiState): QuizEditorUiState {
        val resolvedClassroomId = when {
            incoming.classroomOptions.any { it.id == classroomId } -> classroomId
            incoming.classroomOptions.any { it.id == incoming.classroomId } -> incoming.classroomId
            else -> incoming.classroomOptions.firstOrNull()?.id.orEmpty()
        }
        val resolvedTopicsByClassroom = incoming.topicsByClassroom
        val topicsForClassroom = resolvedTopicsByClassroom[resolvedClassroomId].orEmpty()
        val resolvedTopicId = when {
            topicsForClassroom.any { it.id == topicId } -> topicId
            topicsForClassroom.any { it.id == incoming.topicId } -> incoming.topicId
            else -> topicsForClassroom.firstOrNull()?.id.orEmpty()
        }

        return copy(
            quizId = quizId ?: incoming.quizId,
            classroomId = resolvedClassroomId,
            topicId = resolvedTopicId,
            classroomOptions = incoming.classroomOptions,
            topicsByClassroom = resolvedTopicsByClassroom,
            grade = grade.ifBlank { incoming.grade },
            subject = subject.ifBlank { incoming.subject },
            isNewQuiz = isNewQuiz && incoming.isNewQuiz
        )
    }

    companion object {
        const val QUIZ_ID_KEY = "quizId"
    }
}
