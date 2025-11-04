package com.classroom.quizmaster.domain.agent.impl

import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.data.local.ClassworkBundle
import com.classroom.quizmaster.data.local.SyncEntityType
import com.classroom.quizmaster.domain.agent.ClassroomAgent
import com.classroom.quizmaster.domain.model.Class
import com.classroom.quizmaster.domain.model.ClassRole
import com.classroom.quizmaster.domain.model.ClassworkType
import com.classroom.quizmaster.domain.model.PersonaType
import com.classroom.quizmaster.domain.model.Roster
import com.classroom.quizmaster.domain.model.User
import java.util.UUID

class ClassroomAgentImpl(
    private val localData: BlueprintLocalDataSource
) : ClassroomAgent {

    override suspend fun createClass(
        owner: User,
        subject: String,
        section: String
    ): Result<Class> = runCatching {
        val classId = UUID.randomUUID().toString()
        val joinCode = generateJoinCode()
        val classroom = Class(
            id = classId,
            code = joinCode,
            section = section.trim(),
            subject = subject.trim(),
            ownerId = owner.id,
            coTeachers = emptyList()
        )
        localData.upsertClass(classroom)

        val teacherRoster = Roster(
            classId = classId,
            userId = owner.id,
            role = ClassRole.TEACHER
        )
        localData.upsertRosterEntry(classId, teacherRoster)

        localData.enqueueSync(
            entityType = SyncEntityType.CLASS,
            entityId = classId,
            payload = classroom
        )
        localData.enqueueSync(
            entityType = SyncEntityType.ROSTER,
            entityId = "${classId}:${owner.id}",
            payload = teacherRoster
        )

        // Seed a stream entry and skeleton tabs.
        seedInitialClasswork(classId)
        classroom
    }

    override suspend fun joinClass(user: User, code: String): Result<Roster> = runCatching {
        val classroom = localData.findClassByCode(code.trim())
            ?: throw IllegalArgumentException("Class with code $code was not found.")
        val rosterRole = when (user.role) {
            PersonaType.Instructor -> ClassRole.TEACHER
            PersonaType.Admin -> ClassRole.TEACHER
            else -> ClassRole.STUDENT
        }
        val roster = Roster(
            classId = classroom.id,
            userId = user.id,
            role = rosterRole
        )
        localData.upsertRosterEntry(classroom.id, roster)
        localData.enqueueSync(
            entityType = SyncEntityType.ROSTER,
            entityId = "${classroom.id}:${user.id}",
            payload = roster
        )
        roster
    }

    override suspend fun getRoster(classId: String): List<Roster> =
        localData.rosterFor(classId)

    private suspend fun seedInitialClasswork(classId: String) {
        val welcome = ClassworkBundle(
            item = com.classroom.quizmaster.domain.model.Classwork(
                id = UUID.randomUUID().toString(),
                classId = classId,
                topic = "Welcome",
                type = ClassworkType.MATERIAL,
                title = "Getting started",
                dueAt = null,
                points = null
            ),
            questions = emptyList()
        )
        localData.upsertClasswork(welcome)
        localData.enqueueSync(
            entityType = SyncEntityType.CLASSWORK,
            entityId = welcome.item.id,
            payload = welcome.item
        )
    }

    private fun generateJoinCode(): String =
        buildString {
            repeat(6) {
                append(CHAR_POOL.random())
            }
        }

    companion object {
        private val CHAR_POOL = ('A'..'Z') + ('0'..'9')
    }
}
