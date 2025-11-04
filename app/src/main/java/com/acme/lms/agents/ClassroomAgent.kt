package com.acme.lms.agents

import com.acme.lms.data.model.Class
import com.acme.lms.data.model.Roster
import com.acme.lms.data.model.User

interface ClassroomAgent {
    suspend fun createClass(owner: User, subject: String, section: String): Result<Class>
    suspend fun joinClass(user: User, code: String): Result<Roster>
    suspend fun getRoster(classId: String): List<Roster>
}
