package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.entity.AssignmentLocalEntity
import com.classroom.quizmaster.data.local.entity.SubmissionLocalEntity
import com.classroom.quizmaster.data.remote.FirebaseAssignmentDataSource
import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Submission
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentRepositoryImpl @Inject constructor(
    private val remote: FirebaseAssignmentDataSource,
    private val database: QuizMasterDatabase
) : AssignmentRepository {

    override suspend fun createAssignment(assignment: Assignment) {
        remote.createAssignment(assignment)
        database.assignmentDao().upsertAssignments(listOf(assignment.toEntity()))
    }

    override suspend fun submitHomework(submission: Submission) {
        remote.saveSubmission(submission)
        database.assignmentDao().upsertSubmission(submission.toEntity())
    }

    private fun Assignment.toEntity(): AssignmentLocalEntity {
        val now = Clock.System.now().toEpochMilliseconds()
        return AssignmentLocalEntity(
            id = id,
            quizId = quizId,
            classroomId = classroomId,
            openAt = openAt.toEpochMilliseconds(),
            closeAt = closeAt.toEpochMilliseconds(),
            attemptsAllowed = attemptsAllowed,
            revealAfterSubmit = revealAfterSubmit,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun Submission.toEntity() = SubmissionLocalEntity(
        assignmentId = assignmentId,
        uid = uid,
        bestScore = bestScore,
        lastScore = lastScore,
        attempts = attempts,
        updatedAt = updatedAt.toEpochMilliseconds()
    )
}
