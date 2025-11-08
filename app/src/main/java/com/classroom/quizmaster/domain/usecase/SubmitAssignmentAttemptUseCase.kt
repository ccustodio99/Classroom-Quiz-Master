package com.classroom.quizmaster.domain.usecase

import com.classroom.quizmaster.domain.model.ScoringMode
import com.classroom.quizmaster.domain.model.Submission
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

class SubmitAssignmentAttemptUseCase @Inject constructor(
    private val assignmentRepository: AssignmentRepository
) {
    suspend operator fun invoke(
        assignmentId: String,
        uid: String,
        score: Int
    ): Submission {
        require(score >= 0) { "Score cannot be negative" }
        val assignment = assignmentRepository.assignments.first()
            .firstOrNull { it.id == assignmentId }
            ?: throw IllegalArgumentException("Assignment $assignmentId not found")
        val existingSubmissions = assignmentRepository.submissions(assignmentId).first()
        val existing = existingSubmissions.firstOrNull { it.uid == uid }
        val attempts = (existing?.attempts ?: 0) + 1
        val now = Clock.System.now()
        val bestScore = when (assignment.scoringMode) {
            ScoringMode.BEST -> max(existing?.bestScore ?: 0, score)
            ScoringMode.LAST -> score
            ScoringMode.AVERAGE -> {
                val total = (existing?.bestScore ?: 0) * (existing?.attempts ?: 0) + score
                (total.toDouble() / attempts).roundToInt()
            }
        }
        val updated = Submission(
            uid = uid,
            assignmentId = assignmentId,
            bestScore = bestScore,
            lastScore = score,
            attempts = attempts,
            updatedAt = now
        )
        assignmentRepository.submitHomework(updated)
        return updated
    }
}
