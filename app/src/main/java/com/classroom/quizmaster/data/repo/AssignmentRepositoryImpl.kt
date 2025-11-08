package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.entity.AssignmentLocalEntity
import com.classroom.quizmaster.data.local.entity.SubmissionLocalEntity
import com.classroom.quizmaster.data.remote.FirebaseAssignmentDataSource
import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.ScoringMode
import com.classroom.quizmaster.domain.model.Submission
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentRepositoryImpl @Inject constructor(
    private val remote: FirebaseAssignmentDataSource,
    private val database: QuizMasterDatabase
) : AssignmentRepository {

    private val assignmentDao = database.assignmentDao()

    override val assignments: Flow<List<Assignment>> =
        assignmentDao.observeAssignments().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun submissions(assignmentId: String): Flow<List<Submission>> =
        assignmentDao.observeSubmissions(assignmentId).map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun refreshAssignments() {
        val remoteAssignments = remote.fetchAssignments().getOrElse { emptyList() }
        if (remoteAssignments.isNotEmpty()) {
            assignmentDao.upsertAssignments(remoteAssignments.map { it.toEntity() })
        }
        remoteAssignments.forEach { assignment ->
            val remoteSubmissions = remote.fetchSubmissions(assignment.id).getOrElse { emptyList() }
            if (remoteSubmissions.isNotEmpty()) {
                assignmentDao.upsertSubmissions(remoteSubmissions.map { it.toEntity() })
            }
        }
    }

    override suspend fun createAssignment(assignment: Assignment) {
        assignmentDao.upsertAssignments(listOf(assignment.toEntity()))
        remote.createAssignment(assignment)
    }

    override suspend fun submitHomework(submission: Submission) {
        assignmentDao.upsertSubmission(submission.toEntity())
        remote.saveSubmission(submission)
    }

    private fun Assignment.toEntity(): AssignmentLocalEntity =
        AssignmentLocalEntity(
            id = id,
            quizId = quizId,
            classroomId = classroomId,
            openAt = openAt.toEpochMilliseconds(),
            closeAt = closeAt.toEpochMilliseconds(),
            attemptsAllowed = attemptsAllowed,
            scoringMode = scoringMode.name,
            revealAfterSubmit = revealAfterSubmit,
            createdAt = createdAt.toEpochMilliseconds(),
            updatedAt = updatedAt.toEpochMilliseconds()
        )

    private fun Submission.toEntity() = SubmissionLocalEntity(
        assignmentId = assignmentId,
        uid = uid,
        bestScore = bestScore,
        lastScore = lastScore,
        attempts = attempts,
        updatedAt = updatedAt.toEpochMilliseconds()
    )

    private fun AssignmentLocalEntity.toDomain(): Assignment = Assignment(
        id = id,
        quizId = quizId,
        classroomId = classroomId,
        openAt = Instant.fromEpochMilliseconds(openAt),
        closeAt = Instant.fromEpochMilliseconds(closeAt),
        attemptsAllowed = attemptsAllowed,
        scoringMode = runCatching { ScoringMode.valueOf(scoringMode) }.getOrDefault(ScoringMode.BEST),
        revealAfterSubmit = revealAfterSubmit,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )

    private fun SubmissionLocalEntity.toDomain(): Submission = Submission(
        uid = uid,
        assignmentId = assignmentId,
        bestScore = bestScore,
        lastScore = lastScore,
        attempts = attempts,
        updatedAt = Instant.fromEpochMilliseconds(updatedAt)
    )
}
