package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    suspend fun publishSession(session: Session) = runCatching {
        sessionDoc(session.id).set(session.toMap()).await()
    }.onFailure { Timber.e(it, "publish session failed") }

    suspend fun publishParticipants(sessionId: String, participants: List<Participant>) = runCatching {
        sessionDoc(sessionId)
            .collection("participants")
            .document("snapshot")
            .set(mapOf("participants" to json.encodeToString(participants)))
            .await()
    }.onFailure { Timber.e(it, "publish participants failed") }

    suspend fun publishAttempt(sessionId: String, attempt: Attempt) = runCatching {
        sessionDoc(sessionId)
            .collection("attempts")
            .document(attempt.id)
            .set(attempt.toMap(), SetOptions.merge())
            .await()
    }.onFailure { Timber.e(it, "publish attempt failed") }

    private fun Session.toMap(): Map<String, Any?> = mapOf(
        "teacherId" to teacherId,
        "quizId" to quizId,
        "classroomId" to classroomId,
        "joinCode" to joinCode,
        "status" to status.name.lowercase(),
        "currentIndex" to currentIndex,
        "reveal" to reveal,
        "startedAt" to startedAt?.toEpochMilliseconds()
    )

    private fun Attempt.toMap(): Map<String, Any?> = mapOf(
        "uid" to uid,
        "questionId" to questionId,
        "selected" to json.encodeToString(selected),
        "timeMs" to timeMs,
        "correct" to correct,
        "points" to points,
        "late" to late,
        "createdAt" to createdAt.toEpochMilliseconds()
    )
}
