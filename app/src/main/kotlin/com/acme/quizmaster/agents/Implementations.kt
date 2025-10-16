package com.acme.quizmaster.agents

import com.acme.quizmaster.data.AssignmentRepository
import com.acme.quizmaster.data.AttemptRepository
import com.acme.quizmaster.data.ItemBankRepository
import com.acme.quizmaster.data.ModuleRepository
import com.acme.quizmaster.data.SessionRepository
import com.acme.quizmaster.data.acceptSubmission
import com.acme.quizmaster.domain.*
import com.acme.quizmaster.util.scoreResponses
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ItemBankAgentImpl(
    private val repository: ItemBankRepository
) : ItemBankAgent {
    override fun addItems(items: List<Item>) {
        repository.addItems(items)
    }

    override fun itemsByObjective(objective: String): List<Item> = repository.items(objective)

    override fun allObjectives(): Set<String> = repository.objectives()
}

class ModuleBuilderAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val itemBankAgent: ItemBankAgent
) : ModuleBuilderAgent {

    override fun createOrUpdate(module: Module): Result<Unit> {
        val violations = validate(module)
        return if (violations.isEmpty()) {
            moduleRepository.upsert(module)
            itemBankAgent.addItems((module.preTest.items + module.postTest.items).distinctBy { it.id })
            Result.success(Unit)
        } else {
            val message = violations.joinToString(", ") { "${it.field}: ${it.message}" }
            Result.failure(IllegalArgumentException("Module validation failed: $message"))
        }
    }

    override fun validate(module: Module): List<Violation> {
        val violations = mutableListOf<Violation>()
        if (module.topic.isBlank()) violations += Violation("topic", "Topic is required")
        if (module.objectives.isEmpty()) violations += Violation("objectives", "At least one objective required")
        if (module.preTest.items.isEmpty()) violations += Violation("preTest", "Pre-Test requires at least one item")
        if (module.postTest.items.isEmpty()) violations += Violation("postTest", "Post-Test requires at least one item")
        val preObjectives = module.preTest.items.map { it.objective }.toSet()
        val postObjectives = module.postTest.items.map { it.objective }.toSet()
        if (preObjectives != postObjectives) {
            violations += Violation("assessments", "Pre/Post test objectives must match")
        }
        val preCounts = module.preTest.items.groupingBy { it.objective }.eachCount()
        val postCounts = module.postTest.items.groupingBy { it.objective }.eachCount()
        module.objectives.forEach { objective ->
            if (!preObjectives.contains(objective)) {
                violations += Violation("objectives", "Objective $objective missing from assessments")
            }
            if ((preCounts[objective] ?: 0) != (postCounts[objective] ?: 0)) {
                violations += Violation(
                    "assessments",
                    "Parallel forms must align for $objective"
                )
            }
            if (itemBankAgent.itemsByObjective(objective).isEmpty() &&
                module.preTest.items.none { it.objective == objective }
            ) {
                violations += Violation("itemBank", "Item bank missing items for objective $objective")
            }
        }
        if (module.lesson.slides.isEmpty()) {
            violations += Violation("lesson", "Lesson requires at least one slide")
        }
        val slideObjectives = module.lesson.slides.map { it.objective }.toSet()
        module.objectives.forEach { objective ->
            if (!slideObjectives.contains(objective)) {
                violations += Violation("lesson", "Slides missing coverage for $objective")
            }
        }
        module.lesson.slides.forEachIndexed { index, slide ->
            if (slide.objective !in module.objectives) {
                violations += Violation("lesson", "Slide ${index + 1} targets unknown objective ${slide.objective}")
            }
        }
        return violations
    }

    override fun listModules(): List<Module> = moduleRepository.list()

    override fun findModule(id: String): Module? = moduleRepository.find(id)
}

class AssessmentAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val attemptRepository: AttemptRepository
) : AssessmentAgent {

    override fun start(assessmentId: String, student: Student, moduleId: String): Attempt {
        val module = moduleRepository.find(moduleId)
            ?: error("Module $moduleId not found")
        val assessment = module.assessmentById(assessmentId)
            ?: error("Assessment $assessmentId not found in module")
        val attempt = Attempt(
            assessmentId = assessment.id,
            moduleId = module.id,
            studentId = student.id,
            studentNickname = student.nickname,
            maxScore = assessment.items.size.toDouble()
        )
        attemptRepository.save(attempt)
        return attempt
    }

    override fun submit(attemptId: String, answers: List<AnswerPayload>): Scorecard {
        val attempt = attemptRepository.find(attemptId) ?: error("Attempt $attemptId not found")
        val module = moduleRepository.find(attempt.moduleId) ?: error("Module ${attempt.moduleId} missing")
        val assessment = module.assessmentById(attempt.assessmentId)
            ?: error("Assessment ${attempt.assessmentId} missing in module")
        val (score, breakdown) = scoreResponses(assessment, answers)
        val submitted = attempt.copy(
            responses = answers,
            score = score,
            maxScore = assessment.items.size.toDouble(),
            status = AttemptStatus.SUBMITTED,
            submittedAt = Instant.now()
        )
        attemptRepository.save(submitted)
        return Scorecard(
            attemptId = submitted.id,
            studentId = submitted.studentId,
            studentNickname = submitted.studentNickname,
            moduleId = submitted.moduleId,
            assessmentId = submitted.assessmentId,
            score = submitted.score,
            maxScore = submitted.maxScore,
            objectiveBreakdown = breakdown,
            submittedAt = submitted.submittedAt ?: Instant.now()
        )
    }

    override fun findAttempt(attemptId: String): Attempt? = attemptRepository.find(attemptId)
}

class LiveSessionAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val sessionRepository: SessionRepository,
    private val attemptRepository: AttemptRepository
) : LiveSessionAgent {
    override fun createSession(moduleId: String, settings: SessionSettings): String {
        moduleRepository.find(moduleId) ?: error("Module $moduleId not found")
        val session = sessionRepository.create(moduleId, settings)
        return session.sessionId
    }

    override fun join(sessionId: String, student: Student): Boolean {
        val session = sessionRepository.find(sessionId) ?: return false
        session.participants.computeIfAbsent(student.id) {
            ParticipantProgress(student.id, student.nickname, 0, totalItems(session.moduleId), 0.0)
        }
        return true
    }

    override fun submit(sessionId: String, studentId: String, answer: AnswerPayload): Boolean {
        val session = sessionRepository.find(sessionId) ?: return false
        val participant = session.participants[studentId] ?: return false
        val module = moduleRepository.find(session.moduleId) ?: return false
        val assessment = module.preTest
        if (assessment.items.none { it.id == answer.itemId }) return false
        val attempt = attemptRepository.attemptsForStudent(module.id, studentId)
            .firstOrNull { it.assessmentId == assessment.id }
            ?: Attempt(
                assessmentId = assessment.id,
                moduleId = module.id,
                studentId = studentId,
                studentNickname = participant.nickname,
                maxScore = assessment.items.size.toDouble()
            )
        val existingResponses = attempt.responses.filterNot { it.itemId == answer.itemId } + answer
        val distinctResponses = existingResponses.distinctBy { it.itemId }
        val (score, _) = scoreResponses(assessment, distinctResponses)
        val answeredCount = distinctResponses.size
        val status = if (answeredCount >= assessment.items.size) AttemptStatus.SUBMITTED else AttemptStatus.IN_PROGRESS
        val submittedAt = if (status == AttemptStatus.SUBMITTED) Instant.now() else attempt.submittedAt
        attemptRepository.save(
            attempt.copy(
                responses = distinctResponses,
                score = score,
                maxScore = assessment.items.size.toDouble(),
                status = status,
                submittedAt = submittedAt
            )
        )
        val updated = participant.copy(
            answered = answeredCount,
            total = assessment.items.size,
            score = score
        )
        session.participants[studentId] = updated
        return true
    }

    override fun snapshot(sessionId: String): LiveSnapshot? {
        val session = sessionRepository.find(sessionId) ?: return null
        return LiveSnapshot(
            sessionId = session.sessionId,
            moduleId = session.moduleId,
            participants = session.participants.values.sortedByDescending { it.score }
        )
    }

    private fun totalItems(moduleId: String): Int {
        val module = moduleRepository.find(moduleId)
            ?: error("Module $moduleId not found")
        return module.preTest.items.size
    }
}

class LessonAgentImpl(
    private val moduleRepository: ModuleRepository
) : LessonAgent {
    override fun slidesFor(moduleId: String): List<LessonSlide> =
        moduleRepository.find(moduleId)?.lesson?.slides ?: emptyList()
}

class AssignmentAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val assignmentRepository: AssignmentRepository
) : AssignmentAgent {
    override fun schedule(
        moduleId: String,
        startDate: Instant,
        dueDate: Instant,
        settings: AssignmentSettings
    ): Assignment {
        moduleRepository.find(moduleId) ?: error("Module $moduleId not found")
        require(dueDate.isAfter(startDate)) { "Assignment due date must be after the start date" }
        val assignment = Assignment(
            moduleId = moduleId,
            startDate = startDate,
            dueDate = dueDate,
            settings = settings
        )
        assignmentRepository.save(assignment)
        return assignment
    }

    override fun submit(assignmentId: String, attempt: Attempt): Boolean {
        val assignment = assignmentRepository.find(assignmentId) ?: return false
        if (attempt.moduleId != assignment.moduleId) return false
        val accepted = assignment.acceptSubmission(attempt)
        if (accepted) assignmentRepository.save(assignment)
        return accepted
    }

    override fun listAssignments(): List<Assignment> = assignmentRepository.list()

    override fun getAssignment(id: String): Assignment? = assignmentRepository.find(id)
}

