package com.acme.quizmaster

import com.acme.quizmaster.agents.*
import com.acme.quizmaster.data.AssignmentRepository
import com.acme.quizmaster.data.AttemptRepository
import com.acme.quizmaster.data.ItemBankRepository
import com.acme.quizmaster.data.ModuleRepository
import com.acme.quizmaster.data.SessionRepository
import com.acme.quizmaster.domain.*
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
    val reportExporter = ReportExportAgentImpl()
    val gamificationAgent = GamificationAgentImpl(analyticsAgent)

    val module = buildSampleModule(itemBankAgent)
    moduleBuilder.createOrUpdate(module).getOrThrow()

    val validation = moduleBuilder.validate(module)
    println("Created module: ${module.topic} with objectives ${module.objectives}")
    println("Validation passed: ${validation.isEmpty()}")
    if (validation.isNotEmpty()) {
        validation.forEach { println(" - ${it.field}: ${it.message}") }
    }

    val students = listOf(Student(nickname = "Ava"), Student(nickname = "Ben"))

    // Pre-Test session
    val sessionId = liveSessionAgent.createSession(module.id, SessionSettings(true, SessionPace.TEACHER_LED))
    students.forEach { student ->
        liveSessionAgent.join(sessionId, student)
        val attempt = assessmentAgent.start(module.preTest.id, student, module.id)
        val answers = module.preTest.items.map { item ->
            val response = when (item.objective) {
                "LO1" -> item.answer // Ava and Ben know LO1
                else -> if (student.nickname == "Ava") item.answer else "wrong"
            }
            AnswerPayload(item.id, response)
        }
        liveSessionAgent.submit(sessionId, student.id, answers.first())
        val scorecard = assessmentAgent.submit(attempt.id, answers)
        println("${student.nickname} Pre-Test: ${scorecard.score}/${scorecard.maxScore}")
    }

    println("\nLesson Slides:"
    )
    lessonAgent.slidesFor(module.id).forEachIndexed { index, slide ->
        println("${index + 1}. ${slide.title} → ${slide.body}")
    }

    // Post-Test
    students.forEach { student ->
        val attempt = assessmentAgent.start(module.postTest.id, student, module.id)
        val answers = module.postTest.items.map { item ->
            val response = if (student.nickname == "Ava") item.answer else item.answer.takeIf { item.objective == "LO1" } ?: "wrong"
            AnswerPayload(item.id, response)
        }
        val scorecard = assessmentAgent.submit(attempt.id, answers)
        println("${student.nickname} Post-Test: ${scorecard.score}/${scorecard.maxScore}")
    }

    // Assignment scheduling example
    val assignment = assignmentAgent.schedule(
        module.id,
        Instant.now(),
        Instant.now().plus(3, ChronoUnit.DAYS),
        AssignmentSettings(allowLateSubmissions = false, maxAttempts = 2)
    )
    println("Assignment scheduled with due date ${assignment.dueDate}")

    val classReport = analyticsAgent.buildReports(module.id)
    val studentReport = analyticsAgent.studentReport(module.id, students.first().id)

    val classReportPath = reportExporter.exportClassReport(classReport, "build/reports/class-${module.id}.txt")
    val studentReportPath = studentReport?.let {
        reportExporter.exportStudentReport(it, "build/reports/student-${it.studentId}.txt")
    }

    println("Class report exported to $classReportPath")
    println("Student report exported to $studentReportPath")

    println("Top Improvers: ${gamificationAgent.topImprovers(module.id)}")
    println("Star of the Day: ${gamificationAgent.starOfTheDay(module.id)}")
}

