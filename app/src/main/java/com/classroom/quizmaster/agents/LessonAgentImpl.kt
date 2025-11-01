package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.Lesson
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class LessonAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : LessonAgent {
    private val sessions = mutableMapOf<String, LessonSession>()
    private val lessonCache = ConcurrentHashMap<String, Lesson>()
    private val lock = ReentrantLock()

    init {
        scope.launch {
            moduleRepository.observeModules().collect { modules ->
                val observed = mutableSetOf<String>()
                modules.forEach { module ->
                    val lesson = module.lesson
                    lessonCache[lesson.id] = lesson
                    observed += lesson.id
                }
                val stale = lessonCache.keys - observed
                stale.forEach { lessonCache.remove(it) }
                if (stale.isNotEmpty()) {
                    lock.withLock {
                        val staleSessionIds = sessions
                            .filterValues { it.lesson.id in stale }
                            .keys
                        staleSessionIds.forEach { sessions.remove(it) }
                    }
                }
            }
        }
    }

    override suspend fun start(lessonId: String): String {
        val lesson = loadLesson(lessonId)
        val sessionId = UUID.randomUUID().toString()
        lock.withLock { sessions[sessionId] = LessonSession(lesson, 0) }
        return sessionId
    }

    override fun next(sessionId: String): LessonStep = lock.withLock {
        val session = sessions[sessionId] ?: error("Session not found")
        val lesson = session.lesson
        val slideCount = lesson.slides.size
        val activityCount = lesson.interactiveActivities.size
        val totalSteps = slideCount + activityCount
        if (session.index >= totalSteps) {
            sessions.remove(sessionId)
            return@withLock LessonStep(
                slideTitle = null,
                slideContent = null,
                miniCheckPrompt = null,
                activity = null,
                finished = true
            )
        }

        val currentIndex = session.index
        val nextIndex = currentIndex + 1
        sessions[sessionId] = session.copy(index = nextIndex)
        val finished = nextIndex >= totalSteps

        if (currentIndex < slideCount) {
            val slide = lesson.slides[currentIndex]
            LessonStep(
                slideTitle = slide.title,
                slideContent = slide.content,
                miniCheckPrompt = slide.miniCheck?.prompt,
                activity = null,
                finished = finished
            )
        } else {
            val activityIndex = currentIndex - slideCount
            val activity = lesson.interactiveActivities.getOrNull(activityIndex)
                ?: return@withLock LessonStep(
                    slideTitle = null,
                    slideContent = null,
                    miniCheckPrompt = null,
                    activity = null,
                    finished = true
                )
            LessonStep(
                slideTitle = null,
                slideContent = null,
                miniCheckPrompt = null,
                activity = activity,
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

    private suspend fun loadLesson(lessonId: String): Lesson {
        lessonCache[lessonId]?.let { return it }
        val lesson = moduleRepository.findLesson(lessonId)
            ?: error("Lesson not found")
        lessonCache[lessonId] = lesson
        return lesson
    }

    private data class LessonSession(val lesson: Lesson, val index: Int)
}
