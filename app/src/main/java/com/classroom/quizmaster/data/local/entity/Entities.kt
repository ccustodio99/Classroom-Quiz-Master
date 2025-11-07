package com.classroom.quizmaster.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
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
    val updatedAt: Long
)

@Entity(tableName = "participants")
data class ParticipantLocalEntity(
    @PrimaryKey val uid: String,
    val nickname: String,
    val avatar: String,
    val totalPoints: Int,
    val totalTimeMs: Long,
    val joinedAt: Long
)

@Entity(tableName = "attempts")
data class AttemptLocalEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val questionId: String,
    val selectedJson: String,
    val timeMs: Long,
    val correct: Boolean,
    val points: Int,
    val late: Boolean,
    val createdAt: Long
)

@Entity(tableName = "oplog")
data class OpLogEntity(
    @PrimaryKey val id: String,
    val type: String,
    val payloadJson: String,
    val ts: Long,
    val synced: Boolean
)
