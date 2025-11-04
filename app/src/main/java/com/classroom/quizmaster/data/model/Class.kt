package com.classroom.quizmaster.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Class(
    val id: String,
    val code: String,
    val section: String,
    val subject: String,
    val ownerId: String,
    val coTeachers: List<String> = emptyList(),
    val joinPolicy: String = "invite_only" // or "open"
)

@Serializable
data class Roster(
    val classId: String,
    val userId: String,
    val role: String // "teacher" or "student"
)
