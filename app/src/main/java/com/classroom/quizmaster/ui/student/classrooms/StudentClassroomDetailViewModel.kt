package com.classroom.quizmaster.ui.student.classrooms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.repository.LearningMaterialRepository
import com.classroom.quizmaster.ui.materials.MaterialSummaryUi
import com.classroom.quizmaster.ui.materials.toSummaryUi
import com.classroom.quizmaster.ui.model.AssignmentCardUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock

data class TopicUi(
    val id: String,
    val name: String,
    val description: String,
    val quizCount: Int = 0
)

data class StudentClassroomDetailUiState(
    val classroomName: String = "",
    val teacherName: String = "",
    val subject: String = "",
    val grade: String = "",
    val joinCode: String = "",
    val topics: List<TopicUi> = emptyList(),
    val assignments: List<AssignmentCardUi> = emptyList(),
    val materials: List<MaterialSummaryUi> = emptyList(),
    val materialsCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StudentClassroomDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    classroomRepository: ClassroomRepository,
    assignmentRepository: AssignmentRepository,
    quizRepository: QuizRepository,
    private val authRepository: AuthRepository,
    learningMaterialRepository: LearningMaterialRepository
) : ViewModel() {

    private val classroomId: String = savedStateHandle.get<String>("classroomId").orEmpty()

    val uiState: StateFlow<StudentClassroomDetailUiState> = combine(
        classroomRepository.classrooms,
        classroomRepository.topics,
        assignmentRepository.assignments,
        quizRepository.quizzes,
        learningMaterialRepository.observeStudentMaterials(classroomId = classroomId)
    ) { classrooms, topics, assignments, quizzes, materials ->
        val classroom = classrooms.firstOrNull { it.id == classroomId }
        val teacherName = classroom?.let {
            runCatching { authRepository.getTeacher(it.teacherId).first()?.displayName }.getOrNull()
                .orEmpty()
        }.orEmpty()
        val quizCountByTopic = quizzes.groupBy { it.topicId }.mapValues { it.value.size }
        val topicUi = topics
            .filter { it.classroomId == classroomId && !it.isArchived }
            .map { TopicUi(it.id, it.name, it.description, quizCountByTopic[it.id] ?: 0) }
        val quizLookup = quizzes.associateBy { it.id }
        val now = Clock.System.now()
        val assignmentCards = assignments
            .filter { it.classroomId == classroomId }
            .map { assignment ->
                val quiz = quizLookup[assignment.quizId]
                val title = quiz?.title?.ifBlank { "Untitled quiz" } ?: "Untitled quiz"
                val statusLabel = when {
                    assignment.isArchived -> "Archived"
                    now < assignment.openAt -> "Scheduled"
                    assignment.closeAt > now -> "Open"
                    else -> "Closed"
                }
                AssignmentCardUi(
                    id = assignment.id,
                    title = title,
                    dueIn = formatDue(now, assignment.openAt, assignment.closeAt),
                    submissions = 0,
                    attemptsAllowed = assignment.attemptsAllowed,
                    statusLabel = statusLabel
                )
            }
        val materialSummaries = materials
            .filter { it.classroomId == classroomId }
            .map { it.toSummaryUi() }
        StudentClassroomDetailUiState(
            classroomName = classroom?.name.orEmpty(),
            teacherName = teacherName.ifBlank { "Teacher" },
            subject = classroom?.subject.orEmpty(),
            grade = classroom?.grade.orEmpty(),
            joinCode = classroom?.joinCode.orEmpty(),
            topics = topicUi,
            assignments = assignmentCards,
            materials = materialSummaries,
            materialsCount = materialSummaries.count(),
            isLoading = classroom == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudentClassroomDetailUiState())

    private fun formatDue(now: kotlinx.datetime.Instant, openAt: kotlinx.datetime.Instant, closeAt: kotlinx.datetime.Instant): String =
        when {
            now < openAt -> "Opens soon"
            closeAt > now -> "Due in ${(closeAt - now).inWholeHours}h"
            else -> "Closed"
        }
}
