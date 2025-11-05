package com.acme.lms.agents.impl

import com.acme.lms.agents.AssessmentAgent
import com.example.lms.core.model.Attempt
import com.example.lms.core.model.Submission
import com.example.lms.core.model.Class
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
        val attemptDoc = db.collection("classes").document(classId).collection("classwork").document(classworkId).collection("attempts").document()
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

    override suspend fun fun submit(attempt: Attempt): Result<Submission> = runCatching {
        val classworkDoc = db.collection("classes").document(attempt.classId).collection("classwork").document(attempt.classworkId)
        val submissionDoc = classworkDoc.collection("submissions").document(attempt.userId)
        val submission = Submission(
            id = submissionDoc.path,
            classId = attempt.classId,
            classworkId = attempt.classworkId,
            userId = attempt.userId,
            attemptIds = listOf(attempt.id),
            score = attempt.answers.size,
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
