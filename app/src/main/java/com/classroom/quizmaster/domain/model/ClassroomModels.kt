package com.classroom.quizmaster.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class JoinPolicy {
    @SerialName("code")
    CODE,
    @SerialName("invite_only")
    INVITE_ONLY,
    @SerialName("organization")
    ORGANIZATION
}

@Serializable
enum class ClassroomStatus {
    Active,
    Archived
}

@Serializable
enum class ClassroomRole {
    Teacher,
    Student,
    Guardian
}

@Serializable
data class ClassroomSettings(
    val allowStudentPosts: Boolean = true,
    val enableGuardianEmails: Boolean = false,
    val gradingCategories: List<String> = listOf("Assessment", "Practice", "Participation"),
    val defaultWeighting: Map<String, Double> = mapOf(
        "Assessment" to 0.5,
        "Practice" to 0.3,
        "Participation" to 0.2
    ),
    val leaderboardEnabled: Boolean = false,
    val presenceHeartbeatMs: Long = 30_000L
)

@Serializable
data class ClassRosterEntry(
    val userId: String,
    val role: ClassroomRole,
    val displayName: String,
    val joinedAt: Long,
    val guardianEmail: String? = null,
    val muted: Boolean = false
)

@Serializable
enum class ClassworkType {
    @SerialName("material")
    MATERIAL,
    @SerialName("quiz")
    QUIZ,
    @SerialName("pretest")
    PRETEST,
    @SerialName("posttest")
    POSTTEST,
    @SerialName("discussion")
    DISCUSSION,
    @SerialName("live")
    LIVE
}

@Serializable
enum class ClassworkStatus {
    Draft,
    Published,
    Archived
}

@Serializable
data class ClassworkTopic(
    val id: String,
    val title: String,
    val position: Int = 0
)

@Serializable
data class ClassworkItem(
    val id: String,
    val classId: String,
    val topicId: String?,
    val title: String,
    val summary: String = "",
    val type: ClassworkType,
    val moduleId: String? = null,
    val assignmentId: String? = null,
    val liveSessionId: String? = null,
    val dueAt: Long? = null,
    val points: Int? = null,
    val attachments: List<String> = emptyList(),
    val status: ClassworkStatus = ClassworkStatus.Draft,
    val createdBy: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
enum class StreamItemType {
    Announcement,
    ClassworkPosted,
    Reminder,
    LiveNow
}

@Serializable
data class ClassStreamItem(
    val id: String,
    val classId: String,
    val type: StreamItemType,
    val headline: String,
    val message: String? = null,
    val authorId: String,
    val pinned: Boolean = false,
    val classworkId: String? = null,
    val createdAt: Long
)

@Serializable
enum class LiveSessionMode {
    Individual,
    Team,
    Anonymous
}

@Serializable
enum class LiveSessionStatus {
    Scheduled,
    InProgress,
    Completed,
    Aborted
}

@Serializable
data class LiveSessionMeta(
    val id: String,
    val classId: String,
    val moduleId: String,
    val hostId: String,
    val assignmentId: String? = null,
    val mode: LiveSessionMode = LiveSessionMode.Individual,
    val status: LiveSessionStatus = LiveSessionStatus.Scheduled,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val lanSessionId: String? = null,
    val fallbackChannel: String? = null,
    val participantCount: Int = 0
)

@Serializable
data class ClassroomProfile(
    val id: String = "classroom-default",
    val code: String = "000000",
    val name: String = "Advisory Class",
    val subject: String = "G11 General Mathematics",
    val description: String = "",
    val gradeLevel: String = "Grade 11",
    val section: String = "",
    val ownerId: String? = null,
    val coTeacherIds: List<String> = emptyList(),
    val joinPolicy: JoinPolicy = JoinPolicy.CODE,
    val orgId: String? = null,
    val status: ClassroomStatus = ClassroomStatus.Active,
    val settings: ClassroomSettings = ClassroomSettings(),
    val stream: List<ClassStreamItem> = emptyList(),
    val topics: List<ClassworkTopic> = emptyList(),
    val classwork: List<ClassworkItem> = emptyList(),
    val roster: List<ClassRosterEntry> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val archivedAt: Long? = null
)
