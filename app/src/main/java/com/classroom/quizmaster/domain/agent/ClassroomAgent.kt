package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.Class
import com.classroom.quizmaster.domain.model.Roster
import com.classroom.quizmaster.domain.model.User
import kotlinx.coroutines.flow.Flow

interface ClassroomAgent {
  suspend fun createClass(owner: User, subject: String, section: String): Result<Class>
  suspend fun joinClass(user: User, code: String): Result<Roster>
  suspend fun getRoster(classId: String): List<Roster>
}
