package com.classroom.quizmaster.domain.agent.impl

import com.classroom.quizmaster.data.local.BlueprintLocalDataSource
import com.classroom.quizmaster.domain.agent.PresenceAgent
import com.classroom.quizmaster.domain.model.ClassRole
import com.classroom.quizmaster.domain.model.PersonaType
import com.classroom.quizmaster.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PresenceAgentImpl(
    private val localData: BlueprintLocalDataSource,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : PresenceAgent {

    private val mutex = Mutex()
    private val presenceState = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    override fun goOnline(classId: String, userId: String) {
        scope.launch {
            mutex.withLock {
                val current = presenceState.value[classId].orEmpty()
                val updated = current + userId
                presenceState.value = presenceState.value + (classId to updated)
            }
        }
    }

    override fun goOffline(classId: String, userId: String) {
        scope.launch {
            mutex.withLock {
                val current = presenceState.value[classId].orEmpty()
                val updated = current - userId
                presenceState.value = presenceState.value + (classId to updated)
            }
        }
    }

    override fun getOnlineUsers(classId: String): Flow<List<User>> =
        presenceState.map { state ->
            val onlineIds = state[classId].orEmpty()
            val roster = localData.rosterFor(classId)
            onlineIds.map { id ->
                val role = roster.firstOrNull { it.userId == id }?.role
                val persona = when (role) {
                    ClassRole.TEACHER -> PersonaType.Instructor
                    ClassRole.STUDENT -> PersonaType.Learner
                    else -> PersonaType.Learner
                }
                User(
                    id = id,
                    name = id,
                    role = persona,
                    org = null,
                    email = "$id@classroom.local"
                )
            }
        }
}
