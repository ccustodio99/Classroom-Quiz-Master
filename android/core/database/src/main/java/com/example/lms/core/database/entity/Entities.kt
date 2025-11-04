package com.example.lms.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String?,
    val role: String,
    val org: String,
)

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey val id: String,
    val code: String,
    val section: String,
    val subject: String,
    val ownerId: String,
    val coTeachers: String,
    val joinPolicy: String,
)

@Entity(tableName = "roster")
data class RosterEntity(
    @PrimaryKey(autoGenerate = true) val pk: Long = 0,
    val classId: String,
    val userId: String,
    val role: String,
)

@Entity(tableName = "classwork")
data class ClassworkEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val topic: String,
    val title: String,
    val type: String,
    val dueAt: Long?,
    val points: Int?,
)

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val classworkId: String,
    val type: String,
    val stem: String,
    val media: String,
    val options: String,
    val answerKey: String?,
    val config: String,
)

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val classworkId: String,
    val score: Double,
    val duration: Long,
    val passed: Boolean,
    val payload: String,
    val createdAt: Long,
)

@Entity(tableName = "live_sessions")
data class LiveSessionEntity(
    @PrimaryKey val id: String,
    val classId: String,
    val assignmentId: String,
    val state: String,
    val startedAt: Long?,
    val endedAt: Long?,
)

@Entity(tableName = "live_responses")
data class LiveResponseEntity(
    @PrimaryKey(autoGenerate = true) val pk: Long = 0,
    val sessionId: String,
    val userId: String,
    val questionId: String,
    val answerPayload: String,
    val correct: Boolean,
    val latencyMs: Long,
    val scoreDelta: Double,
    val timestamp: Long,
)

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payloadType: String,
    val payload: String,
    val createdAt: Long,
)

