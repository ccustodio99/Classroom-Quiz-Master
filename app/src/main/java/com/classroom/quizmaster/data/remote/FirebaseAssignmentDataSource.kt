package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Submission
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAssignmentDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private fun assignments() = firestore.collection("assignments")

    suspend fun createAssignment(assignment: Assignment) = runCatching {
        assignments().document(assignment.id).set(assignment.toMap()).await()
    }

    suspend fun saveSubmission(submission: Submission) = runCatching {
        assignments()
            .document(submission.assignmentId)
            .collection("submissions")
            .document(submission.uid)
            .set(submission.toMap())
            .await()
    }

    private fun Assignment.toMap(): Map<String, Any?> = mapOf(
        "quizId" to quizId,
        "classroomId" to classroomId,
        "openAt" to openAt.toEpochMilliseconds(),
        "closeAt" to closeAt.toEpochMilliseconds(),
        "attemptsAllowed" to attemptsAllowed,
        "revealAfterSubmit" to revealAfterSubmit
    )

    private fun Submission.toMap(): Map<String, Any?> = mapOf(
        "bestScore" to bestScore,
        "lastScore" to lastScore,
        "attempts" to attempts,
        "updatedAt" to updatedAt.toEpochMilliseconds()
    )
}
