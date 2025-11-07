package com.classroom.quizmaster.domain.model

data class AuthState(
    val userId: String? = null,
    val displayName: String? = null,
    val isAuthenticated: Boolean = false,
    val role: UserRole = UserRole.STUDENT
)

enum class UserRole { TEACHER, STUDENT }
