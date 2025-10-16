package com.acme.quizmaster.agents

import com.acme.quizmaster.data.AssignmentRepository
import com.acme.quizmaster.data.AttemptRepository
import com.acme.quizmaster.data.ItemBankRepository
import com.acme.quizmaster.data.ModuleRepository
import com.acme.quizmaster.data.SessionRepository
import com.acme.quizmaster.domain.AnswerPayload
import com.acme.quizmaster.domain.Assessment
import com.acme.quizmaster.domain.Attempt
import com.acme.quizmaster.domain.AttemptStatus
import com.acme.quizmaster.domain.AssignmentSettings
import com.acme.quizmaster.domain.ClassReport
import com.acme.quizmaster.domain.FeedbackMode
import com.acme.quizmaster.domain.Item
import com.acme.quizmaster.domain.ItemType
import com.acme.quizmaster.domain.Lesson
import com.acme.quizmaster.domain.LessonSlide
import com.acme.quizmaster.domain.MatchingPair
import com.acme.quizmaster.domain.Module
import com.acme.quizmaster.domain.ModuleSettings
import com.acme.quizmaster.domain.ObjectiveGain
import com.acme.quizmaster.domain.ObjectiveScore
import com.acme.quizmaster.domain.Scorecard
import com.acme.quizmaster.domain.SessionPace
import com.acme.quizmaster.domain.SessionSettings
import com.acme.quizmaster.domain.Student
import com.acme.quizmaster.domain.StudentReport
import com.acme.quizmaster.domain.Violation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.util.Locale

class AgentImplementationsTest {

    @Test
    fun `module builder validates and persists fully aligned modules`() {
        val moduleRepository = ModuleRepository()
        val itemBankRepository = ItemBankRepository()
        val agent = ModuleBuilderAgentImpl(moduleRepository, ItemBankAgentImpl(itemBankRepository))

        val module = sampleModule()

        val result = agent.createOrUpdate(module)

        assertTrue(result.isSuccess)
        assertEquals(module, moduleRepository.find(module.id))
        assertTrue(itemBankRepository.items("LO1").isNotEmpty())
        assertTrue(itemBankRepository.items("LO4").any { it.type == ItemType.MATCHING })
    }

    @Test
    fun `module builder catches lesson and assessment violations`() {
        val moduleRepository = ModuleRepository()
        val itemBankRepository = ItemBankRepository()
        val agent = ModuleBuilderAgentImpl(moduleRepository, ItemBankAgentImpl(itemBankRepository))

        val invalidModule = sampleModule().copy(
            objectives = listOf("LO1", "LO2"),
            lesson = sampleModule().lesson.copy(slides = listOf(sampleSlide("LO1"))),
            postTest = sampleModule().postTest.copy(items = listOf(sampleModule().postTest.items.first()))
        )

        val violations = agent.validate(invalidModule)
        assertTrue(violations.contains(Violation("lesson", "Slides missing coverage for LO2")))
        assertTrue(violations.any { it.field == "assessments" })
    }

    @Test
    fun `assessment agent starts and scores attempts across item types`() {
        val moduleRepository = ModuleRepository()
        val attemptRepository = AttemptRepository()
        val module = sampleModule()
        moduleRepository.upsert(module)
        val agent = AssessmentAgentImpl(moduleRepository, attemptRepository)
        val student = Student(id = "s1", nickname = "Ana")

        val attempt = agent.start(module.preTest.id, student, module.id)
        val answers = listOf(
            AnswerPayload("pre-mcq", "A"),
            AnswerPayload("pre-tf", "true"),
            AnswerPayload("pre-numeric", "12.6"),
            AnswerPayload("pre-matching", "1:one|2:two")
        )

        val scorecard = agent.submit(attempt.id, answers)

        assertEquals(4.0, scorecard.score, 0.0)
        assertEquals(4.0, scorecard.maxScore, 0.0)
        assertEquals(4, scorecard.objectiveBreakdown.size)
        assertEquals(AttemptStatus.SUBMITTED, attemptRepository.find(attempt.id)?.status)
    }

