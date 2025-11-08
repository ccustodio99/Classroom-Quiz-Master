package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.ScoringMode
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class CreateAssignmentUseCase @Inject constructor(
    private val assignmentRepository: AssignmentRepository,
    private val quizRepository: QuizRepository
) {

    data class Params(
        val quizId: String,
        val classroomId: String,
        val openAt: Instant,
        val closeAt: Instant,
        val attemptsAllowed: Int,
        val scoringMode: ScoringMode,
        val revealAfterSubmit: Boolean,
        val assignmentId: String? = null
    )

    suspend operator fun invoke(params: Params): Assignment {
        require(params.attemptsAllowed > 0) { "Attempts allowed must be positive" }
        require(!params.openAt.isAfter(params.closeAt)) { "Open date must be before close date" }
        val quiz = quizRepository.getQuiz(params.quizId)
            ?: throw IllegalArgumentException("Quiz ${params.quizId} not found")
        val totalQuestions = quiz.questions.size.takeIf { it > 0 } ?: quiz.questionCount
        require(totalQuestions > 0) { "Assignments require a quiz with questions" }

        val now = Clock.System.now()
        val resolvedId = params.assignmentId?.takeIf { it.isNotBlank() } ?: "assign-${UUID.randomUUID()}"
        val assignment = Assignment(
            id = resolvedId,
            quizId = params.quizId,
            classroomId = params.classroomId,
            openAt = params.openAt,
            closeAt = params.closeAt,
            attemptsAllowed = params.attemptsAllowed,
            scoringMode = params.scoringMode,
            revealAfterSubmit = params.revealAfterSubmit,
            createdAt = now,
            updatedAt = now
        )
        assignmentRepository.createAssignment(assignment)
        return assignment
    }

    private fun Instant.isAfter(other: Instant): Boolean = this > other
}
