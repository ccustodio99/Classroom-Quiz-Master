package com.acme.lms.agents.impl

import com.acme.lms.agents.LiveSessionAgent
import com.acme.lms.data.model.LiveResponse
import com.acme.lms.data.model.LiveSession
import com.acme.lms.data.repo.LiveRepo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveSessionAgentImpl @Inject constructor(
    private val liveRepo: LiveRepo,
    private val db: FirebaseFirestore
) : LiveSessionAgent {

    override suspend fun startSession(classworkId: String, hostId: String): LiveSession {
        val classworkDoc = db.document(classworkId)
        val classPath = classworkDoc.parent?.parent?.path
            ?: error("Invalid classwork path: $classworkId")
        // ensure classwork exists
        classworkDoc.get().await()
        val session = LiveSession(
            classId = classPath,
            workId = classworkId,
            hostId = hostId
        )
        return liveRepo.hostSession(classPath, session)
    }

    override suspend fun joinSession(joinCode: String, userId: String): Result<Unit> =
        Result.success(Unit) // LAN discovery handled by client; Firestore fallback not yet implemented

    override suspend fun submitResponse(response: LiveResponse): Result<Unit> =
        runCatching { liveRepo.submitResponse(response) }
}
