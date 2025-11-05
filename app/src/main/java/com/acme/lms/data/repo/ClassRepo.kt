package com.acme.lms.data.repo

import com.acme.lms.data.model.Class
import com.acme.lms.data.model.Roster
import com.acme.lms.data.model.User
import kotlinx.coroutines.flow.Flow

interface ClassRepo {
    fun myClasses(org: String): Flow<List<Class>>
    suspend fun createClass(org: String, owner: User, subject: String, section: String): Class
    suspend fun joinByCode(org: String, user: User, code: String): Roster
    suspend fun getRoster(classPath: String): List<Roster>
}
