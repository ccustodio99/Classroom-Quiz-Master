package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.Lesson
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class LessonAgentImpl(
    private val moduleRepository: ModuleRepository
) : LessonAgent {
    private val sessions = mutableMapOf<String, LessonSession>()
    private val lessonCache = ConcurrentHashMap<String, Lesson>()
    private val lock = ReentrantLock()

    override fun start(lessonId: String): String {
        val lesson = loadLesson(lessonId)
        val sessionId = UUID.randomUUID().toString()
        lock.withLock { sessions[sessionId] = LessonSession(lesson, 0) }
        return sessionId
    }

    override fun next(sessionId: String): LessonStep = lock.withLock {
        val session = sessions[sessionId] ?: error("Session not found")
        val slide = session.lesson.slides.getOrNull(session.index)
        if (slide == null) {
            sessions.remove(sessionId)
            LessonStep(
                slideTitle = "Tapos na",
                slideContent = "Lahat ng slide ay natapos na.",
                miniCheckPrompt = null,
                finished = true
            )
        } else {
            val nextIndex = session.index + 1
            sessions[sessionId] = session.copy(index = nextIndex)
            val finished = nextIndex >= session.lesson.slides.size
            LessonStep(
                slideTitle = slide.title,
                slideContent = slide.content,
                miniCheckPrompt = slide.miniCheck?.prompt,
                finished = finished
            )
        }
    }

    override fun recordCheck(sessionId: String, answer: String): Boolean = lock.withLock {
        val session = sessions[sessionId] ?: return false
        val lastShownIndex = (session.index - 1).coerceAtLeast(0)
        val slide = session.lesson.slides.getOrNull(lastShownIndex)
        slide?.miniCheck?.let { it.correctAnswer.equals(answer.trim(), ignoreCase = true) } ?: false
    }

    private fun loadLesson(lessonId: String): Lesson {
        lessonCache[lessonId]?.let { return it }
        val lesson = runBlocking(Dispatchers.IO) {
            moduleRepository.observeModules()
                .first()
                .firstOrNull { it.lesson.id == lessonId }
                ?.lesson
        } ?: error("Lesson not found")
        lessonCache[lessonId] = lesson
        return lesson
    }

    private data class LessonSession(val lesson: Lesson, val index: Int)
}
