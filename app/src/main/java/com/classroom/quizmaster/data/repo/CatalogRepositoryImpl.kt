package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.domain.agent.GamificationAgent
import com.classroom.quizmaster.domain.model.ActivityTimeline
import com.classroom.quizmaster.domain.model.Badge
import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.CourseDifficulty
import com.classroom.quizmaster.domain.model.CourseSummary
import com.classroom.quizmaster.domain.model.HomeFeedItem
import com.classroom.quizmaster.domain.model.HomeFeedType
import com.classroom.quizmaster.domain.model.HomeTask
import com.classroom.quizmaster.domain.model.LearningUnit
import com.classroom.quizmaster.domain.model.LearningUnitType
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.PersonaType
import com.classroom.quizmaster.domain.model.Certificate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.math.max
import kotlin.math.roundToInt

class CatalogRepositoryImpl(
    private val moduleRepository: ModuleRepository,
    private val assignmentRepository: AssignmentRepository,
    private val gamificationAgent: GamificationAgent
) : CatalogRepository {

    override fun observeCourses(): Flow<List<CourseSummary>> =
        moduleRepository.observeModules().map { modules ->
            modules.map { module -> module.toCourseSummary() }
        }

    override fun observeHomeFeed(userId: String): Flow<List<HomeFeedItem>> =
        combine(
            moduleRepository.observeModules(),
            assignmentRepository.observeAll()
        ) { modules, assignments ->
            val now = System.currentTimeMillis()
            val assignmentsByModule = assignments.groupBy { it.moduleId }
            modules.flatMapIndexed { index, module ->
                val moduleAssignments: List<Assignment> = assignmentsByModule[module.id].orEmpty()
                val primaryAssignment = moduleAssignments.minByOrNull { it.dueEpochMs }
                val finalDue = moduleAssignments.maxByOrNull { it.dueEpochMs }?.dueEpochMs
                val preTask = HomeFeedItem(
                    id = "${module.id}-pre",
                    type = HomeFeedType.Task,
                    headline = module.preTest.items.takeIf { it.isNotEmpty() }
                        ?.let { "Pagsusulit Bago ang Aralin: ${module.topic}" }
                        ?: "Simulan ang Aralin",
                    detail = "Tapusin sa ${estimateMinutes(module.preTest.items.size, module.settings.timePerItemSeconds)} minuto upang sukatin ang baseline.",
                    accent = "Pagsusulit Bago ang Aralin",
                    priority = 10,
                    task = HomeTask(
                        id = "${module.id}-pre-task",
                        title = module.topic,
                        description = "Diagnose your starting mastery.",
                        actionLabel = "Take pre-test",
                        relatedModuleId = module.id,
                        relatedAssignmentId = null,
                        dueAt = primaryAssignment?.dueEpochMs ?: now + ((index + 1) * DAY_IN_MS / 2),
                        estimatedMinutes = estimateMinutes(module.preTest.items.size, module.settings.timePerItemSeconds),
                        objectiveTags = module.objectives
                    )
                )
                val lessonTask = HomeFeedItem(
                    id = "${module.id}-lesson",
                    type = HomeFeedType.Task,
                    headline = "Talakayan / Aralin: ${module.topic}",
                    detail = "Microlearning slides at worked examples na tatagal nang ${estimateLessonMinutes(module.lesson.slides.size)} minuto.",
                    accent = "Talakayan / Aralin",
                    priority = 20,
                    task = HomeTask(
                        id = "${module.id}-lesson-task",
                        title = module.lesson.slides.firstOrNull()?.title ?: module.topic,
                        description = module.lesson.slides.firstOrNull()?.content ?: "Review the guided lesson steps.",
                        actionLabel = "Continue lesson",
                        relatedModuleId = module.id,
                        relatedAssignmentId = null,
                        dueAt = primaryAssignment?.dueEpochMs ?: now + ((index + 1) * DAY_IN_MS),
                        estimatedMinutes = estimateLessonMinutes(module.lesson.slides.size),
                        objectiveTags = module.objectives
                    )
                )
                val postTask = HomeFeedItem(
                    id = "${module.id}-post",
                    type = HomeFeedType.Task,
                    headline = "Pagsusulit Pagkatapos ng Aralin: ${module.topic}",
                    detail = "Sukatin ang Pag-angat ng Marka at i-review ang mastery report.",
                    accent = "Pagsusulit Pagkatapos ng Aralin",
                    priority = 30,
                    task = HomeTask(
                        id = "${module.id}-post-task",
                        title = module.topic,
                        description = "Check mastery and unlock reports.",
                        actionLabel = "Take post-test",
                        relatedModuleId = module.id,
                        relatedAssignmentId = null,
                        dueAt = finalDue ?: now + ((index + 1) * DAY_IN_MS * 3 / 2),
                        estimatedMinutes = estimateMinutes(module.postTest.items.size, module.settings.timePerItemSeconds),
                        objectiveTags = module.objectives
                    )
                )
                val reminders = moduleAssignments.mapIndexed { assignmentIndex, assignment ->
                    HomeFeedItem(
                        id = "${module.id}-assignment-$assignmentIndex",
                        type = HomeFeedType.Reminder,
                        headline = "Due soon: ${module.topic}",
                        detail = "Assignment due ${formatDueLabel(assignment.dueEpochMs)}.",
                        accent = "Due ${formatRelativeDay(assignment.dueEpochMs, now)}",
                        priority = 15,
                        task = HomeTask(
                            id = assignment.id,
                            title = module.topic,
                            description = "Homework package for the module.",
                            actionLabel = "Review assignment",
                            relatedModuleId = module.id,
                            relatedAssignmentId = assignment.id,
                            dueAt = assignment.dueEpochMs,
                            estimatedMinutes = estimateLessonMinutes(module.lesson.slides.size),
                            objectiveTags = module.objectives
                        )
                    )
                }
                val streak = HomeFeedItem(
                    id = "${module.id}-streak",
                    type = HomeFeedType.Streak,
                    headline = "3 araw na sunod-sunod na pag-aaral!",
                    detail = "Panatilihin ang streak para sa mas mataas na engagement bonus.",
                    accent = "Streak",
                    priority = 40,
                    task = null,
                    timestamp = now - STREAK_WINDOW_MS
                )
                buildList {
                    add(preTask)
                    addAll(reminders)
                    add(lessonTask)
                    add(postTask)
                    add(streak)
                }
            }.sortedBy { it.priority }
        }

    override fun observeActivityTimeline(userId: String): Flow<ActivityTimeline> =
        moduleRepository.observeModules().map { modules ->
            val now = System.currentTimeMillis()
            val badges: List<Badge> = gamificationAgent.unlocksFor(userId)
            val certificates: List<Certificate> = modules.map { module ->
                Certificate(
                    id = "${module.id}-certificate",
                    title = "${module.topic} Certificate",
                    description = "Completed module with ${module.objectives.size} objectives covered.",
                    issuedAt = max(module.updatedAt, now - DAY_IN_MS),
                    fileRef = null
                )
            }
            ActivityTimeline(
                streakDays = STREAK_DAYS_DEFAULT,
                badges = badges,
                certificates = certificates,
                lastActiveAt = modules.maxOfOrNull { it.updatedAt } ?: now
            )
        }

    private fun Module.toCourseSummary(): CourseSummary {
        val created = if (createdAt > 0) createdAt else System.currentTimeMillis()
        val units = buildList {
            add(
                LearningUnit(
                    id = "${id}-pre",
                    courseId = id,
                    moduleId = id,
                    lessonId = null,
                    assessmentId = preTest.id,
                    type = LearningUnitType.PreTest,
                    title = "Pagsusulit Bago ang Aralin",
                    summary = "Quick baseline diagnostic for ${topic}.",
                    objectiveTags = objectives,
                    estimatedMinutes = estimateMinutes(preTest.items.size, settings.timePerItemSeconds),
                    recommendedFor = listOf(PersonaType.Learner, PersonaType.Instructor)
                )
            )
            add(
                LearningUnit(
                    id = "${id}-lesson",
                    courseId = id,
                    moduleId = id,
                    lessonId = lesson.id,
                    assessmentId = null,
                    type = LearningUnitType.Lesson,
                    title = "Talakayan / Aralin",
                    summary = "Guided lesson with revealable steps and mini checks.",
                    objectiveTags = objectives,
                    estimatedMinutes = estimateLessonMinutes(lesson.slides.size),
                    recommendedFor = listOf(PersonaType.Learner, PersonaType.Instructor),
                    unlocksAfter = "${id}-pre"
                )
            )
            add(
                LearningUnit(
                    id = "${id}-post",
                    courseId = id,
                    moduleId = id,
                    lessonId = null,
                    assessmentId = postTest.id,
                    type = LearningUnitType.PostTest,
                    title = "Pagsusulit Pagkatapos ng Aralin",
                    summary = "Measure learning gain and generate mastery insights.",
                    objectiveTags = objectives,
                    estimatedMinutes = estimateMinutes(postTest.items.size, settings.timePerItemSeconds),
                    recommendedFor = listOf(PersonaType.Learner, PersonaType.Instructor),
                    unlocksAfter = "${id}-lesson"
                )
            )
            if (settings.allowLeaderboard) {
                add(
                    LearningUnit(
                        id = "${id}-live",
                        courseId = id,
                        moduleId = id,
                        lessonId = lesson.id,
                        assessmentId = null,
                        type = LearningUnitType.Live,
                        title = "Live Session",
                        summary = "Real-time in-class activity with LAN priority and leaderboard.",
                        objectiveTags = objectives,
                        estimatedMinutes = LIVE_MINUTES_DEFAULT,
                        recommendedFor = listOf(PersonaType.Instructor),
                        unlocksAfter = "${id}-pre"
                    )
                )
            }
        }
        return CourseSummary(
            id = id,
            title = topic,
            description = "Modular lesson covering ${objectives.size} objectives for ${classroom.gradeLevel}.",
            category = classroom.subject,
            difficulty = CourseDifficulty.Intermediate,
            coverImageUrl = null,
            units = units,
            updatedAt = max(updatedAt, created),
            authorIds = listOfNotNull(classroom.ownerId)
        )
    }

    private fun estimateMinutes(itemCount: Int, timePerItemSec: Int): Int {
        val raw = (itemCount * max(timePerItemSec, 30)) / 60.0
        return max(3, raw.roundToInt())
    }

    private fun estimateLessonMinutes(slideCount: Int): Int {
        if (slideCount <= 0) return 3
        return max(3, (slideCount * LESSON_MIN_PER_SLIDE).roundToInt())
    }

    private fun formatDueLabel(epochMs: Long?): String {
        if (epochMs == null) return "soon"
        val remaining = epochMs - System.currentTimeMillis()
        return when {
            remaining <= 0L -> "today"
            remaining <= DAY_IN_MS -> "within 1 day"
            remaining <= 2 * DAY_IN_MS -> "in 2 days"
            else -> "in ${(remaining / DAY_IN_MS).coerceAtLeast(2)} days"
        }
    }

    private fun formatRelativeDay(epochMs: Long?, now: Long): String {
        if (epochMs == null) return "soon"
        val deltaDays = ((epochMs - now) / DAY_IN_MS).toInt()
        return when {
            deltaDays <= 0 -> "today"
            deltaDays == 1 -> "tomorrow"
            deltaDays in 2..6 -> "in $deltaDays days"
            else -> "next week"
        }
    }

    companion object {
        private const val DAY_IN_MS = 86_400_000L
        private const val STREAK_WINDOW_MS = 72_000_00L
        private const val STREAK_DAYS_DEFAULT = 3
        private const val LESSON_MIN_PER_SLIDE = 1.5
        private const val LIVE_MINUTES_DEFAULT = 10
    }
}
