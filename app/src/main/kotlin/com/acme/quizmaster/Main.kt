package com.classroom.quizmaster

import com.acme.quizmaster.agents.AssessmentAgentImpl
import com.acme.quizmaster.agents.AssignmentAgentImpl
import com.acme.quizmaster.agents.GamificationAgentImpl
import com.acme.quizmaster.agents.ItemBankAgentImpl
import com.acme.quizmaster.agents.LiveSessionAgentImpl
import com.acme.quizmaster.agents.LessonAgentImpl
import com.acme.quizmaster.agents.ModuleBuilderAgentImpl
import com.acme.quizmaster.agents.ReportExportAgentImpl
import com.acme.quizmaster.agents.ScoringAnalyticsAgentImpl
import com.acme.quizmaster.data.AssignmentRepository
import com.acme.quizmaster.data.AttemptRepository
import com.acme.quizmaster.data.ItemBankRepository
import com.acme.quizmaster.data.ModuleRepository
import com.acme.quizmaster.data.SessionRepository
import com.acme.quizmaster.domain.AnswerPayload
import com.acme.quizmaster.domain.AssignmentSettings
import com.acme.quizmaster.domain.SessionPace
import com.acme.quizmaster.domain.SessionSettings
import com.acme.quizmaster.domain.Student
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit

fun main() {
    val moduleRepository = ModuleRepository()
    val attemptRepository = AttemptRepository()
    val assignmentRepository = AssignmentRepository()
    val sessionRepository = SessionRepository()
    val itemBankRepository = ItemBankRepository()

    val itemBankAgent = ItemBankAgentImpl(itemBankRepository)
    val moduleBuilder = ModuleBuilderAgentImpl(moduleRepository, itemBankAgent)
    val assessmentAgent = AssessmentAgentImpl(moduleRepository, attemptRepository)
    val liveSessionAgent = LiveSessionAgentImpl(moduleRepository, sessionRepository, attemptRepository)
    val lessonAgent = LessonAgentImpl(moduleRepository)
    val assignmentAgent = AssignmentAgentImpl(moduleRepository, assignmentRepository)
    val analyticsAgent = ScoringAnalyticsAgentImpl(moduleRepository, attemptRepository)
    val reportExportAgent = ReportExportAgentImpl()
    val gamificationAgent = GamificationAgentImpl(analyticsAgent)

    val module = buildSampleModule(itemBankAgent)
    moduleBuilder.createOrUpdate(module).onFailure { error ->
        System.err.println("Module creation failed: ${error.message}")
        return
    }

    println("📘 Created module: ${module.topic} covering ${module.objectives.joinToString()}")
    println("📚 Lesson slides available: ${lessonAgent.slidesFor(module.id).size}")

    val students = listOf(
        Student(nickname = "Ana"),
        Student(nickname = "Ben")
    )

    val sessionId = liveSessionAgent.createSession(
        moduleId = module.id,
        settings = SessionSettings(leaderboardEnabled = true, pace = SessionPace.TEACHER_LED)
    )
    println("🏁 Live session created: $sessionId")

    students.forEach { student ->
        liveSessionAgent.join(sessionId, student)
        println(" • ${student.nickname} joined the session")
    }

    module.preTest.items.forEachIndexed { index, item ->
        students.forEach { student ->
            val response = if (student.nickname == "Ben" && index == 0) {
                item.options.firstOrNull()?.id ?: ""
            } else {
                item.answer
            }
            liveSessionAgent.submit(sessionId, student.id, AnswerPayload(item.id, response))
        }
    }

    liveSessionAgent.snapshot(sessionId)?.participants?.forEach { participant ->
        println("   - ${participant.nickname} answered ${participant.answered}/${participant.total} with score ${participant.score}")
    }

    val preAttempts = students.associateWith { student ->
        val attempt = assessmentAgent.start(module.preTest.id, student, module.id)
        val answers = module.preTest.items.mapIndexed { index, item ->
            val response = if (student.nickname == "Ben" && index == 0) {
                item.options.firstOrNull()?.id ?: ""
            } else {
                item.answer
            }
            AnswerPayload(item.id, response)
        }
        assessmentAgent.submit(attempt.id, answers)
    }

    val postAttempts = students.associateWith { student ->
        val attempt = assessmentAgent.start(module.postTest.id, student, module.id)
        val answers = module.postTest.items.map { item ->
            AnswerPayload(item.id, item.answer)
        }
        assessmentAgent.submit(attempt.id, answers)
    }

    val assignment = assignmentAgent.schedule(
        moduleId = module.id,
        startDate = Instant.now(),
        dueDate = Instant.now().plus(7, ChronoUnit.DAYS),
        settings = AssignmentSettings(allowLateSubmissions = true, maxAttempts = 2)
    )
    println("📝 Assignment scheduled with due date ${assignment.dueDate}")

    postAttempts.values.forEach { scorecard ->
        assessmentAgent.findAttempt(scorecard.attemptId)?.let { attempt ->
            assignmentAgent.submit(assignment.id, attempt)
        }
    }

    val classReport = analyticsAgent.buildReports(module.id)
    println("📊 Class pre-average ${(classReport.preAverage * 100).formatPercent()}%")
    println("📊 Class post-average ${(classReport.postAverage * 100).formatPercent()}%")

    val reportsDir = File("build/reports").apply { mkdirs() }
    val classTxt = reportExportAgent.exportClassReport(classReport, File(reportsDir, "class-report.txt").absolutePath)
    val classCsv = reportExportAgent.exportClassCsv(classReport, File(reportsDir, "class-report.csv").absolutePath)
    println("📄 Class report exported to $classTxt")
    println("📄 Class CSV exported to $classCsv")

    preAttempts.keys.forEach { student ->
        val studentReport = analyticsAgent.studentReport(module.id, student.id)
        if (studentReport != null) {
            val studentTxt = reportExportAgent.exportStudentReport(
                studentReport,
                File(reportsDir, "student-${student.nickname.lowercase()}-report.txt").absolutePath
            )
            val studentCsv = reportExportAgent.exportStudentCsv(
                studentReport,
                File(reportsDir, "student-${student.nickname.lowercase()}-report.csv").absolutePath
            )
            println("   • Exported reports for ${student.nickname}: $studentTxt, $studentCsv")
        }
    }

    val topImprovers = gamificationAgent.topImprovers(module.id)
    val star = gamificationAgent.starOfTheDay(module.id)
    println("🏅 Top Improvers: ${topImprovers.joinToString { "${it.nickname} (${it.description})" }}")
    println("🌟 Star of the Day: ${star?.nickname ?: "N/A"}")
}

private fun Double.formatPercent(): String = String.format(java.util.Locale.US, "%.1f", this)