    @Test
    fun `live session agent tracks joins submissions and snapshots`() {
        val moduleRepository = ModuleRepository()
        val attemptRepository = AttemptRepository()
        val sessionRepository = SessionRepository()
        val module = sampleModule()
        moduleRepository.upsert(module)
        val agent = LiveSessionAgentImpl(moduleRepository, sessionRepository, attemptRepository)

        val sessionId = agent.createSession(module.id, SessionSettings(leaderboardEnabled = true, pace = SessionPace.TEACHER_LED))
        val student = Student(id = "s1", nickname = "Ana")
        val second = Student(id = "s2", nickname = "Ben")
        assertTrue(agent.join(sessionId, student))
        assertTrue(agent.join(sessionId, second))

        assertTrue(agent.submit(sessionId, student.id, AnswerPayload("pre-mcq", "A")))

        val snapshot = agent.snapshot(sessionId)
        assertNotNull(snapshot)
        val progress = snapshot!!.participants.first { it.studentId == student.id }
        assertEquals(1, progress.answered)
        assertEquals(module.preTest.items.size, progress.total)
        assertTrue(progress.score > 0)
    }

    @Test
    fun `lesson agent returns slides aligned to objectives`() {
        val moduleRepository = ModuleRepository()
        val module = sampleModule()
        moduleRepository.upsert(module)
        val agent = LessonAgentImpl(moduleRepository)

        val slides = agent.slidesFor(module.id)
        assertEquals(module.lesson.slides.size, slides.size)
        assertTrue(slides.all { it.objective in module.objectives })
    }

    @Test
    fun `assignment agent enforces scheduling and submission rules`() {
        val moduleRepository = ModuleRepository()
        val assignmentRepository = AssignmentRepository()
        val module = sampleModule()
        moduleRepository.upsert(module)
        val agent = AssignmentAgentImpl(moduleRepository, assignmentRepository)

        val start = Instant.now().minusSeconds(120)
        val due = Instant.now().minusSeconds(30)
        val assignment = agent.schedule(
            moduleId = module.id,
            startDate = start,
            dueDate = due,
            settings = AssignmentSettings(allowLateSubmissions = false, maxAttempts = 1)
        )

        val attempt = Attempt(
            assessmentId = module.preTest.id,
            moduleId = module.id,
            studentId = "s1",
            studentNickname = "Ana",
            status = AttemptStatus.SUBMITTED,
            score = 2.0,
            maxScore = 4.0,
            responses = emptyList(),
            submittedAt = Instant.now()
        )

        assertFalse(agent.submit(assignment.id, attempt))

        val retryFriendly = agent.schedule(
            moduleId = module.id,
            startDate = Instant.now(),
            dueDate = Instant.now().plusSeconds(3600),
            settings = AssignmentSettings(allowLateSubmissions = true, maxAttempts = 2)
        )

        val acceptedFirst = agent.submit(retryFriendly.id, attempt)
        assertTrue(acceptedFirst)
        val higherScore = attempt.copy(score = 3.0)
        val acceptedSecond = agent.submit(retryFriendly.id, higherScore)
        assertTrue(acceptedSecond)
        val rejectedThird = agent.submit(retryFriendly.id, higherScore.copy(score = 1.0))
        assertFalse(rejectedThird)
    }

    @Test
    fun `analytics agent aggregates learning gains per objective`() {
        val moduleRepository = ModuleRepository()
        val attemptRepository = AttemptRepository()
        val module = sampleModule()
        moduleRepository.upsert(module)
        val assessmentAgent = AssessmentAgentImpl(moduleRepository, attemptRepository)

        val studentA = Student(id = "s1", nickname = "Ana")
        val studentB = Student(id = "s2", nickname = "Ben")

        val preAnswersStrong = listOf(
            AnswerPayload("pre-mcq", "A"),
            AnswerPayload("pre-tf", "true"),
            AnswerPayload("pre-numeric", "12.5"),
            AnswerPayload("pre-matching", "1:one|2:two")
        )
        val preAnswersWeak = listOf(
            AnswerPayload("pre-mcq", "B"),
            AnswerPayload("pre-tf", "false"),
            AnswerPayload("pre-numeric", "10.0"),
            AnswerPayload("pre-matching", "1:one|2:two")
        )
        val postAnswersAllCorrect = listOf(
            AnswerPayload("post-mcq", "A"),
            AnswerPayload("post-tf", "true"),
            AnswerPayload("post-numeric", "12.4"),
            AnswerPayload("post-matching", "1:one|2:two")
        )
        val postAnswersPartial = listOf(
            AnswerPayload("post-mcq", "A"),
            AnswerPayload("post-tf", "false"),
            AnswerPayload("post-numeric", "20.0"),
            AnswerPayload("post-matching", "1:one|2:two")
        )

        val attemptApre = assessmentAgent.start(module.preTest.id, studentA, module.id)
        assessmentAgent.submit(attemptApre.id, preAnswersStrong)
        val attemptApost = assessmentAgent.start(module.postTest.id, studentA, module.id)
        assessmentAgent.submit(attemptApost.id, postAnswersAllCorrect)

        val attemptBpre = assessmentAgent.start(module.preTest.id, studentB, module.id)
        assessmentAgent.submit(attemptBpre.id, preAnswersWeak)
        val attemptBpost = assessmentAgent.start(module.postTest.id, studentB, module.id)
        assessmentAgent.submit(attemptBpost.id, postAnswersPartial)

        val analytics = ScoringAnalyticsAgentImpl(moduleRepository, attemptRepository)
        val report = analytics.buildReports(module.id)

        assertEquals(module.id, report.moduleId)
        assertTrue(report.postAverage > report.preAverage)
        assertEquals(module.objectives.size, report.objectives.size)
        assertEquals(report.postAverage - report.preAverage, report.learningGain, 1e-6)

        val studentReport = analytics.studentReport(module.id, studentA.id)
        assertNotNull(studentReport)
        assertTrue(studentReport!!.learningGain >= 0)
        assertEquals(module.objectives.size, studentReport.objectiveGains.size)
    }

