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
    private val firestore: FirebaseFirestore,
    private val authDataSource: FirebaseAuthDataSource
) {

    private fun assignments() = firestore.collection("assignments")
    private fun classrooms() = firestore.collection("classrooms")

    suspend fun createAssignment(assignment: Assignment): Result<Unit> = runCatching {
        assignments()
            .document(assignment.id)
            .set(FirestoreAssignment.fromDomain(assignment))
            .await()
        Unit
    }

    suspend fun updateAssignment(assignment: Assignment): Result<Unit> = runCatching {
        assignments()
            .document(assignment.id)
            .set(FirestoreAssignment.fromDomain(assignment))
            .await()
        Unit
    }

    suspend fun fetchAssignments(): Result<List<Assignment>> = runCatching {
        val uid = authDataSource.currentUserId().orEmpty()
        if (uid.isBlank()) return@runCatching emptyList()
        val isTeacher = !authDataSource.isCurrentUserAnonymous()

        val allowedClassroomIds: List<String> = if (isTeacher) {
            val snapshots = listOf(
                classrooms()
                    .whereEqualTo("teacherId", uid)
                    .whereEqualTo("isArchived", false)
                    .get()
                    .await(),
                classrooms()
                    .whereEqualTo("teacherId", uid)
                    .whereEqualTo("archived", false)
                    .get()
                    .await()
            )
            snapshots.flatMap { it.documents }.mapNotNull { it.id }.distinct()
        } else {
            val snapshots = listOf(
                classrooms()
                    .whereArrayContains("students", uid)
                    .whereEqualTo("isArchived", false)
                    .get()
                    .await(),
                classrooms()
                    .whereArrayContains("students", uid)
                    .whereEqualTo("archived", false)
                    .get()
                    .await()
            )
            snapshots.flatMap { it.documents }.mapNotNull { it.id }.distinct()
        }

        if (allowedClassroomIds.isEmpty()) return@runCatching emptyList()

        val assignmentsForUser = mutableListOf<Assignment>()
        allowedClassroomIds.chunked(10).forEach { chunk ->
            val snapshot = assignments()
                .whereIn("classroomId", chunk)
                .get()
                .await()
            assignmentsForUser += snapshot.documents.mapNotNull { doc ->
                doc.toObject(FirestoreAssignment::class.java)?.toDomain(doc.id)
            }
        }
        assignmentsForUser
    }

    suspend fun fetchSubmissions(assignmentId: String): Result<List<Submission>> = runCatching {
        val uid = authDataSource.currentUserId().orEmpty()
        val isTeacher = !authDataSource.isCurrentUserAnonymous()
        val submissionsRef = assignments()
            .document(assignmentId)
            .collection("submissions")

        if (isTeacher) {
            submissionsRef
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(FirestoreSubmission::class.java)
                        ?.toDomain(doc.id)
                        // Defensive: older docs may not have assignmentId set; enforce it from the path.
                        ?.let { submission ->
                            if (submission.assignmentId.isBlank()) submission.copy(assignmentId = assignmentId) else submission
                        }
                }
        } else {
            val doc = submissionsRef.document(uid).get().await()
            if (doc.exists()) {
                listOfNotNull(
                    doc.toObject(FirestoreSubmission::class.java)
                        ?.toDomain(doc.id)
                        ?.let { submission ->
                            if (submission.assignmentId.isBlank()) submission.copy(assignmentId = assignmentId) else submission
                        }
                )
            } else {
                emptyList()
            }
        }
    }

    suspend fun saveSubmission(submission: Submission): Result<Unit> = runCatching {
        assignments()
            .document(submission.assignmentId)
            .collection("submissions")
            .document(submission.uid)
            .set(FirestoreSubmission.fromDomain(submission))
            .await()
        Unit
    }

    suspend fun archiveAssignment(id: String, archivedAt: Instant): Result<Unit> = runCatching {
        assignments()
            .document(id)
            .update(
                mapOf(
                    "isArchived" to true,
                    "archivedAt" to archivedAt.toEpochMilliseconds(),
                    "archived" to true,
                    "updatedAt" to archivedAt.toEpochMilliseconds()
                )
            )
            .await()
        Unit
    }

    suspend fun unarchiveAssignment(id: String, unarchivedAt: Instant): Result<Unit> = runCatching {
        assignments()
            .document(id)
            .update(
                mapOf(
                    "isArchived" to false,
                    "archived" to false,
                    "archivedAt" to null,
                    "updatedAt" to unarchivedAt.toEpochMilliseconds()
                )
            )
            .await()
        Unit
    }

    private data class FirestoreAssignment(
        val quizId: String = "",
        val classroomId: String = "",
        val topicId: String = "",
        val openAt: Long = Clock.System.now().toEpochMilliseconds(),
        val closeAt: Long = openAt,
        val attemptsAllowed: Int = 1,
        val scoringMode: String = ScoringMode.BEST.name,
        val revealAfterSubmit: Boolean = true,
        val createdAt: Long = openAt,
        val updatedAt: Long = openAt,
        // Avoid Kotlin's dual boolean getters by mapping the Firestore field explicitly.
        @field:com.google.firebase.firestore.PropertyName("isArchived")
        @JvmField
        var archivedFlag: Boolean = false,
        // Legacy field name kept for backwards compatibility with older documents.
        @field:com.google.firebase.firestore.PropertyName("archived")
        @JvmField
        var archivedLegacy: Boolean? = null,
        val archivedAt: Long? = null
    ) {
        fun toDomain(id: String): Assignment = Assignment(
            id = id,
            quizId = quizId,
            classroomId = classroomId,
            topicId = topicId,
            openAt = Instant.fromEpochMilliseconds(openAt),
            closeAt = Instant.fromEpochMilliseconds(closeAt),
            attemptsAllowed = attemptsAllowed,
            scoringMode = runCatching { ScoringMode.valueOf(scoringMode) }.getOrDefault(ScoringMode.BEST),
            revealAfterSubmit = revealAfterSubmit,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            isArchived = archivedFlag || (archivedLegacy ?: false),
            archivedAt = archivedAt?.let(Instant::fromEpochMilliseconds)
        )

        companion object {
            fun fromDomain(assignment: Assignment) = FirestoreAssignment(
                quizId = assignment.quizId,
                classroomId = assignment.classroomId,
                topicId = assignment.topicId,
                openAt = assignment.openAt.toEpochMilliseconds(),
                closeAt = assignment.closeAt.toEpochMilliseconds(),
                attemptsAllowed = assignment.attemptsAllowed,
                scoringMode = assignment.scoringMode.name,
                revealAfterSubmit = assignment.revealAfterSubmit,
                createdAt = assignment.createdAt.toEpochMilliseconds(),
                updatedAt = assignment.updatedAt.toEpochMilliseconds(),
                archivedFlag = assignment.isArchived,
                archivedLegacy = assignment.isArchived,
                archivedAt = assignment.archivedAt?.toEpochMilliseconds()
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