class ScoringAnalyticsAgentImpl(
    private val moduleRepository: ModuleRepository,
    private val attemptRepository: AttemptRepository
) : ScoringAnalyticsAgent {

    override fun buildReports(moduleId: String): ClassReport {
        val module = moduleRepository.find(moduleId) ?: error("Module $moduleId not found")
        val attempts = attemptRepository.attemptsForModule(moduleId)
            .filter { it.status == AttemptStatus.SUBMITTED }
        val scorecards = attempts.map { it.toScorecard(module) }
        val preScores = scorecards.filter { it.assessmentId == module.preTest.id }
        val postScores = scorecards.filter { it.assessmentId == module.postTest.id }
        val preAverage = preScores.map { it.score / it.maxScore }.averageOrZero()
        val postAverage = postScores.map { it.score / it.maxScore }.averageOrZero()
        val objectives = module.objectives.map { objective ->
            val pre = preScores.objectiveAverage(objective)
            val post = postScores.objectiveAverage(objective)
            ObjectiveGain(objective, pre, post)
        }
        return ClassReport(
            moduleId = module.id,
            topic = module.topic,
            preAssessmentId = module.preTest.id,
            postAssessmentId = module.postTest.id,
            preAverage = preAverage,
            postAverage = postAverage,
            learningGain = postAverage - preAverage,
            objectives = objectives,
            attempts = scorecards
        )
    }

    override fun studentReport(moduleId: String, studentId: String): StudentReport? {
        val module = moduleRepository.find(moduleId) ?: return null
        val attempts = attemptRepository.attemptsForStudent(moduleId, studentId)
            .filter { it.status == AttemptStatus.SUBMITTED }
        if (attempts.isEmpty()) return null
        val scorecards = attempts.map { it.toScorecard(module) }
        val pre = scorecards.find { it.assessmentId == module.preTest.id }
        val post = scorecards.find { it.assessmentId == module.postTest.id }
        val nickname = scorecards.firstOrNull()?.studentNickname ?: studentId
        return StudentReport(
            moduleId = module.id,
            studentId = studentId,
            nickname = nickname,
            preScore = pre?.score ?: 0.0,
            postScore = post?.score ?: 0.0,
            learningGain = (post?.score ?: 0.0) - (pre?.score ?: 0.0),
            objectiveGains = module.objectives.map { objective ->
                ObjectiveGain(
                    objective = objective,
                    pre = pre?.objectiveBreakdown?.get(objective)?.let { it.correct.toDouble() / it.total } ?: 0.0,
                    post = post?.objectiveBreakdown?.get(objective)?.let { it.correct.toDouble() / it.total } ?: 0.0
                )
            }
        )
    }

    private fun Attempt.toScorecard(module: Module): Scorecard {
        val assessment = module.assessmentById(assessmentId)
            ?: error("Assessment $assessmentId not part of module ${module.id}")
        val (score, breakdown) = scoreResponses(assessment, responses)
        return Scorecard(
            attemptId = id,
            studentId = studentId,
            studentNickname = studentNickname,
            moduleId = module.id,
            assessmentId = assessmentId,
            score = score,
            maxScore = assessment.items.size.toDouble(),
            objectiveBreakdown = breakdown,
            submittedAt = submittedAt ?: Instant.now()
        )
    }

    private fun Iterable<Double>.averageOrZero(): Double = if (any()) average() else 0.0

    private fun List<Scorecard>.objectiveAverage(objective: String): Double {
        val entries = mapNotNull { it.objectiveBreakdown[objective] }
        if (entries.isEmpty()) return 0.0
        val correct = entries.sumOf { it.correct }
        val total = entries.sumOf { it.total }
        return if (total == 0) 0.0 else correct.toDouble() / total
    }
}

class ReportExportAgentImpl : ReportExportAgent {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withLocale(Locale.US)
        .withZone(ZoneId.systemDefault())

    override fun exportClassReport(report: ClassReport, path: String): String {
        val file = prepareFile(path)
        val content = buildString {
            appendLine("Class Report for ${report.topic}")
            appendLine("Module ID: ${report.moduleId}")
            appendLine("Pre-Test Average: ${(report.preAverage * 100).formatPercent()}%")
            appendLine("Post-Test Average: ${(report.postAverage * 100).formatPercent()}%")
            appendLine("Learning Gain (Pag-angat ng Marka): ${(report.learningGain * 100).formatPercent()}%")
            appendLine("Objective Gains:")
            report.objectives.forEach { gain ->
                appendLine(
                    " - ${gain.objective}: ${(gain.pre * 100).formatPercent()}% → ${(gain.post * 100).formatPercent()}%"
                )
            }
            appendLine("Attempts:")
            report.attempts.forEach { attempt ->
                appendLine(
                    " • ${attempt.studentNickname} (${attempt.studentId}) ${attempt.score}/${attempt.maxScore} on ${formatter.format(attempt.submittedAt)}"
                )
            }
        }
        file.writeText(content)
        return file.absolutePath
    }