    @Test
    fun `report export agent writes class and student outputs`() {
        val exportAgent = ReportExportAgentImpl()
        val report = ClassReport(
            moduleId = "module",
            topic = "Interest",
            preAssessmentId = "pre",
            postAssessmentId = "post",
            preAverage = 0.25,
            postAverage = 0.75,
            learningGain = 0.5,
            objectives = listOf(ObjectiveGain("LO1", 0.2, 0.9)),
            attempts = listOf(
                Scorecard(
                    attemptId = "a1",
                    studentId = "s1",
                    studentNickname = "Ana",
                    moduleId = "module",
                    assessmentId = "post",
                    score = 3.0,
                    maxScore = 4.0,
                    objectiveBreakdown = mapOf("LO1" to ObjectiveScore("LO1", 3, 4)),
                    submittedAt = Instant.parse("2024-01-01T00:00:00Z")
                )
            )
        )
        val studentReport = StudentReport(
            moduleId = "module",
            studentId = "s1",
            nickname = "Ana",
            preScore = 1.0,
            postScore = 3.0,
            learningGain = 2.0,
            objectiveGains = listOf(ObjectiveGain("LO1", 0.25, 0.75))
        )

        val tempDir = Files.createTempDirectory("reports").toFile()
        val classPath = exportAgent.exportClassReport(report, File(tempDir, "class.txt").absolutePath)
        val studentPath = exportAgent.exportStudentReport(studentReport, File(tempDir, "student.txt").absolutePath)
        val classCsv = exportAgent.exportClassCsv(report, File(tempDir, "class.csv").absolutePath)
        val studentCsv = exportAgent.exportStudentCsv(studentReport, File(tempDir, "student.csv").absolutePath)

        assertTrue(File(classPath).readText().contains("Learning Gain"))
        assertTrue(File(studentPath).readText().contains("Student Report"))
        assertTrue(File(classCsv).readText().contains("objective,pre_percent"))
        assertTrue(File(studentCsv).readText().contains("pre_score"))
    }

    @Test
    fun `gamification agent surfaces top improvers and star of the day`() {
        val moduleRepository = ModuleRepository()
        val attemptRepository = AttemptRepository()
        val module = sampleModule()
        moduleRepository.upsert(module)
        val assessmentAgent = AssessmentAgentImpl(moduleRepository, attemptRepository)

        val students = listOf(
            Student(id = "s1", nickname = "Ana"),
            Student(id = "s2", nickname = "Ben"),
            Student(id = "s3", nickname = "Cara")
        )

        val preAnswersLow = listOf(
            AnswerPayload("pre-mcq", "B"),
            AnswerPayload("pre-tf", "false"),
            AnswerPayload("pre-numeric", "20.0"),
            AnswerPayload("pre-matching", "1:one|2:two")
        )
        val postAnswersHigh = listOf(
            AnswerPayload("post-mcq", "A"),
            AnswerPayload("post-tf", "true"),
            AnswerPayload("post-numeric", "12.5"),
            AnswerPayload("post-matching", "1:one|2:two")
        )
        val postAnswersMedium = listOf(
            AnswerPayload("post-mcq", "A"),
            AnswerPayload("post-tf", "false"),
            AnswerPayload("post-numeric", "12.8"),
            AnswerPayload("post-matching", "1:one|2:two")
        )

        students.forEachIndexed { index, student ->
            val pre = assessmentAgent.start(module.preTest.id, student, module.id)
            assessmentAgent.submit(pre.id, preAnswersLow)
            val post = assessmentAgent.start(module.postTest.id, student, module.id)
            val answers = if (index == 0) postAnswersHigh else postAnswersMedium
            assessmentAgent.submit(post.id, answers)
        }

        val analytics = ScoringAnalyticsAgentImpl(moduleRepository, attemptRepository)
        val gamification = GamificationAgentImpl(analytics)

        val badges = gamification.topImprovers(module.id)
        assertEquals(3, badges.size)
        assertTrue(badges.first().badge.contains("Top Improver"))
        val star = gamification.starOfTheDay(module.id)
        assertNotNull(star)
        assertEquals("Star of the Day", star!!.badge)
    }

