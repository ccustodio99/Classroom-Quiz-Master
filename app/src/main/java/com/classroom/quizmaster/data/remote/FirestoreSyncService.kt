package com.classroom.quizmaster.data.remote

import com.classroom.quizmaster.data.local.PendingSync
import com.classroom.quizmaster.data.local.SyncEntityType
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.Class
import com.classroom.quizmaster.domain.model.Classwork
import com.classroom.quizmaster.domain.model.LiveResponse
import com.classroom.quizmaster.domain.model.LiveSession
import com.classroom.quizmaster.domain.model.Roster
import com.classroom.quizmaster.domain.model.Submission
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestoreSettings
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FirestoreSyncService(
    private val firestore: FirebaseFirestore,
    private val json: Json,
    private val orgIdProvider: () -> String? = { null }
) {

    init {
        val settings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestore.CACHE_SIZE_UNLIMITED
        }
        firestore.firestoreSettings = settings
    }

    suspend fun pushOperation(operation: PendingSync) {
        when (operation.entityType) {
            SyncEntityType.CLASS -> {
                val payload = json.decodeFromString(Class.serializer(), operation.payloadJson)
                pushClass(payload)
            }
            SyncEntityType.ROSTER -> {
                val payload = json.decodeFromString(Roster.serializer(), operation.payloadJson)
                pushRoster(payload)
            }
            SyncEntityType.CLASSWORK -> {
                val payload = json.decodeFromString(Classwork.serializer(), operation.payloadJson)
                pushClasswork(payload)
            }
            SyncEntityType.ATTEMPT -> {
                val payload = json.decodeFromString(Attempt.serializer(), operation.payloadJson)
                pushAttempt(payload)
            }
            SyncEntityType.SUBMISSION -> {
                val payload = json.decodeFromString(Submission.serializer(), operation.payloadJson)
                pushSubmission(payload)
            }
            SyncEntityType.LIVE_SESSION -> {
                val payload = json.decodeFromString(LiveSession.serializer(), operation.payloadJson)
                pushLiveSession(payload)
            }
            SyncEntityType.LIVE_RESPONSE -> {
                val payload = json.decodeFromString(LiveResponse.serializer(), operation.payloadJson)
                pushLiveResponse(payload)
            }
        }
    }

    suspend fun fetchSnapshot(): RemoteSnapshot {
        val classesQuery = classesCollection().get().await()
        val classes = mutableListOf<Class>()
        val rosters = mutableMapOf<String, List<Roster>>()
        val classwork = mutableMapOf<String, List<Classwork>>()

        for (doc in classesQuery.documents) {
            val payloadJson = doc.getString(FIELD_PAYLOAD) ?: continue
            val classroom = runCatching { json.decodeFromString(Class.serializer(), payloadJson) }
                .getOrNull() ?: continue
            classes += classroom

            val rosterSnapshot = doc.reference.collection(COLLECTION_ROSTER).get().await()
            val rosterEntries = rosterSnapshot.documents.mapNotNull { rosterDoc ->
                val rosterJson = rosterDoc.getString(FIELD_PAYLOAD) ?: return@mapNotNull null
                runCatching { json.decodeFromString(Roster.serializer(), rosterJson) }.getOrNull()
            }
            if (rosterEntries.isNotEmpty()) {
                rosters[classroom.id] = rosterEntries
            }

            val classworkSnapshot = doc.reference.collection(COLLECTION_CLASSWORK).get().await()
            val classworkItems = classworkSnapshot.documents.mapNotNull { classworkDoc ->
                val cwJson = classworkDoc.getString(FIELD_PAYLOAD) ?: return@mapNotNull null
                runCatching { json.decodeFromString(Classwork.serializer(), cwJson) }.getOrNull()
            }
            if (classworkItems.isNotEmpty()) {
                classwork[classroom.id] = classworkItems
            }
        }

        val attempts = firestore.collection(COLLECTION_ATTEMPTS).get().await().documents.mapNotNull { doc ->
            val payloadJson = doc.getString(FIELD_PAYLOAD) ?: return@mapNotNull null
            runCatching { json.decodeFromString(Attempt.serializer(), payloadJson) }.getOrNull()
        }

        val submissions = firestore.collection(COLLECTION_SUBMISSIONS).get().await().documents.mapNotNull { doc ->
            val payloadJson = doc.getString(FIELD_PAYLOAD) ?: return@mapNotNull null
            runCatching { json.decodeFromString(Submission.serializer(), payloadJson) }.getOrNull()
        }

        val liveSessionDocs = firestore.collection(COLLECTION_LIVE_SESSIONS).get().await()
        val liveSessions = mutableListOf<LiveSession>()
        val liveResponses = mutableMapOf<String, List<LiveResponse>>()
        for (doc in liveSessionDocs.documents) {
            val payloadJson = doc.getString(FIELD_PAYLOAD) ?: continue
            val session = runCatching { json.decodeFromString(LiveSession.serializer(), payloadJson) }
                .getOrNull() ?: continue
            liveSessions += session

            val responsesSnapshot = doc.reference.collection(COLLECTION_RESPONSES).get().await()
            val responses = responsesSnapshot.documents.mapNotNull { responseDoc ->
                val responseJson = responseDoc.getString(FIELD_PAYLOAD) ?: return@mapNotNull null
                runCatching { json.decodeFromString(LiveResponse.serializer(), responseJson) }.getOrNull()
            }
            if (responses.isNotEmpty()) {
                liveResponses[session.id] = responses
            }
        }

        return RemoteSnapshot(
            classes = classes,
            rosters = rosters,
            classwork = classwork,
            attempts = attempts,
            submissions = submissions,
            liveSessions = liveSessions,
            liveResponses = liveResponses
        )
    }

    private suspend fun pushClass(payload: Class) {
        val now = System.currentTimeMillis()
        classesCollection().document(payload.id).set(
            mapOf(
                FIELD_PAYLOAD to json.encodeToString(Class.serializer(), payload),
                "subject" to payload.subject,
                "section" to payload.section,
                "ownerId" to payload.ownerId,
                "code" to payload.code,
                FIELD_UPDATED_AT to now
            ),
            SetOptions.merge()
        ).await()
    }

    private suspend fun pushRoster(payload: Roster) {
        val now = System.currentTimeMillis()
        classesCollection()
            .document(payload.classId)
            .collection(COLLECTION_ROSTER)
            .document(payload.userId)
            .set(
                mapOf(
                    FIELD_PAYLOAD to json.encodeToString(Roster.serializer(), payload),
                    "role" to payload.role.name,
                    FIELD_UPDATED_AT to now
                ),
                SetOptions.merge()
            ).await()
    }

    private suspend fun pushClasswork(payload: Classwork) {
        val now = System.currentTimeMillis()
        classesCollection()
            .document(payload.classId)
            .collection(COLLECTION_CLASSWORK)
            .document(payload.id)
            .set(
                mapOf(
                    FIELD_PAYLOAD to json.encodeToString(Classwork.serializer(), payload),
                    "type" to payload.type.name,
                    "dueAt" to payload.dueAt,
                    FIELD_UPDATED_AT to now
                ),
                SetOptions.merge()
            ).await()
    }

    private suspend fun pushAttempt(payload: Attempt) {
        val now = System.currentTimeMillis()
        firestore.collection(COLLECTION_ATTEMPTS)
            .document(payload.id)
            .set(
                mapOf(
                    FIELD_PAYLOAD to json.encodeToString(Attempt.serializer(), payload),
                    "assessmentId" to payload.assessmentId,
                    "userId" to payload.userId,
                    FIELD_UPDATED_AT to now
                ),
                SetOptions.merge()
            ).await()
    }

    private suspend fun pushSubmission(payload: Submission) {
        val now = System.currentTimeMillis()
        firestore.collection(COLLECTION_SUBMISSIONS)
            .document(payload.id)
            .set(
                mapOf(
                    FIELD_PAYLOAD to json.encodeToString(Submission.serializer(), payload),
                    "classworkId" to payload.classworkId,
                    "userId" to payload.userId,
                    FIELD_UPDATED_AT to now
                ),
                SetOptions.merge()
            ).await()
    }

    private suspend fun pushLiveSession(payload: LiveSession) {
        val now = System.currentTimeMillis()
        firestore.collection(COLLECTION_LIVE_SESSIONS)
            .document(payload.id)
            .set(
                mapOf(
                    FIELD_PAYLOAD to json.encodeToString(LiveSession.serializer(), payload),
                    "classworkId" to payload.classworkId,
                    "joinCode" to payload.joinCode,
                    FIELD_UPDATED_AT to now
                ),
                SetOptions.merge()
            ).await()
    }

    private suspend fun pushLiveResponse(payload: LiveResponse) {
        val now = System.currentTimeMillis()
        firestore.collection(COLLECTION_LIVE_SESSIONS)
            .document(payload.liveSessionId)
            .collection(COLLECTION_RESPONSES)
            .document(payload.id)
            .set(
                mapOf(
                    FIELD_PAYLOAD to json.encodeToString(LiveResponse.serializer(), payload),
                    "questionId" to payload.questionId,
                    "userId" to payload.userId,
                    FIELD_UPDATED_AT to now
                ),
                SetOptions.merge()
            ).await()
    }

    private fun classesCollection(): CollectionReference {
        val orgId = orgIdProvider()
        return if (orgId.isNullOrBlank()) {
            firestore.collection(COLLECTION_CLASSES)
        } else {
            firestore.collection(COLLECTION_ORGS)
                .document(orgId)
                .collection(COLLECTION_CLASSES)
        }
    }

    data class RemoteSnapshot(
        val classes: List<Class>,
        val rosters: Map<String, List<Roster>>,
        val classwork: Map<String, List<Classwork>>,
        val attempts: List<Attempt>,
        val submissions: List<Submission>,
        val liveSessions: List<LiveSession>,
        val liveResponses: Map<String, List<LiveResponse>>
    )

    companion object {
        private const val COLLECTION_ORGS = "orgs"
        private const val COLLECTION_CLASSES = "classes"
        private const val COLLECTION_ROSTER = "roster"
        private const val COLLECTION_CLASSWORK = "classwork"
        private const val COLLECTION_ATTEMPTS = "attempts"
        private const val COLLECTION_SUBMISSIONS = "submissions"
        private const val COLLECTION_LIVE_SESSIONS = "liveSessions"
        private const val COLLECTION_RESPONSES = "responses"
        private const val FIELD_PAYLOAD = "payload"
        private const val FIELD_UPDATED_AT = "updatedAt"
    }
}
