package com.classroom.quizmaster.agents

import com.classroom.quizmaster.domain.model.Student
import java.util.UUID
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LiveSessionAgentImpl : LiveSessionAgent {
    private val sessions = mutableMapOf<String, LiveState>()
    private val lock = ReentrantLock()

    override fun createSession(moduleId: String): String = lock.withLock {
        val sessionId = UUID.randomUUID().toString().take(6).uppercase()
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

    private data class LiveState(
        val moduleId: String,
        val participants: MutableList<Student> = mutableListOf(),
        val answers: MutableMap<String, MutableList<AnswerPayload>> = mutableMapOf()
    )
}
