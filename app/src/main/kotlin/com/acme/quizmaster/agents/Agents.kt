package com.acme.quizmaster.agents

import com.acme.quizmaster.domain.*
import java.time.Instant

interface ModuleBuilderAgent {
    fun createOrUpdate(module: Module): Result<Unit>
    fun validate(module: Module): List<String>
    fun listModules(): List<Module>
    fun findModule(id: String): Module?
}

interface LiveSessionAgent {
    fun createSession(moduleId: String, settings: SessionSettings): String
    fun join(sessionId: String, student: Student): Boolean
    fun submit(sessionId: String, studentId: String, answer: AnswerPayload): Boolean
    fun snapshot(sessionId: String): LiveSnapshot?
}

interface AssignmentAgent {
    fun schedule(moduleId: String, startDate: Instant, dueDate: Instant, settings: AssignmentSettings): Assignment
    fun submit(assignmentId: String, attempt: Attempt): Boolean
    fun listAssignments(): List<Assignment>
    fun getAssignment(id: String): Assignment?
}

interface AssessmentAgent {
    fun start(assessmentId: String, student: Student, moduleId: String): Attempt
    fun submit(attemptId: String, answers: List<AnswerPayload>): Scorecard
    fun findAttempt(attemptId: String): Attempt?
}

interface LessonAgent {
    fun slidesFor(moduleId: String): List<LessonSlide>
}

interface ScoringAnalyticsAgent {
    fun buildReports(moduleId: String): ClassReport
    fun studentReport(moduleId: String, studentId: String): StudentReport?
}

interface ReportExportAgent {
    fun exportClassReport(report: ClassReport, path: String): String
    fun exportStudentReport(report: StudentReport, path: String): String
}

interface ItemBankAgent {
    fun addItems(items: List<Item>)
    fun itemsByObjective(objective: String): List<Item>
    fun allObjectives(): Set<String>
}

interface GamificationAgent {
    fun topImprovers(moduleId: String): List<GamificationBadge>
    fun starOfTheDay(moduleId: String): GamificationBadge?
}