    override fun exportStudentReport(report: StudentReport, path: String): String {
        val file = prepareFile(path)
        val content = buildString {
            appendLine("Student Report for ${report.nickname}")
            appendLine("Module: ${report.moduleId}")
            appendLine("Pre-Test: ${report.preScore}")
            appendLine("Post-Test: ${report.postScore}")
            appendLine(
                "Learning Gain (Pag-angat ng Marka): ${String.format(Locale.US, "%.1f", report.learningGain)}"
            )
            appendLine("Objective Gains:")
            report.objectiveGains.forEach { gain ->
                appendLine(" - ${gain.objective}: ${(gain.pre * 100).formatPercent()}% → ${(gain.post * 100).formatPercent()}%")
            }
        }
        file.writeText(content)
        return file.absolutePath
    }

    override fun exportClassCsv(report: ClassReport, path: String): String {
        val file = prepareFile(path)
        val content = buildString {
            appendLine("metric,value")
            appendLine("pre_average,${(report.preAverage * 100).formatPercent()}")
            appendLine("post_average,${(report.postAverage * 100).formatPercent()}")
            appendLine("learning_gain,${(report.learningGain * 100).formatPercent()}")
            appendLine()
            appendLine("objective,pre_percent,post_percent,gain_percent")
            report.objectives.forEach { gain ->
                val gainPercent = ((gain.post - gain.pre) * 100).formatPercent()
                appendLine(
                    "${gain.objective},${(gain.pre * 100).formatPercent()},${(gain.post * 100).formatPercent()},$gainPercent"
                )
            }
        }
        file.writeText(content)
        return file.absolutePath
    }

    override fun exportStudentCsv(report: StudentReport, path: String): String {
        val file = prepareFile(path)
        val content = buildString {
            appendLine("metric,value")
            appendLine("pre_score,${report.preScore}")
            appendLine("post_score,${report.postScore}")
            appendLine("learning_gain,${report.learningGain}")
            appendLine()
            appendLine("objective,pre_percent,post_percent,gain_percent")
            report.objectiveGains.forEach { gain ->
                val gainPercent = ((gain.post - gain.pre) * 100).formatPercent()
                appendLine(
                    "${gain.objective},${(gain.pre * 100).formatPercent()},${(gain.post * 100).formatPercent()},$gainPercent"
                )
            }
        }
        file.writeText(content)
        return file.absolutePath
    }

    private fun prepareFile(path: String): File {
        val file = File(path)
        file.parentFile?.mkdirs()
        return file
    }

    private fun Double.formatPercent(): String = String.format(Locale.US, "%.1f", this)
}

class GamificationAgentImpl(
    private val analyticsAgent: ScoringAnalyticsAgent
) : GamificationAgent {
    override fun topImprovers(moduleId: String): List<GamificationBadge> {
        val report = analyticsAgent.buildReports(moduleId)
        val grouped = report.attempts.groupBy { it.studentId }
        val gains = grouped.mapNotNull { (studentId, attempts) ->
            val pre = attempts.find { it.assessmentId == report.preAssessmentId }
            val post = attempts.find { it.assessmentId == report.postAssessmentId }
            if (pre != null && post != null) {
                Triple(
                    (post.score / post.maxScore) - (pre.score / pre.maxScore),
                    studentId,
                    post.studentNickname.ifBlank { pre.studentNickname }
                )
            } else null
        }.sortedByDescending { it.first }
        return gains.take(3).map { (gain, studentId, nickname) ->
            GamificationBadge(
                studentId = studentId,
                nickname = nickname,
                badge = "Top Improver",
                description = "Improved ${(gain * 100).formatPercent()}%"
            )
        }
    }

    override fun starOfTheDay(moduleId: String): GamificationBadge? {
        val report = analyticsAgent.buildReports(moduleId)
        val topScore = report.attempts
            .filter { it.assessmentId == report.postAssessmentId }
            .maxByOrNull { it.score / it.maxScore }
            ?: return null
        return GamificationBadge(
            studentId = topScore.studentId,
            nickname = topScore.studentNickname,
            badge = "Star of the Day",
            description = "Top post-test score ${(topScore.score / topScore.maxScore * 100).formatPercent()}%"
        )
    }

    private fun Double.formatPercent(): String = String.format(Locale.US, "%.1f", this)
}

private fun Module.assessmentById(id: String): Assessment? = when {
    preTest.id == id -> preTest
    postTest.id == id -> postTest
    else -> null
}
