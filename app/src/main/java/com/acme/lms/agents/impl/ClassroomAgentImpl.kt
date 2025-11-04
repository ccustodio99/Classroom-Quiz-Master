package com.acme.lms.agents.impl

import com.acme.lms.agents.ClassroomAgent
import com.acme.lms.data.model.Class
import com.acme.lms.data.model.Roster
import com.acme.lms.data.model.User
import com.acme.lms.data.repo.ClassRepo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClassroomAgentImpl @Inject constructor(
    private val classRepo: ClassRepo
) : ClassroomAgent {

    override suspend fun createClass(owner: User, subject: String, section: String): Result<Class> =
        runCatching {
            require(owner.org.isNotEmpty()) { "Owner must belong to an org" }
            classRepo.createClass(owner.org, owner, subject, section)
        }

    override suspend fun joinClass(user: User, code: String): Result<Roster> =
        runCatching {
            require(user.org.isNotEmpty()) { "User must belong to an org" }
            classRepo.joinByCode(user.org, user, code)
        }

    override suspend fun getRoster(classId: String): List<Roster> =
        classRepo.getRoster(classId)
}
