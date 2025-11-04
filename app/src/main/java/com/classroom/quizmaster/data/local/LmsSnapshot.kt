package com.classroom.quizmaster.data.local

import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.Class
import com.classroom.quizmaster.domain.model.Classwork
import com.classroom.quizmaster.domain.model.LiveResponse
import com.classroom.quizmaster.domain.model.LiveSession
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.Roster
import com.classroom.quizmaster.domain.model.Submission
import kotlinx.serialization.Serializable

@Serializable
data class LmsSnapshot(
    val classes: Map<String, Class> = emptyMap(),
    val rosters: Map<String, List<Roster>> = emptyMap(),
    val classwork: Map<String, List<ClassworkBundle>> = emptyMap(),
    val attempts: Map<String, List<Attempt>> = emptyMap(),
    val submissions: Map<String, List<Submission>> = emptyMap(),
    val liveSessions: Map<String, LiveSession> = emptyMap(),
    val liveResponses: Map<String, List<LiveResponse>> = emptyMap(),
    val pendingSync: List<PendingSync> = emptyList()
)

@Serializable
data class ClassworkBundle(
    val item: Classwork,
    val questions: List<Question> = emptyList()
)

@Serializable
data class PendingSync(
    val id: String,
    val entityType: SyncEntityType,
    val entityId: String,
    val payloadJson: String,
    val queuedAt: Long,
    val attempts: Int = 0
)

@Serializable
enum class SyncEntityType {
    CLASS,
    ROSTER,
    CLASSWORK,
    ATTEMPT,
    SUBMISSION,
    LIVE_SESSION,
    LIVE_RESPONSE
}
