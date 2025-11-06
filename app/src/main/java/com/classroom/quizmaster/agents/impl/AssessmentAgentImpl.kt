package com.classroom.quizmaster.agents.impl

import com.classroom.quizmaster.agents.AssessmentAgent
import com.classroom.quizmaster.data.model.Attempt
import com.classroom.quizmaster.data.model.Submission
import com.classroom.quizmaster.data.util.Time
import com.classroom.quizmaster.data.util.DEFAULT_ORG_ID
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssessmentAgentImpl @Inject constructor(
    private val db: FirebaseFirestore
) : AssessmentAgent {

    override suspend fun start(classId: String, classworkId: String, userId: String): Attempt {
        val classPath = normalizeClassPath(classId)
        val classDoc = db.document(classPath)
        val classworkDoc = resolveClassworkDocument(classPath, classworkId)
        val attemptDoc = classworkDoc.collection("attempts").document()
        val attempt = Attempt(
            id = attemptDoc.path,
            classId = classPath,
            classworkId = classworkDoc.path,
            userId = userId,
            startedAt = Time.now()
        )
        attemptDoc.set(attempt).await()
        return attempt
    }

    override suspend fun submit(attempt: Attempt): Result<Submission> = runCatching {
        val classPath = normalizeClassPath(attempt.classId)
        val classworkDoc = resolveClassworkDocument(classPath, attempt.classworkId)
        val submissionDoc = classworkDoc.collection("submissions").document(attempt.userId)
        val submission = Submission(
            id = submissionDoc.path,
            classId = classPath,
            classworkId = classworkDoc.path,
            userId = attempt.userId,
            attemptIds = listOf(attempt.id),
            score = attempt.score ?: attempt.answers.size.toDouble(),
            updatedAt = Time.now()
        )

        submissionDoc.set(submission).await()
        db.document(normalizeDocumentPath(attempt.id, "attemptId")).update(
            mapOf(
                "answers" to attempt.answers,
                "submittedAt" to Time.now()
            )
        ).await()
        submission
    }

    private fun normalizeClassPath(classId: String): String {
        if (classId.contains("/")) {
            return classId.trimStart('/')
        }
        return "orgs/$DEFAULT_ORG_ID/classes/${classId.trim()}"
    }

    private fun resolveClassworkDocument(classPath: String, classworkId: String): DocumentReference {
        val normalizedClassPath = classPath.trimStart('/')
        return if (classworkId.contains("/")) {
            db.document(classworkId.trimStart('/'))
        } else {
            db.document(normalizedClassPath).collection("classwork").document(classworkId)
        }
    }

    private fun normalizeDocumentPath(raw: String, label: String): String {
        require(raw.isNotBlank()) { "$label must not be blank" }
        return raw.trimStart('/')
    }
}
