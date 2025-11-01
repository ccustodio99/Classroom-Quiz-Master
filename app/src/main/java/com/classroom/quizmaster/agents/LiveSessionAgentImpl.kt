package com.classroom.quizmaster.agents

import android.util.Log
import com.classroom.quizmaster.domain.model.Student
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LiveSessionAgentImpl : LiveSessionAgent {
    private val sessions = mutableMapOf<String, LiveState>()
    private val lock = ReentrantLock()

    override fun createSession(moduleId: String): String = lock.withLock {
        var attempts = 0
        var sessionId: String
        do {
            sessionId = generateCode()
            attempts += 1
        } while (sessions.containsKey(sessionId))
        if (attempts > 1) {
            Log.w(TAG, "Session code collision detected after ${attempts - 1} retry/retries")
        }
        val state = LiveState(
            moduleId = moduleId,
            updates = MutableStateFlow(
                LiveSnapshot(
                    moduleId = moduleId,
                    participants = emptyList(),
                    answers = emptyMap()
                )
            )
        )
        sessions[sessionId] = state
        state.publish()
        sessionId
    }

    override fun join(sessionId: String, nickname: String): JoinResult = lock.withLock {
        val state = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        val student = Student(id = UUID.randomUUID().toString(), displayName = nickname)
        state.participants += student
        state.publish()
        JoinResult(student, sessionId)
    }

    override fun submit(sessionId: String, answer: AnswerPayload): Ack = lock.withLock {
        val state = sessions[sessionId] ?: return Ack(false)
        val list = state.answers.getOrPut(answer.itemId) { mutableListOf() }
        val studentId = answer.studentId
        if (studentId != null) {
            val index = list.indexOfFirst { it.studentId == studentId }
            if (index >= 0) {
                list[index] = answer
            } else {
                list += answer
            }
        } else {
            list += answer
        }
        state.publish()
        Ack(true)
    }

    override fun snapshot(sessionId: String): LiveSnapshot = lock.withLock {
        val state = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        state.toSnapshot()
    }

    override fun observe(sessionId: String): Flow<LiveSnapshot> = lock.withLock {
        val state = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        state.updates.asStateFlow()
    }

    private fun generateCode(): String = UUID.randomUUID().toString()
        .replace("-", "")
        .take(6)
        .uppercase()

    private data class LiveState(
        val moduleId: String,
        val participants: MutableList<Student> = mutableListOf(),
        val answers: MutableMap<String, MutableList<AnswerPayload>> = mutableMapOf(),
        val updates: MutableStateFlow<LiveSnapshot>
    )

    private fun LiveState.publish() {
        updates.value = toSnapshot()
    }

    private fun LiveState.toSnapshot(): LiveSnapshot =
        LiveSnapshot(
            moduleId = moduleId,
            participants = participants.toList(),
            answers = answers.mapValues { entry -> entry.value.toList() }
        )

    companion object {
        private const val TAG = "LiveSessionAgent"
    }
}
