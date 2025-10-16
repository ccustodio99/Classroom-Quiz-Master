package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.Lesson
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LessonAgentImpl(
    private val moduleRepository: ModuleRepository
) : LessonAgent {
    private val sessions = mutableMapOf<String, LessonSession>()
    private val mutex = Mutex()

    override fun start(lessonId: String): String {
        val lesson = runBlocking { findLesson(lessonId) }
        val sessionId = UUID.randomUUID().toString()
        runBlocking { mutex.withLock { sessions[sessionId] = LessonSession(lesson, 0) } }
        return sessionId
    }

    override fun next(sessionId: String): LessonStep {
        val session = runBlocking { mutex.withLock { sessions[sessionId] } } ?: error("Session not found")
        val slide = session.lesson.slides.getOrNull(session.index)
        return if (slide == null) {
            LessonStep("Tapusin", "Lahat ng slide ay natapos na", null, finished = true)
        } else {
            runBlocking { mutex.withLock { sessions[sessionId] = session.copy(index = session.index + 1) } }
            LessonStep(
                slideTitle = slide.title,
                slideContent = slide.content,
                miniCheckPrompt = slide.miniCheck?.prompt,
                finished = session.index + 1 >= session.lesson.slides.size
            )
        }
    }

    override fun recordCheck(sessionId: String, answer: String): Boolean {
        val session = runBlocking { mutex.withLock { sessions[sessionId] } } ?: return false
        val slide = session.lesson.slides.getOrNull(session.index - 1)
        return slide?.miniCheck?.let { it.correctAnswer.equals(answer.trim(), ignoreCase = true) } ?: false
    }

    private suspend fun findLesson(lessonId: String): Lesson {
        val modules = moduleRepository.observeModules().first()
        return modules.firstOrNull { it.lesson.id == lessonId }?.lesson ?: error("Lesson not found")
    }

    private data class LessonSession(val lesson: Lesson, val index: Int)
}
