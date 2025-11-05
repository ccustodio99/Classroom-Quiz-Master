package com.acme.lms.agents.impl

import com.acme.lms.agents.LiveSessionAgent
import com.example.lms.core.model.LiveResponse
import com.example.lms.core.model.LiveSession
import com.example.lms.core.model.LiveSessionState // Added
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

    override suspend fun startSession(classId: String, classworkId: String, hostId: String): LiveSession = runCatching {
        val classworkDoc = db.document(classworkId)
        // ensure classwork exists
        classworkDoc.get().await()
        val session = LiveSession(
            classId = classId,
            assignmentId = classworkId, // Fixed: Renamed workId to assignmentId
            state = LiveSessionState.LOBBY, // Added default state
            startedAt = null, // Explicitly null for starting session
            endedAt = null, // Explicitly null for starting session
        )
        liveRepo.hostSession(classId, session)
    }.getOrThrow()

    override suspend fun joinSession(joinCode: String, userId: String): Result<Unit> {
        // LAN discovery handled by client; Firestore fallback not yet implemented
        return Result.success(Unit)
    }

    override suspend fun submitResponse(response: LiveResponse): Result<Unit> =
        runCatching { liveRepo.submitResponse(response) }
}
