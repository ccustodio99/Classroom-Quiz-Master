package com.classroom.quizmaster.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: String, // "learner", "instructor", "admin"
    val org: String? = null
)
