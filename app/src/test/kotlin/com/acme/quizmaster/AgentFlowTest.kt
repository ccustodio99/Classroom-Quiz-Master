package com.acme.quizmaster

import com.acme.quizmaster.agents.AssessmentAgentImpl
import com.acme.quizmaster.agents.AssignmentAgentImpl
import com.acme.quizmaster.agents.GamificationAgentImpl
import com.acme.quizmaster.agents.ItemBankAgentImpl
import com.acme.quizmaster.agents.LiveSessionAgentImpl
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
import com.acme.quizmaster.domain.AttemptStatus
import com.acme.quizmaster.domain.SessionPace
import com.acme.quizmaster.domain.SessionSettings
import com.acme.quizmaster.domain.Student
import kotlin.io.path.createTempDirectory
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.io.File
import java.time.Instant

class AgentFlowTest {
    private lateinit var moduleRepository: ModuleRepository
    private lateinit var attemptRepository: AttemptRepository
    private lateinit var assignmentRepository: AssignmentRepository
    private lateinit var sessionRepository: SessionRepository
    private lateinit var itemBankRepository: ItemBankRepository

    private lateinit var itemBankAgent: ItemBankAgentImpl
    private lateinit var moduleBuilder: ModuleBuilderAgentImpl
    private lateinit var assessmentAgent: AssessmentAgentImpl
    private lateinit var liveSessionAgent: LiveSessionAgentImpl
    private lateinit var assignmentAgent: AssignmentAgentImpl
    private lateinit var analyticsAgent: ScoringAnalyticsAgentImpl
    private lateinit var reportExportAgent: ReportExportAgentImpl
    private lateinit var gamificationAgent: GamificationAgentImpl

    private lateinit var module: com.acme.quizmaster.domain.Module
    private lateinit var students: List<Student>

    @BeforeTest
    fun setup() {
        moduleRepository = ModuleRepository()
        attemptRepository = AttemptRepository()
        assignmentRepository = AssignmentRepository()
        sessionRepository = SessionRepository()
        itemBankRepository = ItemBankRepository()

        itemBankAgent = ItemBankAgentImpl(itemBankRepository)
        moduleBuilder = ModuleBuilderAgentImpl(moduleRepository, itemBankAgent)
        assessmentAgent = AssessmentAgentImpl(moduleRepository, attemptRepository)
        liveSessionAgent = LiveSessionAgentImpl(moduleRepository, sessionRepository, attemptRepository)
        assignmentAgent = AssignmentAgentImpl(moduleRepository, assignmentRepository)
        analyticsAgent = ScoringAnalyticsAgentImpl(moduleRepository, attemptRepository)
        reportExportAgent = ReportExportAgentImpl()
        gamificationAgent = GamificationAgentImpl(analyticsAgent)

        module = buildSampleModule(itemBankAgent)
        val result = moduleBuilder.createOrUpdate(module)
        check(result.isSuccess) { "Sample module failed validation: ${result.exceptionOrNull()?.message}" }

        students = listOf(Student(nickname = "Ana"), Student(nickname = "Ben"))
    }

    @Test
    fun `module builder accepts the sample module`() {
        val violations = moduleBuilder.validate(module)
        assertTrue(violations.isEmpty(), "Expected no validation issues but found: $violations")
        assertNotNull(moduleBuilder.findModule(module.id))
        assertTrue(moduleBuilder.listModules().any { it.id == module.id })
    }

    @Test
    fun `live session tracks participants and scores`() {
        val sessionId = liveSessionAgent.createSession(module.id, SessionSettings(true, SessionPace.TEACHER_LED))
        students.forEach { student ->
            assertTrue(liveSessionAgent.join(sessionId, student))
        }
        val firstItem = module.preTest.items.first()
        assertTrue(liveSessionAgent.submit(sessionId, students.first().id, AnswerPayload(firstItem.id, firstItem.answer)))

        val snapshot = liveSessionAgent.snapshot(sessionId)
        assertNotNull(snapshot)
        val participant = snapshot.participants.first { it.studentId == students.first().id }
        assertEquals(1, participant.answered)
        assertEquals(1.0, participant.score)
    }

    @Test
    fun `live session deduplicates answers and marks completion`() {
        val sessionId = liveSessionAgent.createSession(module.id, SessionSettings(true, SessionPace.TEACHER_LED))
        val student = students.first()
        assertTrue(liveSessionAgent.join(sessionId, student))

        val items = module.preTest.items
        val firstItem = items.first()

        // first attempt earns credit
        assertTrue(liveSessionAgent.submit(sessionId, student.id, AnswerPayload(firstItem.id, firstItem.answer)))
        // resubmitting the same item should not increase the answered count
        assertTrue(liveSessionAgent.submit(sessionId, student.id, AnswerPayload(firstItem.id, firstItem.answer)))

        var snapshot = liveSessionAgent.snapshot(sessionId)
        assertNotNull(snapshot)
        var participant = snapshot.participants.first { it.studentId == student.id }
        assertEquals(1, participant.answered)
        assertEquals(1.0, participant.score)

        // finish remaining items to complete the attempt
        items.drop(1).forEach { item ->
            assertTrue(liveSessionAgent.submit(sessionId, student.id, AnswerPayload(item.id, item.answer)))
        }

        snapshot = liveSessionAgent.snapshot(sessionId)
        assertNotNull(snapshot)
        participant = snapshot.participants.first { it.studentId == student.id }
        assertEquals(items.size, participant.answered)
        assertEquals(items.size.toDouble(), participant.score)

        val attempt = attemptRepository.attemptsForStudent(module.id, student.id)
            .first { it.assessmentId == module.preTest.id }
        assertEquals(AttemptStatus.SUBMITTED, attempt.status)
        assertNotNull(attempt.submittedAt)
    }

