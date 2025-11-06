package com.classroom.quizmaster.data.model

/**
 * Basic representation of an authenticated user in the LMS.
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.LEARNER,
    val org: String = ""
)

enum class UserRole { LEARNER, TEACHER, ADMIN }
