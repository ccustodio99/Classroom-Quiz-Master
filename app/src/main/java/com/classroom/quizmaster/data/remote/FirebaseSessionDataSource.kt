package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.model.SessionStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.classroom.quizmaster.config.FeatureToggles
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSessionDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val json: Json
) {

    private fun sessionDoc(sessionId: String) = firestore.collection("sessions").document(sessionId)

    suspend fun publishSession(session: Session): Result<Unit> = runCatching {
        sessionDoc(session.id).set(session.toMap()).await()
        Unit
    }.onFailure { Timber.e(it, "publish session failed") }

    suspend fun publishParticipants(sessionId: String, participants: List<Participant>): Result<Unit> = runCatching {
        sessionDoc(sessionId)
            .collection("participants")
            .document("snapshot")
            .set(mapOf("participants" to json.encodeToString(participants)))
            .await()
        Unit
    }.onFailure { Timber.e(it, "publish participants failed") }

    suspend fun publishAttempt(sessionId: String, attempt: Attempt): Result<Unit> = runCatching {
        sessionDoc(sessionId)
            .collection("attempts")
            .document(attempt.id)
            .set(attempt.toMap(), SetOptions.merge())
            .await()
        Unit
    }.onFailure { Timber.e(it, "publish attempt failed") }

    fun observeSession(sessionId: String): Flow<Session?> = callbackFlow {
        if (!FeatureToggles.LIVE_ENABLED) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = sessionDoc(sessionId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.w(error, "Session snapshot error for %s", sessionId)
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            val data = snapshot.data ?: return@addSnapshotListener
            val statusRaw = data["status"] as? String ?: "lobby"
            val status = runCatching { SessionStatus.valueOf(statusRaw.uppercase()) }
                .getOrDefault(SessionStatus.LOBBY)
            val session = Session(
                id = snapshot.id,
                quizId = data["quizId"] as? String ?: "",
                classroomId = data["classroomId"] as? String ?: "",
                joinCode = data["joinCode"] as? String ?: "",
                status = status,
                currentIndex = (data["currentIndex"] as? Number)?.toInt() ?: 0,
                reveal = data["reveal"] as? Boolean ?: false,
                startedAt = (data["startedAt"] as? Number)?.toLong()?.let(Instant::fromEpochMilliseconds),
                lockAfterQ1 = data["lockAfterQ1"] as? Boolean ?: false,
                hideLeaderboard = data["hideLeaderboard"] as? Boolean ?: false,
                teacherId = data["teacherId"] as? String ?: "",
                endedAt = (data["endedAt"] as? Number)?.toLong()?.let(Instant::fromEpochMilliseconds),
                lanMeta = null
            )
            trySend(session)
        }
        awaitClose { registration.remove() }
    }

    private fun Session.toMap(): Map<String, Any?> = mapOf(
        "teacherId" to teacherId,
        "quizId" to quizId,
        "classroomId" to classroomId,
        "joinCode" to joinCode,
        "status" to status.name.lowercase(),
        "currentIndex" to currentIndex,
        "reveal" to reveal,
        "startedAt" to startedAt?.toEpochMilliseconds(),
        "lockAfterQ1" to lockAfterQ1,
        "hideLeaderboard" to hideLeaderboard,
        "endedAt" to endedAt?.toEpochMilliseconds()
    )

    private fun Attempt.toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "questionId" to questionId,
        "selected" to json.encodeToString(selected),
        "timeMs" to timeMs,
        "points" to points,
        "correct" to correct,
        "late" to late,
        "createdAt" to createdAt.toEpochMilliseconds()
    )
}
