package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.User
import kotlinx.coroutines.flow.Flow

interface PresenceAgent {
    fun goOnline(classId: String, userId: String)
    fun goOffline(classId: String, userId: String)
    fun getOnlineUsers(classId: String): Flow<List<User>>
}
