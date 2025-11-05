package com.acme.lms.data.repo

import com.example.lms.core.model.LiveResponse
import com.example.lms.core.model.LiveSession
import com.example.lms.core.model.LiveSessionState
import com.acme.lms.data.net.lan.LanBroadcaster
import com.acme.lms.data.net.webrtc.WebRtcHost
import com.acme.lms.data.util.Time
import com.acme.lms.data.util.asFlow
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
        val collection = sessionCollection(classPath)
        val doc = if (session.id.isBlank()) collection.document() else db.document(session.id)
        val enriched = session.copy(
            id = doc.path,
            classId = classPath,
            state = LiveSessionState.RUNNING, // FIXED: from 'status = "running"' to 'state = LiveSessionState.RUNNING'
            startedAt = Time.now()
        )
        lan.start(enriched.id)
        host.start(enriched.id)
        doc.set(enriched).await()
        return enriched
    }

    fun answersStream(sessionPath: String): Flow<List<LiveResponse>> =
        sessionDoc(sessionPath)
            .collection("responses")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .asFlow(::responseFromSnapshot)

    suspend fun submitResponse(response: LiveResponse) {
        val doc = sessionDoc(response.sessionId)
        doc.collection("responses")
            .document(response.id.ifBlank { doc.collection("responses").document().id })
            .set(response.copy(timestamp = Time.now()))
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
        val segments = path.split("/")
        require(segments.size >= 4) { "Class path must be /orgs/{orgId}/classes/{classId}" }
        val orgIndex = segments.indexOf("orgs")
        val classIndex = segments.indexOf("classes")
        require(orgIndex >= 0 && classIndex >= 0) { "Invalid class path: $path" }
        val orgId = segments.getOrNull(orgIndex + 1) ?: error("Missing orgId in $path")
        val classId = segments.getOrNull(classIndex + 1) ?: error("Missing classId in $path")
        return orgId to classId
    }
}
