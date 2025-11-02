package com.classroom.quizmaster.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    Admin,
    Teacher,
    Student
}

@Serializable
enum class AccountStatus {
    PendingApproval,
    Active,
    Suspended,
    Archived
}

@Serializable
data class UserAccount(
    val id: String,
    val email: String,
    val displayName: String,
    val role: UserRole,
    val status: AccountStatus,
    val hashedPassword: String,
    val createdAt: Long,
    val approvedAt: Long? = null,
    val approvedBy: String? = null,
    val lastLoginAt: Long? = null
)

@Serializable
data class AccountCredentials(
    val email: String,
    val password: String
)
