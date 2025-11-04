package com.acme.lms.agents

import com.acme.lms.data.model.User
import kotlinx.coroutines.flow.Flow

interface PresenceAgent {
    fun goOnline(classId: String, userId: String)
    fun goOffline(classId: String, userId: String)
    fun getOnlineUsers(classId: String): Flow<List<User>>
}
