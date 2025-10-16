package com.acme.quizmaster

import com.acme.quizmaster.agents.*
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuizMasterTest {
    private val moduleRepository = ModuleRepository()
    private val attemptRepository = AttemptRepository()
    private val assignmentRepository = AssignmentRepository()
    private val sessionRepository = SessionRepository()
    private val itemBankRepository = ItemBankRepository()

    private val itemBankAgent = ItemBankAgentImpl(itemBankRepository)
    private val moduleBuilder = ModuleBuilderAgentImpl(moduleRepository, itemBankAgent)
    private val assessmentAgent = AssessmentAgentImpl(moduleRepository, attemptRepository)
    private val liveSessionAgent = LiveSessionAgentImpl(moduleRepository, sessionRepository, attemptRepository)
    private val lessonAgent = LessonAgentImpl(moduleRepository)
    private val assignmentAgent = AssignmentAgentImpl(moduleRepository, assignmentRepository)
    private val analyticsAgent = ScoringAnalyticsAgentImpl(moduleRepository, attemptRepository)
    private val reportExporter = ReportExportAgentImpl()
    private val gamificationAgent = GamificationAgentImpl(analyticsAgent)

    private val module = buildSampleModule(itemBankAgent).also { moduleBuilder.createOrUpdate(it) }

    @Test
    fun `module validation ensures objectives and slides`() {
        val violations = moduleBuilder.validate(module)
        assertTrue(violations.isEmpty(), "Expected no validation issues: $violations")
    }

    @Test
    fun `assessment agent scores answers correctly`() {
        val student = Student(nickname = "Casey")
        val attempt = assessmentAgent.start(module.preTest.id, student, module.id)
        val answers = module.preTest.items.map { AnswerPayload(it.id, it.answer) }
        val scorecard = assessmentAgent.submit(attempt.id, answers)
        assertEquals(module.preTest.items.size.toDouble(), scorecard.score)
        assertEquals(module.preTest.items.size.toDouble(), scorecard.maxScore)
    }

    @Test
    fun `analytics builds class report with learning gains`() {
        val student = Student(nickname = "Dana")
        val preAttempt = assessmentAgent.start(module.preTest.id, student, module.id)
        assessmentAgent.submit(preAttempt.id, module.preTest.items.map { AnswerPayload(it.id, it.answer) })

        val postAttempt = assessmentAgent.start(module.postTest.id, student, module.id)
        val wrongAnswers = module.postTest.items.map { AnswerPayload(it.id, "wrong") }
        assessmentAgent.submit(postAttempt.id, wrongAnswers)

        val report = analyticsAgent.buildReports(module.id)
        assertEquals(module.id, report.moduleId)
        assertTrue(report.preAverage >= report.postAverage)
        assertEquals(module.preTest.id, report.preAssessmentId)
        assertEquals(module.postTest.id, report.postAssessmentId)
    }

    @Test
    fun `live session tracks participants`() {
        val student = Student(nickname = "Eli")
        val sessionId = liveSessionAgent.createSession(module.id, SessionSettings(true, SessionPace.TEACHER_LED))
        assertTrue(liveSessionAgent.join(sessionId, student))
        val item = module.preTest.items.first()
        assertTrue(liveSessionAgent.submit(sessionId, student.id, AnswerPayload(item.id, item.answer)))
        val snapshot = liveSessionAgent.snapshot(sessionId)
        assertNotNull(snapshot)
        assertEquals(1, snapshot.participants.size)
        assertEquals(student.id, snapshot.participants.first().studentId)
    }

    @Test
    fun `assignment submissions respect settings`() {
        val student = Student(nickname = "Fern")
        val assignment = assignmentAgent.schedule(
            module.id,
            java.time.Instant.now(),
            java.time.Instant.now().plusSeconds(60),
            AssignmentSettings(allowLateSubmissions = false, maxAttempts = 1)
        )
        val attempt = assessmentAgent.start(module.preTest.id, student, module.id)
        val answers = module.preTest.items.map { AnswerPayload(it.id, it.answer) }
        val scorecard = assessmentAgent.submit(attempt.id, answers)
        val storedAttempt = assessmentAgent.findAttempt(scorecard.attemptId)!!
        assertTrue(assignmentAgent.submit(assignment.id, storedAttempt))
    }

    @Test
    fun `reports export to filesystem`() {
        val student = Student(nickname = "Gale")
        val preAttempt = assessmentAgent.start(module.preTest.id, student, module.id)
        assessmentAgent.submit(preAttempt.id, module.preTest.items.map { AnswerPayload(it.id, it.answer) })
        val postAttempt = assessmentAgent.start(module.postTest.id, student, module.id)
        assessmentAgent.submit(postAttempt.id, module.postTest.items.map { AnswerPayload(it.id, it.answer) })

        val classReport = analyticsAgent.buildReports(module.id)
        val studentReport = analyticsAgent.studentReport(module.id, student.id)!!
        val classPath = reportExporter.exportClassReport(classReport, "build/test/class.txt")
        val studentPath = reportExporter.exportStudentReport(studentReport, "build/test/student.txt")
        assertTrue(java.io.File(classPath).exists())
        assertTrue(java.io.File(studentPath).exists())
        assertEquals(student.nickname, studentReport.nickname)
    }

    @Test
    fun `student nicknames flow through analytics and gamification`() {
        val student = Student(nickname = "Jude")
        val preAttempt = assessmentAgent.start(module.preTest.id, student, module.id)
        assessmentAgent.submit(preAttempt.id, module.preTest.items.map { AnswerPayload(it.id, "wrong") })
        val postAttempt = assessmentAgent.start(module.postTest.id, student, module.id)
        assessmentAgent.submit(postAttempt.id, module.postTest.items.map { AnswerPayload(it.id, it.answer) })

        val report = analyticsAgent.buildReports(module.id)
        val studentReport = analyticsAgent.studentReport(module.id, student.id)
        assertEquals(student.nickname, studentReport?.nickname)

        val badges = gamificationAgent.topImprovers(module.id)
        val badge = badges.firstOrNull { it.studentId == student.id }
        if (badge != null) {
            assertEquals(student.nickname, badge.nickname)
        }
        gamificationAgent.starOfTheDay(module.id)?.let {
            if (it.studentId == student.id) {
                assertEquals(student.nickname, it.nickname)
            }
        }
    }

    @Test
    fun `gamification badges surface improvements`() {
        val s1 = Student(nickname = "Hero")
        val s2 = Student(nickname = "Ivy")
        val pre1 = assessmentAgent.start(module.preTest.id, s1, module.id)
        assessmentAgent.submit(pre1.id, module.preTest.items.map { AnswerPayload(it.id, "wrong") })
        val post1 = assessmentAgent.start(module.postTest.id, s1, module.id)
        assessmentAgent.submit(post1.id, module.postTest.items.map { AnswerPayload(it.id, it.answer) })

        val pre2 = assessmentAgent.start(module.preTest.id, s2, module.id)
        assessmentAgent.submit(pre2.id, module.preTest.items.map { AnswerPayload(it.id, it.answer) })
        val post2 = assessmentAgent.start(module.postTest.id, s2, module.id)
        assessmentAgent.submit(post2.id, module.postTest.items.map { AnswerPayload(it.id, it.answer) })

        val badges = gamificationAgent.topImprovers(module.id)
        assertTrue(badges.isNotEmpty())
        assertNotNull(gamificationAgent.starOfTheDay(module.id))
    }
}
