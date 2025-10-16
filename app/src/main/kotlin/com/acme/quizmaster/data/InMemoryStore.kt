package com.acme.quizmaster.data

import com.acme.quizmaster.domain.*
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class ModuleRepository {
    private val modules = ConcurrentHashMap<String, Module>()

    fun upsert(module: Module) {
        modules[module.id] = module
    }

    fun list(): List<Module> = modules.values.sortedBy { it.topic }

    fun find(id: String): Module? = modules[id]
}

class AttemptRepository {
    private val attempts = ConcurrentHashMap<String, Attempt>()

    fun save(attempt: Attempt) {
        attempts[attempt.id] = attempt
    }

    fun find(id: String): Attempt? = attempts[id]

    fun attemptsForModule(moduleId: String): List<Attempt> =
        attempts.values.filter { it.moduleId == moduleId }

    fun attemptsForStudent(moduleId: String, studentId: String): List<Attempt> =
        attempts.values.filter { it.moduleId == moduleId && it.studentId == studentId }
}

class AssignmentRepository {
    private val assignments = ConcurrentHashMap<String, Assignment>()

    fun save(assignment: Assignment) {
        assignments[assignment.id] = assignment
    }

    fun list(): List<Assignment> = assignments.values.sortedBy { it.dueDate }

    fun find(id: String): Assignment? = assignments[id]
}

class SessionRepository {
    data class SessionState(
        val sessionId: String,
        val moduleId: String,
        val settings: SessionSettings,
        val participants: MutableMap<String, ParticipantProgress>
    )

    private val sessions = ConcurrentHashMap<String, SessionState>()

    fun create(moduleId: String, settings: SessionSettings): SessionState {
        val state = SessionState(
            sessionId = java.util.UUID.randomUUID().toString(),
            moduleId = moduleId,
            settings = settings,
            participants = ConcurrentHashMap()
        )
        sessions[state.sessionId] = state
        return state
    }

    fun find(id: String): SessionState? = sessions[id]
}

class ItemBankRepository {
    private val itemsByObjective = ConcurrentHashMap<String, MutableList<Item>>()

    fun addItems(items: List<Item>) {
        items.forEach { item ->
            itemsByObjective.computeIfAbsent(item.objective) { mutableListOf() }.add(item)
        }
    }

    fun items(objective: String): List<Item> = itemsByObjective[objective]?.toList() ?: emptyList()

    fun objectives(): Set<String> = itemsByObjective.keys
}

fun Assignment.acceptSubmission(attempt: Attempt): Boolean {
    val already = submissions.count { it.studentId == attempt.studentId }
    if (already >= settings.maxAttempts) return false
    if (!settings.allowLateSubmissions && Instant.now().isAfter(dueDate)) return false
    submissions.removeAll { it.studentId == attempt.studentId && attempt.score >= it.score }
    submissions.add(attempt)
    return true
}
