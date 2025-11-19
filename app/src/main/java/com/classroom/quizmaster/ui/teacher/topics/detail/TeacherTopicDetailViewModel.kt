package com.classroom.quizmaster.ui.teacher.topics.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.QuizCategory
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class TeacherTopicDetailViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    private val quizRepository: QuizRepository,
    private val assignmentRepository: AssignmentRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classroomIdArg: String = savedStateHandle[CLASSROOM_ID_KEY]
        ?: throw IllegalArgumentException("classroomId is required")
    private val topicIdArg: String = savedStateHandle[TOPIC_ID_KEY]
        ?: throw IllegalArgumentException("topicId is required")

    val classroomId: String
        get() = classroomIdArg

    val topicId: String
        get() = topicIdArg

    val uiState: StateFlow<TopicDetailUiState> =
        combine(
            classroomRepository.classrooms,
            classroomRepository.topics,
            quizRepository.quizzes,
            assignmentRepository.assignments
        ) { classrooms, topics, quizzes, assignments ->
            val classroom = classrooms.firstOrNull { it.id == classroomIdArg && !it.isArchived }
            val topic = topics.firstOrNull {
                it.id == topicIdArg &&
                    it.classroomId == classroomIdArg &&
                    !it.isArchived
            }
            if (classroom == null || topic == null) {
                TopicDetailUiState(
                    classroomId = classroomIdArg,
                    topicId = topicIdArg,
                    isLoading = false,
                    errorMessage = "Topic not available"
                )
            } else {
                val activeQuizzes = quizzes
                    .filter {
                        !it.isArchived &&
                            it.classroomId == classroomIdArg &&
                            it.topicId == topicIdArg &&
                            it.category == QuizCategory.STANDARD
                    }
                val topicQuizzes = activeQuizzes
                    .sortedByDescending { it.updatedAt }
                    .map { quiz ->
                        QuizSummaryUi(
                            id = quiz.id,
                            title = quiz.title.ifBlank { "Untitled quiz" },
                            questionCount = quiz.questionCount,
                            updatedAgo = formatRelativeTime(quiz.updatedAt)
                        )
                    }
                val assignmentsForTopic = assignments
                    .filter {
                        !it.isArchived &&
                            it.classroomId == classroomIdArg &&
                            it.topicId == topicIdArg
                    }
                    .sortedByDescending { it.openAt }
                    .map { assignment ->
                        val assignmentQuiz = activeQuizzes.firstOrNull { it.id == assignment.quizId }
                            ?: return@map null
                        AssignmentSummaryUi(
                            id = assignment.id,
                            quizId = assignment.quizId,
                            quizTitle = assignmentQuiz.title.ifBlank { "Untitled quiz" },
                            openAt = formatRelativeTime(assignment.openAt),
                            closeAt = formatRelativeTime(assignment.closeAt)
                        )
                    }
                    .filterNotNull()
                TopicDetailUiState(
                    classroomId = classroomIdArg,
                    topicId = topicIdArg,
                    classroomName = classroom.name,
                    topicName = topic.name,
                    topicDescription = topic.description,
                    quizzes = topicQuizzes,
                    assignments = assignmentsForTopic,
                    isLoading = false
                )
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TopicDetailUiState(
                    classroomId = classroomIdArg,
                    topicId = topicIdArg
                )
            )

    companion object {
        const val CLASSROOM_ID_KEY = "classroomId"
        const val TOPIC_ID_KEY = "topicId"
    }
}

data class TopicDetailUiState(
    val classroomId: String = "",
    val topicId: String = "",
    val classroomName: String = "",
    val topicName: String = "",
    val topicDescription: String = "",
    val quizzes: List<QuizSummaryUi> = emptyList(),
    val assignments: List<AssignmentSummaryUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class QuizSummaryUi(
    val id: String,
    val title: String,
    val questionCount: Int,
    val updatedAgo: String
)

data class AssignmentSummaryUi(
    val id: String,
    val quizId: String,
    val quizTitle: String,
    val openAt: String,
    val closeAt: String
)

private fun formatRelativeTime(instant: Instant): String {
    val now = Clock.System.now()
    val duration = (now - instant).coerceAtLeast(Duration.ZERO)
    return when {
        duration < 1.minutes -> "Just now"
        duration < 1.hours -> "${duration.inWholeMinutes} min ago"
        duration < 24.hours -> "${duration.inWholeHours} hr ago"
        duration < 7.days -> "${duration.inWholeDays} d ago"
        else -> "${duration.inWholeDays / 7} wk ago"
    }
}
