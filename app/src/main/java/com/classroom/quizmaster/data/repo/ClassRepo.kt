package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.model.Class
import com.classroom.quizmaster.data.model.Roster
import com.classroom.quizmaster.data.model.User
import kotlinx.coroutines.flow.Flow

interface ClassRepo {
    fun myClasses(org: String): Flow<List<Class>>
    suspend fun createClass(org: String, owner: User, subject: String, section: String): Class
    suspend fun joinByCode(org: String, user: User, code: String): Roster
    suspend fun getRoster(classPath: String): List<Roster>
}
