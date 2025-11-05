package com.acme.lms.agents

import com.example.lms.core.model.Class
import com.example.lms.core.model.Roster
import com.example.lms.core.model.User

interface ClassroomAgent {
    suspend fun createClass(owner: User, subject: String, section: String): Result<Class>
    suspend fun joinClass(user: User, code: String): Result<Roster>
    suspend fun getRoster(classId: String): List<Roster>
}
