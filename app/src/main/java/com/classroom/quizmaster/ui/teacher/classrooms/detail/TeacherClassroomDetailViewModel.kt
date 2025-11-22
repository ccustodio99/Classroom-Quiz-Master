package com.classroom.quizmaster.ui.teacher.classrooms.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.model.QuizCategory
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class TeacherClassroomDetailViewModel @Inject constructor(
    private val classroomRepository: ClassroomRepository,
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val classroomIdArg: String = savedStateHandle[CLASSROOM_ID_KEY]
        ?: throw IllegalArgumentException("classroomId is required")

    val classroomId: String
        get() = classroomIdArg

    val uiState: StateFlow<ClassroomDetailUiState> =
        combine(
            classroomRepository.classrooms,
            classroomRepository.topics,
            quizRepository.quizzes
        ) { classrooms, topics, quizzes ->
            val classroom = classrooms.firstOrNull { it.id == classroomIdArg && !it.isArchived }
            if (classroom == null) {
                ClassroomDetailUiState(
                    classroomId = classroomIdArg,
                    isLoading = false,
                    errorMessage = "Classroom not available"
                )
            } else {
                val topicSummaries = topics
                    .filter { it.classroomId == classroomIdArg && !it.isArchived }
                    .sortedBy { it.name.lowercase() }
                    .map { topic ->
                        val quizCount = quizzes.count { quiz ->
                            !quiz.isArchived &&
                                quiz.classroomId == classroomIdArg &&
                                quiz.topicId == topic.id &&
                                quiz.category == QuizCategory.STANDARD
                        }
                        TopicSummaryUi(
                            id = topic.id,
                            name = topic.name,
                            description = topic.description,
                            quizCount = quizCount
                        )
                    }
                val topicLookup = topicSummaries.associateBy { it.id }
                val classroomQuizzes = quizzes
                    .filter { quiz ->
                        quiz.classroomId == classroomIdArg &&
                            !quiz.isArchived &&
                            topicLookup.containsKey(quiz.topicId)
                    }
                val preTest = classroomQuizzes
                    .filter { it.category == QuizCategory.PRE_TEST }
                    .maxByOrNull { it.updatedAt }
                    ?.let { quiz ->
                        topicLookup[quiz.topicId]?.let { topic ->
                            quiz.toClassroomTestUi(topicName = topic.name)
                        }
                    }
                val postTest = classroomQuizzes
                    .filter { it.category == QuizCategory.POST_TEST }
                    .maxByOrNull { it.updatedAt }
                    ?.let { quiz ->
                        topicLookup[quiz.topicId]?.let { topic ->
                            quiz.toClassroomTestUi(topicName = topic.name)
                        }
                    }
                val defaultTopicId = topicSummaries.firstOrNull()?.id.orEmpty()
                ClassroomDetailUiState(
                    classroomId = classroomIdArg,
                    classroomName = classroom.name,
                    grade = classroom.grade,
                    subject = classroom.subject,
                    joinCode = classroom.joinCode,
                    topics = topicSummaries,
                    preTest = preTest,
                    postTest = postTest,
                    defaultTopicId = defaultTopicId,
                    canCreateTests = topicSummaries.isNotEmpty(),
                    isLoading = false
                )
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ClassroomDetailUiState(classroomId = classroomIdArg)
            )

    fun deleteTest(quizId: String) {
        if (quizId.isBlank()) return
        viewModelScope.launch {
            runCatching { quizRepository.delete(quizId) }
                .onFailure { Timber.e(it, "Failed to delete diagnostic test $quizId") }
        }
    }

    companion object {
        const val CLASSROOM_ID_KEY = "classroomId"
    }
}

data class ClassroomDetailUiState(
    val classroomId: String = "",
    val classroomName: String = "",
    val grade: String = "",
    val subject: String = "",
    val joinCode: String = "",
    val topics: List<TopicSummaryUi> = emptyList(),
    val preTest: ClassroomTestUi? = null,
    val postTest: ClassroomTestUi? = null,
    val defaultTopicId: String = "",
    val canCreateTests: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class TopicSummaryUi(
    val id: String,
    val name: String,
    val description: String,
    val quizCount: Int
)

data class ClassroomTestUi(
    val id: String,
    val topicId: String,
    val topicName: String,
    val title: String,
    val questionCount: Int,
    val updatedAgo: String
)

private fun com.classroom.quizmaster.domain.model.Quiz.toClassroomTestUi(
    topicName: String
): ClassroomTestUi =
    ClassroomTestUi(
        id = id,
        topicId = topicId,
        topicName = topicName,
        title = title.ifBlank { "Untitled test" },
        questionCount = questionCount,
        updatedAgo = formatRelativeTime(updatedAt)
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