    @Test
    fun `assessment agent scores attempts and persists state`() {
        val student = students.first()
        val attempt = assessmentAgent.start(module.preTest.id, student, module.id)
        val answers = module.preTest.items.map { AnswerPayload(it.id, it.answer) }
        val scorecard = assessmentAgent.submit(attempt.id, answers)

        assertEquals(module.preTest.items.size.toDouble(), scorecard.score)
        val storedAttempt = assessmentAgent.findAttempt(scorecard.attemptId)
        assertNotNull(storedAttempt)
        assertEquals(AttemptStatus.SUBMITTED, storedAttempt.status)
    }

    @Test
    fun `assignment agent enforces schedule and attempt limits`() {
        val assignment = assignmentAgent.schedule(
            moduleId = module.id,
            startDate = Instant.now().minusSeconds(60),
            dueDate = Instant.now().plusSeconds(3600),
            settings = AssignmentSettings(allowLateSubmissions = false, maxAttempts = 1)
        )
        val attempt = assessmentAgent.start(module.postTest.id, students.first(), module.id)
        val answers = module.postTest.items.map { AnswerPayload(it.id, it.answer) }
        val scorecard = assessmentAgent.submit(attempt.id, answers)
        val storedAttempt = assessmentAgent.findAttempt(scorecard.attemptId)!!

        assertTrue(assignmentAgent.submit(assignment.id, storedAttempt))
        assertFalse(assignmentAgent.submit(assignment.id, storedAttempt))
    }

    @Test
    fun `reports aggregate submitted attempts and export to disk`() {
        seedCompletedAssessments()

        val classReport = analyticsAgent.buildReports(module.id)
        assertTrue(classReport.postAverage >= classReport.preAverage)
        assertEquals(2, classReport.attempts.count { it.assessmentId == module.postTest.id })

        val tempDir = createTempDirectory().toFile().apply { deleteOnExit() }
        val classPath = reportExportAgent.exportClassReport(classReport, File(tempDir, "class.txt").absolutePath)
        val classCsv = reportExportAgent.exportClassCsv(classReport, File(tempDir, "class.csv").absolutePath)
        assertTrue(File(classPath).exists())
        assertTrue(File(classCsv).exists())

        val studentReport = analyticsAgent.studentReport(module.id, students.last().id)
        assertNotNull(studentReport)
        val studentExport = reportExportAgent.exportStudentReport(studentReport, File(tempDir, "student.txt").absolutePath)
        val studentCsv = reportExportAgent.exportStudentCsv(studentReport, File(tempDir, "student.csv").absolutePath)
        assertTrue(File(studentExport).exists())
        assertTrue(File(studentCsv).exists())
    }

    @Test
    fun `gamification surfaces top improver and star of the day`() {
        seedCompletedAssessments()

        val topImprovers = gamificationAgent.topImprovers(module.id)
        assertTrue(topImprovers.isNotEmpty())
        assertEquals("Top Improver", topImprovers.first().badge)
        assertEquals("Ben", topImprovers.first().nickname)

        val star = gamificationAgent.starOfTheDay(module.id)
        assertNotNull(star)
        assertEquals("Star of the Day", star.badge)
    }

    private fun seedCompletedAssessments() {
        val ana = students.first()
        val ben = students.last()

        val anaPre = assessmentAgent.start(module.preTest.id, ana, module.id)
        assessmentAgent.submit(anaPre.id, module.preTest.items.map { AnswerPayload(it.id, it.answer) })

        val benPre = assessmentAgent.start(module.preTest.id, ben, module.id)
        assessmentAgent.submit(
            benPre.id,
            module.preTest.items.mapIndexed { index, item ->
                val response = if (index == 0) {
                    item.options.firstOrNull()?.id ?: ""
                } else {
                    item.answer
                }
                AnswerPayload(item.id, response)
            }
        )

        val anaPost = assessmentAgent.start(module.postTest.id, ana, module.id)
        assessmentAgent.submit(anaPost.id, module.postTest.items.map { AnswerPayload(it.id, it.answer) })

        val benPost = assessmentAgent.start(module.postTest.id, ben, module.id)
        assessmentAgent.submit(benPost.id, module.postTest.items.map { AnswerPayload(it.id, it.answer) })
    }
}
