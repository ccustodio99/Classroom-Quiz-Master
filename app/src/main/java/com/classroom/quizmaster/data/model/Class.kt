package com.classroom.quizmaster.data.model

data class Class(
    val id: String = "",
    val orgId: String = "",
    val code: String = "",
    val subject: String = "",
    val section: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val coTeachers: List<String> = emptyList(),
    val joinPolicy: JoinPolicy = JoinPolicy.OPEN
)

enum class JoinPolicy { OPEN, INVITE_ONLY }

data class Roster(
    val id: String = "",
    val classId: String = "",
    val userId: String = "",
    val role: RosterRole = RosterRole.LEARNER
)

enum class RosterRole { OWNER, CO_TEACHER, LEARNER }
