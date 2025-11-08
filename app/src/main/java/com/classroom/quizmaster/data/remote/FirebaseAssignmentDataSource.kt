package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.ScoringMode
import com.classroom.quizmaster.domain.model.Submission
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAssignmentDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun assignments() = firestore.collection("assignments")

    suspend fun createAssignment(assignment: Assignment) = runCatching {
        assignments()
            .document(assignment.id)
            .set(FirestoreAssignment.fromDomain(assignment))
            .await()
    }

    suspend fun fetchAssignments(): Result<List<Assignment>> = runCatching {
        assignments()
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(FirestoreAssignment::class.java)?.toDomain(doc.id)
            }
    }

    suspend fun fetchSubmissions(assignmentId: String): Result<List<Submission>> = runCatching {
        assignments()
            .document(assignmentId)
            .collection("submissions")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(FirestoreSubmission::class.java)?.toDomain(doc.id)
            }
    }

    suspend fun saveSubmission(submission: Submission) = runCatching {
        assignments()
            .document(submission.assignmentId)
            .collection("submissions")
            .document(submission.uid)
            .set(FirestoreSubmission.fromDomain(submission))
            .await()
    }

    private data class FirestoreAssignment(
        val quizId: String = "",
        val classroomId: String = "",
        val openAt: Long = Clock.System.now().toEpochMilliseconds(),
        val closeAt: Long = openAt,
        val attemptsAllowed: Int = 1,
        val scoringMode: String = ScoringMode.BEST.name,
        val revealAfterSubmit: Boolean = true,
        val createdAt: Long = openAt,
        val updatedAt: Long = openAt
    ) {
        fun toDomain(id: String): Assignment = Assignment(
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

        companion object {
            fun fromDomain(assignment: Assignment) = FirestoreAssignment(
                quizId = assignment.quizId,
                classroomId = assignment.classroomId,
                openAt = assignment.openAt.toEpochMilliseconds(),
                closeAt = assignment.closeAt.toEpochMilliseconds(),
                attemptsAllowed = assignment.attemptsAllowed,
                scoringMode = assignment.scoringMode.name,
                revealAfterSubmit = assignment.revealAfterSubmit,
                createdAt = assignment.createdAt.toEpochMilliseconds(),
                updatedAt = assignment.updatedAt.toEpochMilliseconds()
            )
        }
    }

    private data class FirestoreSubmission(
        val assignmentId: String = "",
        val bestScore: Int = 0,
        val lastScore: Int = 0,
        val attempts: Int = 0,
        val updatedAt: Long = Clock.System.now().toEpochMilliseconds()
    ) {
        fun toDomain(uid: String): Submission = Submission(
            uid = uid,
            assignmentId = assignmentId,
            bestScore = bestScore,
            lastScore = lastScore,
            attempts = attempts,
            updatedAt = Instant.fromEpochMilliseconds(updatedAt)
        )

        companion object {
            fun fromDomain(submission: Submission) = FirestoreSubmission(
                assignmentId = submission.assignmentId,
                bestScore = submission.bestScore,
                lastScore = submission.lastScore,
                attempts = submission.attempts,
                updatedAt = submission.updatedAt.toEpochMilliseconds()
            )
        }
    }
}
