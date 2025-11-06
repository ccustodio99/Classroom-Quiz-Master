package com.classroom.quizmaster.agents.impl

import com.classroom.quizmaster.agents.ClassroomAgent
import com.classroom.quizmaster.data.model.Class
import com.classroom.quizmaster.data.model.Roster
import com.classroom.quizmaster.data.model.User
import com.classroom.quizmaster.data.repo.ClassRepo
import com.classroom.quizmaster.data.util.DEFAULT_ORG_ID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassroomAgentImpl @Inject constructor(
    private val classRepo: ClassRepo
) : ClassroomAgent {

    override suspend fun createClass(owner: User, subject: String, section: String): Result<Class> {
        val orgId = resolveOrg(owner.org)
        return runCatching { classRepo.createClass(orgId, owner.copy(org = orgId), subject, section) }
    }

    override suspend fun joinClass(user: User, code: String): Result<Roster> {
        val orgId = resolveOrg(user.org)
        return runCatching { classRepo.joinByCode(orgId, user.copy(org = orgId), code) }
    }

    override suspend fun getRoster(classId: String): List<Roster> =
        classRepo.getRoster(classId)

    private fun resolveOrg(org: String): String = org.takeIf { it.isNotBlank() } ?: DEFAULT_ORG_ID
}
