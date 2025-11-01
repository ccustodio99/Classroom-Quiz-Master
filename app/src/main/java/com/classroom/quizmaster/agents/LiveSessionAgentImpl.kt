package com.classroom.quizmaster.agents

import android.util.Log
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.lan.LanAnswerAck
import com.classroom.quizmaster.lan.LanJoinAck
import com.classroom.quizmaster.lan.LiveSessionLanFactory
import com.classroom.quizmaster.lan.LiveSessionLanHost
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LiveSessionAgentImpl(
    private val lanFactory: LiveSessionLanFactory = LiveSessionLanFactory()
) : LiveSessionAgent {
    private val sessions = mutableMapOf<String, LiveState>()
    private val lock = ReentrantLock()

    override fun createSession(moduleId: String): String = lock.withLock {
        shutdownLanSessionsLocked()
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
                    answers = emptyMap(),
                    activeItemId = null,
                    activePrompt = null,
                    activeObjective = null
                )
            )
        )
        val lanSession = lanFactory.create(
            sessionId = sessionId,
            moduleId = moduleId,
            onJoin = { nickname -> handleLanJoin(sessionId, nickname) },
            onAnswer = { payload -> handleLanAnswer(sessionId, payload) },
            snapshotProvider = { lock.withLock { sessions[sessionId]?.toSnapshot() } }
        )
        state.lanSession = lanSession
        sessions[sessionId] = state
        lanSession.start()
        state.publish()
        sessionId
    }

    override fun join(sessionId: String, nickname: String): JoinResult = lock.withLock {
        val state = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        val student = state.registerParticipant(nickname)
        state.publish()
        JoinResult(student, sessionId)
    }

    override fun submit(sessionId: String, answer: AnswerPayload): Ack = lock.withLock {
        val state = sessions[sessionId] ?: return Ack(false)
        val result = state.recordAnswer(answer)
        if (result.accepted) {
            state.publish()
        }
        Ack(result.accepted)
    }

    override fun snapshot(sessionId: String): LiveSnapshot = lock.withLock {
        val state = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        state.toSnapshot()
    }

    override fun observe(sessionId: String): Flow<LiveSnapshot> = lock.withLock {
        val state = sessions[sessionId] ?: throw IllegalArgumentException("Session not found")
        state.updates.asStateFlow()
    }

    override fun setActiveItem(
        sessionId: String,
        itemId: String?,
        prompt: String?,
        objective: String?
    ): Boolean = lock.withLock {
        val state = sessions[sessionId] ?: return false
        state.activeItemId = itemId
        state.activePrompt = prompt
        state.activeObjective = objective
        state.publish()
        true
    }

    private fun generateCode(): String = ThreadLocalRandom.current()
        .nextInt(100_000, 1_000_000)
        .toString()

    private data class LiveState(
        val moduleId: String,
        val participants: MutableList<Student> = mutableListOf(),
        val answers: MutableMap<String, MutableList<AnswerPayload>> = mutableMapOf(),
        val updates: MutableStateFlow<LiveSnapshot>,
        var activeItemId: String? = null,
        var activePrompt: String? = null,
        var activeObjective: String? = null,
        var lanSession: LiveSessionLanHost? = null
    )

    private fun LiveState.publish() {
        val snapshot = toSnapshot()
        updates.value = snapshot
        lanSession?.broadcast(snapshot)
    }

    private fun LiveState.toSnapshot(): LiveSnapshot =
        LiveSnapshot(
            moduleId = moduleId,
            participants = participants.toList(),
            answers = answers.mapValues { entry -> entry.value.toList() },
            activeItemId = activeItemId,
            activePrompt = activePrompt,
            activeObjective = activeObjective
        )

    private fun LiveState.registerParticipant(nickname: String): Student {
        val existing = participants.firstOrNull { it.displayName.equals(nickname, ignoreCase = true) }
        if (existing != null) {
            return existing
        }
        val student = Student(id = UUID.randomUUID().toString(), displayName = nickname)
        participants += student
        lanSession?.notifyJoin(student)
        return student
    }

    private fun LiveState.recordAnswer(answer: AnswerPayload): AnswerAcceptance {
        val targetItem = answer.itemId.ifBlank {
            activeItemId ?: return AnswerAcceptance(false, reason = "No active question")
        }
        val list = answers.getOrPut(targetItem) { mutableListOf() }
        val studentId = answer.studentId ?: return AnswerAcceptance(false, reason = "Missing participant")
        val participant = participants.firstOrNull { it.id == studentId }
            ?: return AnswerAcceptance(false, reason = "Unknown participant")
        val normalized = answer.copy(itemId = targetItem, studentId = participant.id)
        val index = list.indexOfFirst { it.studentId == participant.id }
        if (index >= 0) {
            list[index] = normalized
        } else {
            list += normalized
        }
        return AnswerAcceptance(true)
    }

    private fun handleLanJoin(sessionId: String, nickname: String): LanJoinAck = lock.withLock {
        val state = sessions[sessionId] ?: return LanJoinAck(false, reason = "Session not found")
        val student = state.registerParticipant(nickname)
        state.publish()
        LanJoinAck(true, studentId = student.id, displayName = student.displayName)
    }

    private fun handleLanAnswer(sessionId: String, payload: AnswerPayload): LanAnswerAck = lock.withLock {
        val state = sessions[sessionId]
            ?: return LanAnswerAck(false, reason = "Session not found")
        val result = state.recordAnswer(payload)
        if (result.accepted) {
            state.publish()
        }
        val reason = when {
            result.accepted -> null
            result.reason != null -> result.reason
            else -> "No active question"
        }
        LanAnswerAck(result.accepted, reason = reason)
    }

    private fun shutdownLanSessionsLocked() {
        if (sessions.isEmpty()) return
        sessions.values.forEach { state ->
            runCatching { state.lanSession?.stop() }
        }
        sessions.clear()
    }

    companion object {
        private const val TAG = "LiveSessionAgent"
    }

    private data class AnswerAcceptance(val accepted: Boolean, val reason: String? = null)
}
