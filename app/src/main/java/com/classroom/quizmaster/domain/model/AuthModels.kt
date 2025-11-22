package com.classroom.quizmaster.domain.model

/**
 * Simple view of the current authentication state exposed to the UI layer.
 */
data class AuthState(
    val userId: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val isAuthenticated: Boolean = false,
    val isTeacher: Boolean = false,
    val role: UserRole = UserRole.STUDENT,
    val teacherProfile: Teacher? = null
)

enum class UserRole { TEACHER, STUDENT }