    private fun sampleModule(): Module {
        val objectives = listOf("LO1", "LO2", "LO3", "LO4")
        val pre = Assessment(
            id = "pre",
            name = "Pre-Test",
            timeLimit = Duration.ofMinutes(15),
            items = listOf(
                Item(
                    id = "pre-mcq",
                    type = ItemType.MULTIPLE_CHOICE,
                    stem = "Select correct option",
                    objective = "LO1",
                    options = listOf(
                        com.acme.quizmaster.domain.Option("A", "A"),
                        com.acme.quizmaster.domain.Option("B", "B")
                    ),
                    answer = "A",
                    explanation = "A is correct"
                ),
                Item(
                    id = "pre-tf",
                    type = ItemType.TRUE_FALSE,
                    stem = "Statement is true",
                    objective = "LO2",
                    answer = "TRUE",
                    explanation = "It is true"
                ),
                Item(
                    id = "pre-numeric",
                    type = ItemType.NUMERIC,
                    stem = "Compute value",
                    objective = "LO3",
                    answer = "12.5",
                    tolerance = 0.5,
                    explanation = "Computation"
                ),
                Item(
                    id = "pre-matching",
                    type = ItemType.MATCHING,
                    stem = "Match numbers",
                    objective = "LO4",
                    answer = "1:one|2:two",
                    matchingPairs = listOf(
                        MatchingPair("1", "one"),
                        MatchingPair("2", "two")
                    ),
                    explanation = "Pairs"
                )
            )
        )
        val post = Assessment(
            id = "post",
            name = "Post-Test",
            timeLimit = Duration.ofMinutes(15),
            items = listOf(
                Item(
                    id = "post-mcq",
                    type = ItemType.MULTIPLE_CHOICE,
                    stem = "Select correct option",
                    objective = "LO1",
                    options = listOf(
                        com.acme.quizmaster.domain.Option("A", "A"),
                        com.acme.quizmaster.domain.Option("B", "B")
                    ),
                    answer = "A",
                    explanation = "A is correct"
                ),
                Item(
                    id = "post-tf",
                    type = ItemType.TRUE_FALSE,
                    stem = "Statement is true",
                    objective = "LO2",
                    answer = "TRUE",
                    explanation = "It is true"
                ),
                Item(
                    id = "post-numeric",
                    type = ItemType.NUMERIC,
                    stem = "Compute value",
                    objective = "LO3",
                    answer = "12.5",
                    tolerance = 0.5,
                    explanation = "Computation"
                ),
                Item(
                    id = "post-matching",
                    type = ItemType.MATCHING,
                    stem = "Match numbers",
                    objective = "LO4",
                    answer = "1:one|2:two",
                    matchingPairs = listOf(
                        MatchingPair("1", "one"),
                        MatchingPair("2", "two")
                    ),
                    explanation = "Pairs"
                )
            )
        )
        val slides = objectives.map { sampleSlide(it) }
        return Module(
            id = "module",
            topic = "Compound Interest",
            objectives = objectives,
            preTest = pre,
            lesson = Lesson(
                id = "lesson",
                title = "Lesson Deck",
                slides = slides
            ),
            postTest = post,
            settings = ModuleSettings(
                leaderboardEnabled = true,
                allowRetakes = false,
                locale = Locale("en", "PH").toLanguageTag(),
                feedbackMode = FeedbackMode.END_OF_MODULE
            )
        )
    }

    private fun sampleSlide(objective: String) = LessonSlide(
        title = "Slide $objective",
        body = "Content for $objective",
        objective = objective,
        media = listOf("media-$objective.png")
    )
}
