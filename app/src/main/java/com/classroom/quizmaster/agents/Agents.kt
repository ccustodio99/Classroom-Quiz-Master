package com.classroom.quizmaster.agents

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Badge
import com.classroom.quizmaster.domain.model.ClassReport
import com.classroom.quizmaster.domain.model.InteractiveActivity
import com.classroom.quizmaster.domain.model.Item
import com.classroom.quizmaster.domain.model.Module
import com.classroom.quizmaster.domain.model.Scorecard
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.model.StudentReport
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

interface ModuleBuilderAgent {
    suspend fun createOrUpdate(module: Module): Result<Unit>
    fun validate(module: Module): List<Violation>
}

@Serializable
data class Violation(val field: String, val message: String)

interface AssessmentAgent {
    suspend fun start(assessmentId: String, student: Student): String
    suspend fun submit(attemptId: String, answers: List<AnswerPayload>): Scorecard
}

@Serializable
data class AnswerPayload(
    val itemId: String,
    val answer: String,
    val studentId: String? = null
)

interface LessonAgent {
    suspend fun start(lessonId: String): String
    fun next(sessionId: String): LessonStep
    fun recordCheck(sessionId: String, answer: String): Boolean
}

data class LessonStep(
    val slideTitle: String?,
    val slideContent: String?,
    val miniCheckPrompt: String?,
    val activity: InteractiveActivity?,
    val finished: Boolean
)

interface LiveSessionAgent {
    fun createSession(moduleId: String): String
    fun join(sessionId: String, nickname: String): JoinResult
    fun submit(sessionId: String, answer: AnswerPayload): Ack
    fun snapshot(sessionId: String): LiveSnapshot
    fun observe(sessionId: String): Flow<LiveSnapshot>
    fun setActiveItem(
        sessionId: String,
        itemId: String?,
        prompt: String? = null,
        objective: String? = null
    ): Boolean
}

@Serializable
data class JoinResult(val student: Student, val sessionId: String)

@Serializable
data class Ack(val accepted: Boolean)

@Serializable
data class LiveSnapshot(
    val moduleId: String,
    val participants: List<Student>,
    val answers: Map<String, List<AnswerPayload>>,
    val activeItemId: String?,
    val activePrompt: String?,
    val activeObjective: String?
)

interface AssignmentAgent {
    suspend fun assign(moduleId: String, dueEpochMs: Long): Assignment
    fun status(assignmentId: String): Flow<Assignment>
}

interface ScoringAnalyticsAgent {
    suspend fun buildClassReport(moduleId: String): ClassReport
    suspend fun buildStudentReport(moduleId: String, studentId: String): StudentReport
}

interface ReportExportAgent {
    suspend fun exportClassPdf(report: ClassReport): FileRef
    suspend fun exportStudentPdf(report: StudentReport): FileRef
    suspend fun exportCsv(rows: List<CsvRow>): FileRef
}

@Serializable
data class FileRef(val path: String)

@Serializable
data class CsvRow(val cells: List<String>)

interface ItemBankAgent {
    fun query(objectives: List<String>, limit: Int = 20): List<Item>
    fun upsert(items: List<Item>): Result<Unit>
}

interface GamificationAgent {
    fun onReportsAvailable(report: ClassReport)
    fun unlocksFor(studentId: String): List<Badge>
}

interface SyncAgent {
    suspend fun pushModule(moduleId: String): Result<Unit>
    suspend fun pullUpdates(): Result<Int>
}
