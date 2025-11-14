package com.classroom.quizmaster.ui.teacher.classrooms.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
                                quiz.topicId == topic.id
                        }
                        TopicSummaryUi(
                            id = topic.id,
                            name = topic.name,
                            description = topic.description,
                            quizCount = quizCount
                        )
                    }
                ClassroomDetailUiState(
                    classroomId = classroomIdArg,
                    classroomName = classroom.name,
                    grade = classroom.grade,
                    subject = classroom.subject,
                    topics = topicSummaries,
                    isLoading = false
                )
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ClassroomDetailUiState(classroomId = classroomIdArg)
            )

    companion object {
        const val CLASSROOM_ID_KEY = "classroomId"
    }
}

data class ClassroomDetailUiState(
    val classroomId: String = "",
    val classroomName: String = "",
    val grade: String = "",
    val subject: String = "",
    val topics: List<TopicSummaryUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class TopicSummaryUi(
    val id: String,
    val name: String,
    val description: String,
    val quizCount: Int
)
