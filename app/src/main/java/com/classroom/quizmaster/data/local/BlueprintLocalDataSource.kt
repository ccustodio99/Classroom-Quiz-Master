package com.classroom.quizmaster.data.local

import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.Class
import com.classroom.quizmaster.domain.model.Classwork
import com.classroom.quizmaster.domain.model.LiveResponse
import com.classroom.quizmaster.domain.model.LiveSession
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.Roster
import com.classroom.quizmaster.domain.model.Submission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class BlueprintLocalDataSource(
    private val store: BlueprintLocalStore,
    private val json: Json
) {

    val snapshot: StateFlow<LmsSnapshot> = store.snapshot

    fun observeClasses(): Flow<List<Class>> =
        snapshot.map { it.classes.values.sortedBy { cls -> cls.subject.lowercase() } }

    suspend fun upsertClass(cls: Class): Class {
        store.update { state ->
            state.copy(classes = state.classes + (cls.id to cls))
        }
        return cls
    }

    suspend fun removeClass(classId: String) {
        store.update { state ->
            state.copy(
                classes = state.classes - classId,
                rosters = state.rosters - classId,
                classwork = state.classwork - classId,
                liveSessions = state.liveSessions.filterValues { it.classworkId != classId },
                liveResponses = state.liveResponses.filterKeys { sessionId ->
                    state.liveSessions[sessionId]?.classworkId != classId
                }
            )
        }
    }

    fun findClassByCode(code: String): Class? =
        snapshot.value.classes.values.firstOrNull { it.code.equals(code, ignoreCase = true) }

    fun findClassById(id: String): Class? = snapshot.value.classes[id]

    fun rosterFor(classId: String): List<Roster> =
        snapshot.value.rosters[classId].orEmpty()

    suspend fun upsertRosterEntry(classId: String, roster: Roster): Roster {
        store.update { state ->
            val current = state.rosters[classId].orEmpty()
            val updated = current
                .filterNot { it.userId == roster.userId }
                .plus(roster)
            state.copy(rosters = state.rosters + (classId to updated))
        }
        return roster
    }

    suspend fun removeRosterEntry(classId: String, userId: String) {
        store.update { state ->
            val updated = state.rosters[classId].orEmpty().filterNot { it.userId == userId }
            state.copy(rosters = state.rosters + (classId to updated))
        }
    }

    fun classworkFor(classId: String): List<ClassworkBundle> =
        snapshot.value.classwork[classId].orEmpty()

    suspend fun upsertClasswork(bundle: ClassworkBundle): ClassworkBundle {
        store.update { state ->
            val current = state.classwork[bundle.item.classId].orEmpty()
            val updated = current
                .filterNot { it.item.id == bundle.item.id }
                .plus(bundle)
                .sortedBy { it.item.dueAt ?: Long.MAX_VALUE }
            state.copy(classwork = state.classwork + (bundle.item.classId to updated))
        }
        return bundle
    }

    suspend fun removeClasswork(classId: String, classworkId: String) {
        store.update { state ->
            val current = state.classwork[classId].orEmpty()
            val updated = current.filterNot { it.item.id == classworkId }
            state.copy(
                classwork = state.classwork + (classId to updated),
                attempts = state.attempts - classworkId,
                submissions = state.submissions - classworkId
            )
        }
    }

    fun findClasswork(classworkId: String): ClassworkBundle? =
        snapshot.value.classwork.values.flatten().firstOrNull { it.item.id == classworkId }

    suspend fun recordAttempt(attempt: Attempt): Attempt {
        store.update { state ->
            val updated = state.attempts[attempt.assessmentId].orEmpty()
                .filterNot { it.id == attempt.id }
                .plus(attempt)
            state.copy(attempts = state.attempts + (attempt.assessmentId to updated))
        }
        return attempt
    }

    fun attemptsFor(classworkId: String, userId: String? = null): List<Attempt> {
        val all = snapshot.value.attempts[classworkId].orEmpty()
        return userId?.let { id -> all.filter { it.userId == id } } ?: all
    }

    suspend fun recordSubmission(submission: Submission): Submission {
        store.update { state ->
            val updated = state.submissions[submission.classworkId].orEmpty()
                .filterNot { it.id == submission.id }
                .plus(submission)
            state.copy(submissions = state.submissions + (submission.classworkId to updated))
        }
        return submission
    }

    fun submissionsFor(classworkId: String): List<Submission> =
        snapshot.value.submissions[classworkId].orEmpty()

    suspend fun upsertLiveSession(session: LiveSession): LiveSession {
        store.update { state ->
            state.copy(liveSessions = state.liveSessions + (session.id to session))
        }
        return session
    }

    suspend fun removeLiveSession(sessionId: String) {
        store.update { state ->
            state.copy(
                liveSessions = state.liveSessions - sessionId,
                liveResponses = state.liveResponses - sessionId
            )
        }
    }

    fun liveSession(sessionId: String): LiveSession? = snapshot.value.liveSessions[sessionId]

    fun liveSessionByJoinCode(joinCode: String): LiveSession? =
        snapshot.value.liveSessions.values.firstOrNull { it.joinCode == joinCode }

    suspend fun recordLiveResponse(response: LiveResponse): LiveResponse {
        store.update { state ->
            val updated = state.liveResponses[response.liveSessionId].orEmpty() + response
            state.copy(liveResponses = state.liveResponses + (response.liveSessionId to updated))
        }
        return response
    }

    fun responsesForSession(sessionId: String): List<LiveResponse> =
        snapshot.value.liveResponses[sessionId].orEmpty()

    suspend fun enqueueSync(entityType: SyncEntityType, entityId: String, payload: Any) {
        val payloadJson = when (entityType) {
            SyncEntityType.CLASS -> json.encodeToString(
                com.classroom.quizmaster.domain.model.Class.serializer(),
                payload as com.classroom.quizmaster.domain.model.Class
            )
            SyncEntityType.ROSTER -> json.encodeToString(
                com.classroom.quizmaster.domain.model.Roster.serializer(),
                payload as com.classroom.quizmaster.domain.model.Roster
            )
            SyncEntityType.CLASSWORK -> json.encodeToString(
                com.classroom.quizmaster.domain.model.Classwork.serializer(),
                payload as com.classroom.quizmaster.domain.model.Classwork
            )
            SyncEntityType.ATTEMPT -> json.encodeToString(
                com.classroom.quizmaster.domain.model.Attempt.serializer(),
                payload as com.classroom.quizmaster.domain.model.Attempt
            )
            SyncEntityType.SUBMISSION -> json.encodeToString(
                com.classroom.quizmaster.domain.model.Submission.serializer(),
                payload as com.classroom.quizmaster.domain.model.Submission
            )
            SyncEntityType.LIVE_SESSION -> json.encodeToString(
                com.classroom.quizmaster.domain.model.LiveSession.serializer(),
                payload as com.classroom.quizmaster.domain.model.LiveSession
            )
            SyncEntityType.LIVE_RESPONSE -> json.encodeToString(
                com.classroom.quizmaster.domain.model.LiveResponse.serializer(),
                payload as com.classroom.quizmaster.domain.model.LiveResponse
            )
        }
        val operation = PendingSync(
            id = UUID.randomUUID().toString(),
            entityType = entityType,
            entityId = entityId,
            payloadJson = payloadJson,
            queuedAt = System.currentTimeMillis()
        )
        store.update { state ->
            state.copy(pendingSync = state.pendingSync + operation)
        }
    }

    suspend fun markSyncSuccess(operationId: String) {
        store.update { state ->
            state.copy(pendingSync = state.pendingSync.filterNot { it.id == operationId })
        }
    }

    suspend fun incrementSyncAttempts(operationId: String) {
        store.update { state ->
            val updated = state.pendingSync.map {
                if (it.id == operationId) it.copy(attempts = it.attempts + 1) else it
            }
            state.copy(pendingSync = updated)
        }
    }
}
