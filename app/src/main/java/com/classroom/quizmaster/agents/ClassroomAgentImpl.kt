package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.Class
import com.classroom.quizmaster.data.model.Roster
import com.classroom.quizmaster.data.model.User

class ClassroomAgentImpl : ClassroomAgent {
    override suspend fun createClass(owner: User, subject: String, section: String): Result<Class> {
        TODO("Not yet implemented")
    }

    override suspend fun joinClass(user: User, code: String): Result<Roster> {
        TODO("Not yet implemented")
    }

    override suspend fun getRoster(classId: String): List<Roster> {
        TODO("Not yet implemented")
    }
}
