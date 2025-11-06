package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.model.LiveResponse
import com.classroom.quizmaster.data.model.LiveSession
import com.classroom.quizmaster.data.net.lan.LanBroadcaster
import com.classroom.quizmaster.data.net.webrtc.WebRtcHost
import com.classroom.quizmaster.data.util.Time
import com.classroom.quizmaster.data.util.asFlow
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveRepo @Inject constructor(
    private val db: FirebaseFirestore,
    private val lan: LanBroadcaster,
    private val host: WebRtcHost
) {

    suspend fun hostSession(classPath: String, session: LiveSession): LiveSession {
        val normalizedClassPath = normalizePath(classPath, "classPath")
        val collection = sessionCollection(normalizedClassPath)
        val doc = if (session.id.isBlank()) collection.document() else db.document(normalizePath(session.id, "sessionId"))
        val enriched = session.copy(
            id = doc.path,
            classId = normalizedClassPath,
            startedAt = session.startedAt ?: Time.now()
        )
        require(enriched.hostId.isNotBlank()) { "hostId must be provided when starting a live session" }
        lan.start(enriched.id)
        host.start(enriched.id)
        doc.set(enriched).await()
        return enriched
    }

    fun answersStream(sessionPath: String): Flow<List<LiveResponse>> =
        sessionDoc(normalizePath(sessionPath, "sessionPath"))
            .collection("responses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .asFlow(::responseFromSnapshot)

    suspend fun submitResponse(response: LiveResponse) {
        val sessionDoc = sessionDoc(normalizePath(response.sessionId, "sessionId"))
        val responses = sessionDoc.collection("responses")
        val resolvedId = response.id.ifBlank { responses.document().id }
        responses
            .document(resolvedId)
            .set(response.copy(id = resolvedId, timestamp = Time.now()))
            .await()
    }

    private fun sessionCollection(classPath: String) = run {
        val (orgId, classId) = parseClassPath(classPath)
        db.collection("orgs").document(orgId)
            .collection("classes").document(classId)
            .collection("liveSessions")
    }

    private fun sessionDoc(sessionPath: String): DocumentReference =
        db.document(sessionPath)

    private fun responseFromSnapshot(snapshot: DocumentSnapshot): LiveResponse =
        snapshot.toObject(LiveResponse::class.java)!!

    private fun parseClassPath(path: String): Pair<String, String> {
        val trimmed = path.trimStart('/')
        val segments = trimmed.split("/")
        require(segments.size >= 4) { "Class path must be /orgs/{orgId}/classes/{classId}" }
        val orgIndex = segments.indexOf("orgs")
        val classIndex = segments.indexOf("classes")
        require(orgIndex >= 0 && classIndex >= 0) { "Invalid class path: $path" }
        val orgId = segments.getOrNull(orgIndex + 1) ?: error("Missing orgId in $path")
        val classId = segments.getOrNull(classIndex + 1) ?: error("Missing classId in $path")
        return orgId to classId
    }

    private fun normalizePath(raw: String, label: String): String {
        require(raw.isNotBlank()) { "$label must not be blank" }
        return raw.trimStart('/')
    }
}
