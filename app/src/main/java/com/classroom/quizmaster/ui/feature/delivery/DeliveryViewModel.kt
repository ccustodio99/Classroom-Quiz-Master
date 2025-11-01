package com.classroom.quizmaster.ui.feature.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.AppContainer
import com.classroom.quizmaster.agents.AnswerPayload
import com.classroom.quizmaster.domain.model.Badge
import com.classroom.quizmaster.domain.model.Item
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.model.StudentReport
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeliveryViewModel(
    private val container: AppContainer,
    private val moduleId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryUiState())
    val uiState: StateFlow<DeliveryUiState> = _uiState

    private var module: Module? = null
    private var student: Student? = null
    private var lessonSessionId: String? = null
    private var lessonIndex: Int = 0

    init {
        viewModelScope.launch {
            module = container.moduleRepository.getModule(moduleId)
            _uiState.value = DeliveryUiState(stage = Stage.CaptureStudent)
        }
    }

    fun onStudentNameChanged(value: String) {
        val current = _uiState.value
        _uiState.value = current.copy(studentName = value)
    }

    fun beginPreTest() {
        val module = module ?: return
        val name = _uiState.value.studentName.ifBlank { "Learner" }
        val student = Student(id = UUID.randomUUID().toString(), displayName = name)
        this.student = student
        viewModelScope.launch {
            val attemptId = container.assessmentAgent.start(module.preTest.id, student)
            _uiState.value = DeliveryUiState(
                studentName = name,
                stage = Stage.AssessmentStage(
                    kind = AssessmentKind.PRE,
                    attemptId = attemptId,
                    questions = module.preTest.items.map { QuestionState(item = it) }
                )
            )
        }
    }

    fun updateAnswer(index: Int, answer: String) {
        val stage = _uiState.value.stage
        if (stage is Stage.AssessmentStage) {
            val updated = stage.questions.toMutableList()
            updated[index] = updated[index].copy(answer = answer)
            _uiState.value = _uiState.value.copy(
                stage = stage.copy(questions = updated)
            )
        }
    }

    fun nextQuestion() {
        val stage = _uiState.value.stage
        if (stage is Stage.AssessmentStage) {
            val nextIndex = (stage.currentIndex + 1).coerceAtMost(stage.questions.lastIndex)
            _uiState.value = _uiState.value.copy(stage = stage.copy(currentIndex = nextIndex))
        }
    }

    fun previousQuestion() {
        val stage = _uiState.value.stage
        if (stage is Stage.AssessmentStage) {
            val nextIndex = (stage.currentIndex - 1).coerceAtLeast(0)
            _uiState.value = _uiState.value.copy(stage = stage.copy(currentIndex = nextIndex))
        }
    }

    fun submitAssessment() {
        val stage = _uiState.value.stage
        val module = module ?: return
        val student = student ?: return
        if (stage !is Stage.AssessmentStage) return
        viewModelScope.launch {
            val payload = stage.questions.map { AnswerPayload(it.item.id, it.answer, student.id) }
            val scorecard = container.assessmentAgent.submit(stage.attemptId, payload)
            if (stage.kind == AssessmentKind.PRE) {
                _uiState.value = _uiState.value.copy(prePercent = scorecard.percent)
                startLesson()
            } else {
                _uiState.value = _uiState.value.copy(postPercent = scorecard.percent)
                showSummary()
            }
        }
    }

    private suspend fun startLesson() {
        val module = module ?: return
        val sessionId = container.lessonAgent.start(module.lesson.id)
        lessonSessionId = sessionId
        lessonIndex = 0
        goToNextLessonStep()
    }

    fun goToNextLessonStep() {
        val sessionId = lessonSessionId ?: return
        val module = module ?: return
        val step = container.lessonAgent.next(sessionId)
        if (step.finished && lessonIndex >= module.lesson.slides.size) {
            startPostTest()
        } else {
            lessonIndex += 1
            _uiState.value = _uiState.value.copy(
                stage = Stage.LessonStage(
                    sessionId = sessionId,
                    slideIndex = lessonIndex,
                    totalSlides = module.lesson.slides.size,
                    slideTitle = step.slideTitle,
                    slideContent = step.slideContent,
                    miniCheckPrompt = step.miniCheckPrompt,
                    finished = step.finished
                )
            )
        }
    }

    fun submitMiniCheck(answer: String) {
        val stage = _uiState.value.stage
        if (stage is Stage.LessonStage) {
            val correct = container.lessonAgent.recordCheck(stage.sessionId, answer)
            _uiState.value = _uiState.value.copy(
                stage = stage.copy(miniCheckAnswer = answer, miniCheckResult = correct)
            )
        }
    }

    private fun startPostTest() {
        val module = module ?: return
        val student = student ?: return
        viewModelScope.launch {
            val attemptId = container.assessmentAgent.start(module.postTest.id, student)
            _uiState.value = _uiState.value.copy(
                stage = Stage.AssessmentStage(
                    kind = AssessmentKind.POST,
                    attemptId = attemptId,
                    questions = module.postTest.items.map { QuestionState(it) }
                )
            )
        }
    }

    private fun showSummary() {
        val module = module ?: return
        val student = student ?: return
        viewModelScope.launch {
            val report = container.scoringAnalyticsAgent.buildStudentReport(module.id, student.id)
            val classReport = container.scoringAnalyticsAgent.buildClassReport(module.id)
            container.gamificationAgent.onReportsAvailable(classReport)
            val badges = container.gamificationAgent.unlocksFor(student.id)
            _uiState.value = _uiState.value.copy(
                stage = Stage.Summary(
                    pre = _uiState.value.prePercent ?: 0.0,
                    post = _uiState.value.postPercent ?: 0.0,
                    report = report,
                    badges = badges
                )
            )
        }
    }
}

data class DeliveryUiState(
    val studentName: String = "",
    val prePercent: Double? = null,
    val postPercent: Double? = null,
    val stage: Stage = Stage.Loading
)

data class QuestionState(
    val item: Item,
    val answer: String = ""
)

enum class AssessmentKind { PRE, POST }

sealed class Stage {
    data object Loading : Stage()
    data object CaptureStudent : Stage()
    data class AssessmentStage(
        val kind: AssessmentKind,
        val attemptId: String,
        val questions: List<QuestionState>,
        val currentIndex: Int = 0
    ) : Stage()
    data class LessonStage(
        val sessionId: String,
        val slideIndex: Int,
        val totalSlides: Int,
        val slideTitle: String,
        val slideContent: String,
        val miniCheckPrompt: String?,
        val miniCheckAnswer: String = "",
        val miniCheckResult: Boolean? = null,
        val finished: Boolean = false
    ) : Stage()
    data class Summary(
        val pre: Double,
        val post: Double,
        val report: StudentReport?,
        val badges: List<Badge>
    ) : Stage()
}
