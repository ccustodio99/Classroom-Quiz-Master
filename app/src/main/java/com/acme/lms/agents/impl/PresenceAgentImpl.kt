package com.acme.lms.agents.impl

import com.acme.lms.agents.PresenceAgent
import com.acme.lms.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenceAgentImpl @Inject constructor() : PresenceAgent {

    private val onlineUsers = mutableMapOf<String, MutableStateFlow<List<User>>>()

    override fun goOnline(classId: String, userId: String) {
        val flow = onlineUsers.getOrPut(classId) { MutableStateFlow(emptyList()) }
        if (flow.value.none { it.id == userId }) {
            flow.value = flow.value + User(id = userId)
        }
    }

    override fun goOffline(classId: String, userId: String) {
        val flow = onlineUsers[classId] ?: return
        flow.value = flow.value.filterNot { it.id == userId }
    }

    override fun getOnlineUsers(classId: String): Flow<List<User>> =
        onlineUsers.getOrPut(classId) { MutableStateFlow(emptyList()) }
}
