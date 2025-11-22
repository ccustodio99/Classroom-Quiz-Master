package com.classroom.quizmaster.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "teachers",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class TeacherEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String,
    val createdAt: Long
)

@Entity(
    tableName = "students",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class StudentEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String,
    val createdAt: Long
)

@Entity(
    tableName = "join_requests",
    indices = [
        Index(value = ["studentId"]),
        Index(value = ["classroomId"]),
        Index(value = ["teacherId"]),
        Index(value = ["status"])
    ]
)
data class JoinRequestEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val classroomId: String,
    val teacherId: String,
    val status: String,
    val createdAt: Long,
    val resolvedAt: Long?
)

@Entity(
    tableName = "classrooms",
    indices = [
        Index(value = ["teacherId"]),
        Index(value = ["teacherId", "name"], unique = true),
        Index(value = ["createdAt"]),
        Index(value = ["isArchived"])
    ]
)
data class ClassroomEntity(
    @PrimaryKey val id: String,
    val teacherId: String,
    val name: String,
    val grade: String,
    val subject: String,
    val joinCode: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
    val archivedAt: Long?,
    val students: List<String>
)

@Entity(
    tableName = "topics",
    indices = [
        Index(value = ["classroomId"]),
        Index(value = ["teacherId"]),
        Index(value = ["classroomId", "name"], unique = true),
        Index(value = ["isArchived"])
    ]
)
data class TopicEntity(
    @PrimaryKey val id: String,
    val classroomId: String,
    val teacherId: String,
    val name: String,
    val description: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
    val archivedAt: Long?
)

@Entity(
    tableName = "quizzes",
    indices = [
        Index(value = ["teacherId"]),
        Index(value = ["classroomId"]),
        Index(value = ["topicId"]),
        Index(value = ["createdAt"]),
        Index(value = ["isArchived"])
    ]
)
data class QuizEntity(
    @PrimaryKey val id: String,
    val teacherId: String,
    val classroomId: String,
    val topicId: String,
    val title: String,
    val defaultTimePerQ: Int,
    val shuffle: Boolean,
    val questionCount: Int,
    @ColumnInfo(defaultValue = "'STANDARD'")
    val category: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
    val archivedAt: Long?
)

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["quizId"]),
        Index(value = ["type"])
    ]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val type: String,
    val stem: String,
    @ColumnInfo(name = "choicesJson") val choicesJson: String,
    @ColumnInfo(name = "answerKeyJson") val answerKeyJson: String,
    val explanation: String,
    val mediaType: String?,
    val mediaUrl: String?,
    val timeLimitSeconds: Int,
    val position: Int,
    val updatedAt: Long
)

@Entity(
    tableName = "sessions",
    indices = [
        Index(value = ["teacherId"]),
        Index(value = ["classroomId"]),
        Index(value = ["joinCode"], unique = true),
        Index(value = ["joinCode", "status"])
    ]
)
data class SessionLocalEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val teacherId: String,
    val classroomId: String,
    val joinCode: String,
    val status: String,
    val currentIndex: Int,
    val reveal: Boolean,
    val hideLeaderboard: Boolean,
    val lockAfterQ1: Boolean,
    val startedAt: Long?,
    val endedAt: Long?,
    val updatedAt: Long
)

@Entity(
    tableName = "participants",
    primaryKeys = ["sessionId", "uid"],
    indices = [
        Index(value = ["sessionId", "totalPoints", "totalTimeMs"])
    ]
)
data class ParticipantLocalEntity(
    val sessionId: String,
    val uid: String,
    val nickname: String,
    val avatar: String,
    val totalPoints: Int,
    val totalTimeMs: Long,
    val joinedAt: Long
)

@Entity(
    tableName = "attempts",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["questionId"]),
        Index(value = ["uid"]),
        Index(value = ["sessionId", "questionId"])
    ]
)
data class AttemptLocalEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val uid: String,
    val questionId: String,
    val selectedJson: String,
    val timeMs: Long,
    val correct: Boolean,
    val points: Int,
    val late: Boolean,
    val createdAt: Long,
    val syncedAt: Long?,
    val sequenceNumber: Long
)

@Entity(
    tableName = "oplog",
    indices = [
        Index(value = ["synced"]),
        Index(value = ["ts"])
    ]
)
data class OpLogEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payloadJson: String,
    val ts: Long,
    val synced: Boolean,
    val retryCount: Int
)

@Entity(
    tableName = "assignments",
    indices = [
        Index(value = ["classroomId"]),
        Index(value = ["quizId"]),
        Index(value = ["openAt"]),
        Index(value = ["topicId"]),
        Index(value = ["isArchived"])
    ]
)
data class AssignmentLocalEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val classroomId: String,
    @ColumnInfo(defaultValue = "''")
    val topicId: String,
    val openAt: Long,
    val closeAt: Long,
    val attemptsAllowed: Int,
    @ColumnInfo(defaultValue = "'BEST'")
    val scoringMode: String,
    val revealAfterSubmit: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "0")
    val isArchived: Boolean,
    val archivedAt: Long?
)

@Entity(
    tableName = "submissions",
    primaryKeys = ["assignmentId", "uid"],
    indices = [
        Index(value = ["assignmentId", "bestScore"]),
        Index(value = ["assignmentId", "updatedAt"])
    ]
)
data class SubmissionLocalEntity(
    val assignmentId: String,
    val uid: String,
    val bestScore: Int,
    val lastScore: Int,
    val attempts: Int,
    val updatedAt: Long
)

@Entity(tableName = "lan_session_meta")
data class LanSessionMetaEntity(
    @PrimaryKey val sessionId: String,
    val token: String,
    val hostIp: String,
    val port: Int,
    val startedAt: Long,
    val rotationCount: Int
)

@Entity(
    tableName = "learning_materials",
    indices = [
        Index(value = ["teacherId"]),
        Index(value = ["classroomId"]),
        Index(value = ["topicId"]),
        Index(value = ["isArchived"])
    ]
)
data class LearningMaterialEntity(
    @PrimaryKey val id: String,
    val teacherId: String,
    val classroomId: String,
    val classroomName: String,
    val topicId: String,
    val topicName: String,
    val title: String,
    val description: String,
    val body: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isArchived: Boolean,
    val archivedAt: Long?
)

@Entity(
    tableName = "material_attachments",
    indices = [
        Index(value = ["materialId"]),
        Index(value = ["type"])
    ]
)
data class MaterialAttachmentEntity(
    @PrimaryKey val id: String,
    val materialId: String,
    val displayName: String,
    val type: String,
    val uri: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val downloadedAt: Long?,
    val metadataJson: String
)

data class MaterialWithAttachments(
    @Embedded val material: LearningMaterialEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "materialId"
    )
    val attachments: List<MaterialAttachmentEntity>
)
