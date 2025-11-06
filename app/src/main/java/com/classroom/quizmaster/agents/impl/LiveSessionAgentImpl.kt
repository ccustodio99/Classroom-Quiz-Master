package com.classroom.quizmaster.agents.impl

import com.classroom.quizmaster.agents.LiveSessionAgent
import com.classroom.quizmaster.data.model.LiveResponse
import com.classroom.quizmaster.data.model.LiveSession
import com.classroom.quizmaster.data.model.LiveSessionState
import com.classroom.quizmaster.data.repo.LiveRepo
import com.classroom.quizmaster.data.util.DEFAULT_ORG_ID
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveSessionAgentImpl @Inject constructor(
    private val liveRepo: LiveRepo,
    private val db: FirebaseFirestore
) : LiveSessionAgent {

    override suspend fun startSession(classId: String, classworkId: String, hostId: String): LiveSession = runCatching {
        val classPath = normalizeClassPath(classId)
        val classworkDoc = resolveClassworkDocument(classPath, classworkId)
        // ensure classwork exists
        classworkDoc.get().await()
        val session = LiveSession(
            classId = classPath,
            assignmentId = classworkDoc.path,
            hostId = hostId,
            state = LiveSessionState.LOBBY,
            startedAt = null,
            endedAt = null,
        )
        liveRepo.hostSession(classPath, session)
    }.getOrThrow()

    override suspend fun joinSession(joinCode: String, userId: String): Result<Unit> {
        // LAN discovery handled by client; Firestore fallback not yet implemented
        return Result.success(Unit)
    }

    override suspend fun submitResponse(response: LiveResponse): Result<Unit> =
        runCatching { liveRepo.submitResponse(response) }

    private fun normalizeClassPath(classId: String): String {
        if (classId.contains("/")) {
            return classId.trimStart('/')
        }
        // Fall back to default org namespace when only a class document id is provided.
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
}
