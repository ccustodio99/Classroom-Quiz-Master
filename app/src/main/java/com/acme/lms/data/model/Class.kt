package com.acme.lms.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Class(
    val id: String = "",
    val orgId: String = "",
    val code: String = "",
    val section: String = "",
    val subject: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val coTeachers: List<String> = emptyList(),
    val joinPolicy: String = "code"
)

@Serializable
data class Roster(
    val id: String = "",
    val classId: String = "",
    val userId: String = "",
    val role: String = "learner",
    val joinedAt: Long? = null
)
