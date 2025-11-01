package com.classroom.quizmaster.agents

import android.util.Log
import com.classroom.quizmaster.domain.model.Student
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
        sessions[sessionId] = LiveState(moduleId)
        sessionId
    }

    override fun join(sessionId: String, nickname: String): JoinResult = lock.withLock {
        val state = sessions[sessionId] ?: error("Session not found")
        val student = Student(id = UUID.randomUUID().toString(), displayName = nickname)
        state.participants += student
        JoinResult(student, sessionId)
    }

    override fun submit(sessionId: String, answer: AnswerPayload): Ack = lock.withLock {
        val state = sessions[sessionId] ?: return Ack(false)
        val list = state.answers.getOrPut(answer.itemId) { mutableListOf() }
        list += answer
        Ack(true)
    }

    override fun snapshot(sessionId: String): LiveSnapshot = lock.withLock {
        val state = sessions[sessionId] ?: error("Session not found")
        LiveSnapshot(
            moduleId = state.moduleId,
            participants = state.participants.toList(),
            answers = state.answers.mapValues { it.value.toList() }
        )
    }

    private fun generateCode(): String = UUID.randomUUID().toString()
        .replace("-", "")
        .take(6)
        .uppercase()

    private data class LiveState(
        val moduleId: String,
        val participants: MutableList<Student> = mutableListOf(),
        val answers: MutableMap<String, MutableList<AnswerPayload>> = mutableMapOf()
    )

    companion object {
        private const val TAG = "LiveSessionAgent"
    }
}
