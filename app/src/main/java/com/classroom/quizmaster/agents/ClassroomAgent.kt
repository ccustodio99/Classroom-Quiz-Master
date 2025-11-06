package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.Class
import com.classroom.quizmaster.data.model.Roster
import com.classroom.quizmaster.data.model.User

interface ClassroomAgent {
    suspend fun createClass(owner: User, subject: String, section: String): Result<Class>
    suspend fun joinClass(user: User, code: String): Result<Roster>
    suspend fun getRoster(classId: String): List<Roster>
}
