package com.acme.lms.agents.impl

import com.acme.lms.agents.LiveSessionAgent
import com.acme.lms.data.model.LiveResponse
import com.acme.lms.data.model.LiveSession
import com.acme.lms.data.model.LiveSessionState
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
        val classworkDoc = if (classworkId.contains("/")) {
            db.document(classworkId)
        } else {
            db.document(classId).collection("classwork").document(classworkId)
        }
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
