package com.acme.lms.agents.impl

import com.acme.lms.agents.AssessmentAgent
import com.acme.lms.data.model.Attempt
import com.acme.lms.data.model.Submission
import com.acme.lms.data.util.Time
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssessmentAgentImpl @Inject constructor(
    private val db: FirebaseFirestore
) : AssessmentAgent {

    override suspend fun start(classId: String, classworkId: String, userId: String): Attempt {
        val classDoc = if (classId.contains("/")) {
            db.document(classId)
        } else {
            db.collection("classes").document(classId)
        }
        val classworkDoc = if (classworkId.contains("/")) {
            db.document(classworkId)
        } else {
            classDoc.collection("classwork").document(classworkId)
        }
        val attemptDoc = classworkDoc.collection("attempts").document()
        val attempt = Attempt(
            id = attemptDoc.path,
            classId = classId,
            classworkId = classworkId,
            userId = userId,
            startedAt = Time.now()
        )
        attemptDoc.set(attempt).await()
        return attempt
    }

    override suspend fun submit(attempt: Attempt): Result<Submission> = runCatching {
        val classworkDoc = if (attempt.classworkId.contains("/")) {
            db.document(attempt.classworkId)
        } else {
            val parentClassDoc = if (attempt.classId.contains("/")) {
                db.document(attempt.classId)
            } else {
                db.collection("classes").document(attempt.classId)
            }
            parentClassDoc.collection("classwork").document(attempt.classworkId)
        }
        val submissionDoc = classworkDoc.collection("submissions").document(attempt.userId)
        val submission = Submission(
            id = submissionDoc.path,
            classId = attempt.classId,
            classworkId = attempt.classworkId,
            userId = attempt.userId,
            attemptIds = listOf(attempt.id),
            score = attempt.score ?: attempt.answers.size.toDouble(),
            updatedAt = Time.now()
        )

        submissionDoc.set(submission).await()
        db.document(attempt.id).update(
            mapOf(
                "answers" to attempt.answers,
                "submittedAt" to Time.now()
            )
        ).await()
        submission
    }
}
