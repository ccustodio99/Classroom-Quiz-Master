package com.classroom.quizmaster.ui.teacher.reports

import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.JoinRequest
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.QuizCategory
import com.classroom.quizmaster.domain.model.ScoringMode
import com.classroom.quizmaster.domain.model.Teacher
import com.classroom.quizmaster.domain.model.Student
import com.classroom.quizmaster.domain.model.Submission
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.ui.state.RealAssignmentRepositoryUi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealAssignmentRepositoryUiTest {

    @Test
    fun `buildReportsState aggregates scores and improvements`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val assignmentsRepo = FakeAssignmentRepository()
        val quizRepo = FakeQuizRepository()
        val classroomRepo = FakeClassroomRepository()
        val subject = RealAssignmentRepositoryUi(assignmentsRepo, quizRepo, classroomRepo, dispatcher)

        val now = Instant.parse("2024-01-01T00:00:00Z")
        val classroom = Classroom(
            id = "c1",
            teacherId = "t1",
            name = "Algebra",
            grade = "8",
            subject = "Math",
            joinCode = "JOIN",
            createdAt = now,
            updatedAt = now
        )
        val topicFractions = Topic(
            id = "t1",
            classroomId = classroom.id,
            teacherId = classroom.teacherId,
            name = "Fractions",
            description = "",
            createdAt = now,
            updatedAt = now
        )
        val topicDecimals = topicFractions.copy(id = "t2", name = "Decimals")

        val quizStandard = Quiz(
            id = "q1",
            teacherId = classroom.teacherId,
            classroomId = classroom.id,
            topicId = topicFractions.id,
            title = "Standard quiz",
            defaultTimePerQ = 30,
            shuffle = false,
            createdAt = now,
            updatedAt = now,
            category = QuizCategory.STANDARD
        )
        val quizPre = quizStandard.copy(id = "q2", topicId = topicDecimals.id, title = "Pre", category = QuizCategory.PRE_TEST)
        val quizPost = quizStandard.copy(id = "q3", topicId = topicFractions.id, title = "Post", category = QuizCategory.POST_TEST)

        val assignmentStandard = Assignment(
            id = "a1",
            quizId = quizStandard.id,
            classroomId = classroom.id,
            topicId = topicFractions.id,
            openAt = now,
            closeAt = now,
            attemptsAllowed = 1,
            scoringMode = ScoringMode.BEST,
            revealAfterSubmit = true,
            createdAt = now,
            updatedAt = now
        )
        val assignmentPre = assignmentStandard.copy(id = "a2", quizId = quizPre.id, topicId = topicDecimals.id)
        val assignmentPost = assignmentStandard.copy(id = "a3", quizId = quizPost.id, topicId = topicFractions.id)

        val s1 = Student(id = "s1", displayName = "Alex", email = "a@school", createdAt = now)
        val s2 = Student(id = "s2", displayName = "Jamie", email = "j@school", createdAt = now)

        classroomRepo.classroomsFlow.value = listOf(classroom)
        classroomRepo.topicsFlow.value = listOf(topicFractions, topicDecimals)
        classroomRepo.studentsFlow.value = listOf(s1, s2)
        quizRepo.quizzesFlow.value = listOf(quizStandard, quizPre, quizPost)
        assignmentsRepo.assignmentsFlow.value = listOf(assignmentStandard, assignmentPre, assignmentPost)

        assignmentsRepo.setSubmissions(
            assignmentStandard.id,
            listOf(
                Submission(uid = s1.id, assignmentId = assignmentStandard.id, bestScore = 80, lastScore = 80, attempts = 1, updatedAt = now),
                Submission(uid = s2.id, assignmentId = assignmentStandard.id, bestScore = 60, lastScore = 60, attempts = 1, updatedAt = now)
            )
        )
        assignmentsRepo.setSubmissions(
            assignmentPre.id,
            listOf(
                Submission(uid = s1.id, assignmentId = assignmentPre.id, bestScore = 50, lastScore = 50, attempts = 1, updatedAt = now),
                Submission(uid = s2.id, assignmentId = assignmentPre.id, bestScore = 40, lastScore = 40, attempts = 1, updatedAt = now)
            )
        )
        assignmentsRepo.setSubmissions(
            assignmentPost.id,
            listOf(
                Submission(uid = s1.id, assignmentId = assignmentPost.id, bestScore = 80, lastScore = 80, attempts = 1, updatedAt = now),
                Submission(uid = s2.id, assignmentId = assignmentPost.id, bestScore = 60, lastScore = 60, attempts = 1, updatedAt = now)
            )
        )

        advanceUntilIdle()

        val state = subject.reports.first { it.assignments.isNotEmpty() }

        assertThat(state.classroomName).isEqualTo("Algebra")
        assertThat(state.average).isEqualTo(62)
        assertThat(state.median).isEqualTo(60)
        assertThat(state.topTopics.map { it.topicName }).containsExactly("Fractions", "Decimals").inOrder()
        assertThat(state.topTopics.first().averageScore).isEqualTo(70)
        assertThat(state.assignments.map { it.pValue }).containsExactlyElementsIn(listOf(70, 45, 70))
        assertThat(state.classPreAverage).isEqualTo(45)
        assertThat(state.classPostAverage).isEqualTo(70)
        assertThat(state.classDelta).isEqualTo(25)
        assertThat(state.studentProgress.first().name).isEqualTo("Alex")
        assertThat(state.studentImprovement.map { it.name }).containsExactly("Alex", "Jamie").inOrder()
        assertThat(state.studentImprovement.first().delta).isEqualTo(30)
        assertThat(state.completionOverview.onTimeRate).isEqualTo(100)
        assertThat(state.assignmentCompletion).hasSize(3)
        assertThat(state.studentCompletion.first { it.studentId == s1.id }.completedOnTime).isEqualTo(3)
        assertThat(state.studentProgressAtRisk.first { it.studentId == s2.id }.atRisk).isTrue()
        assertThat(state.questionDifficulty).isEmpty()
    }
}

private class FakeAssignmentRepository : AssignmentRepository {
    val assignmentsFlow = MutableStateFlow<List<Assignment>>(emptyList())
    private val submissionFlows = mutableMapOf<String, MutableStateFlow<List<Submission>>>()

    override val assignments: Flow<List<Assignment>> = assignmentsFlow

    override fun submissions(assignmentId: String): Flow<List<Submission>> =
        submissionFlows.getOrPut(assignmentId) { MutableStateFlow(emptyList()) }

    override fun submissionsForUser(userId: String): Flow<List<Submission>> = MutableStateFlow(emptyList())

    fun setSubmissions(assignmentId: String, values: List<Submission>) {
        submissionFlows.getOrPut(assignmentId) { MutableStateFlow(emptyList()) }.value = values
    }

    override suspend fun refreshAssignments() {}
    override suspend fun createAssignment(assignment: Assignment) {}
    override suspend fun getAssignment(id: String): Assignment? = assignmentsFlow.value.firstOrNull { it.id == id }
    override suspend fun updateAssignment(assignment: Assignment) {}
    override suspend fun archiveAssignment(id: String, archivedAt: Instant) {}
    override suspend fun unarchiveAssignment(id: String, unarchivedAt: Instant) {}
    override suspend fun submitHomework(submission: Submission) {}
}

private class FakeQuizRepository : QuizRepository {
    val quizzesFlow = MutableStateFlow<List<Quiz>>(emptyList())
    override val quizzes: Flow<List<Quiz>> = quizzesFlow
    override suspend fun refresh() {}
    override suspend fun upsert(quiz: Quiz) {}
    override suspend fun delete(id: String) {}
    override suspend fun getQuiz(id: String): Quiz? = quizzesFlow.value.firstOrNull { it.id == id }
    override suspend fun seedDemoData() {}
}

private class FakeClassroomRepository : ClassroomRepository {
    val classroomsFlow = MutableStateFlow<List<Classroom>>(emptyList())
    val topicsFlow = MutableStateFlow<List<Topic>>(emptyList())
    val studentsFlow = MutableStateFlow<List<Student>>(emptyList())
    override val classrooms: Flow<List<Classroom>> = classroomsFlow
    override val topics: Flow<List<Topic>> = topicsFlow
    override val archivedClassrooms: Flow<List<Classroom>> = MutableStateFlow(emptyList())
    override val archivedTopics: Flow<List<Topic>> = MutableStateFlow(emptyList())
    override val joinRequests: Flow<List<JoinRequest>> = MutableStateFlow(emptyList())
    override val students: Flow<List<Student>> = studentsFlow
    override suspend fun refresh() {}
    override suspend fun upsertClassroom(classroom: Classroom): String = classroom.id
    override suspend fun archiveClassroom(classroomId: String, archivedAt: Instant) {}
    override suspend fun upsertTopic(topic: Topic): String = topic.id
    override suspend fun archiveTopic(topicId: String, archivedAt: Instant) {}
    override suspend fun createJoinRequest(joinCode: String) {}
    override suspend fun createJoinRequest(classroomId: String, teacherId: String) {}
    override suspend fun approveJoinRequest(requestId: String) {}
    override suspend fun denyJoinRequest(requestId: String) {}
    override suspend fun addStudentByEmailOrUsername(classroomId: String, identifier: String) {}
    override suspend fun removeStudentFromClassroom(classroomId: String, studentId: String) {}
    override suspend fun getStudent(id: String): Student? = studentsFlow.value.firstOrNull { it.id == id }
    override suspend fun searchTeachers(query: String): List<com.classroom.quizmaster.domain.model.Teacher> = emptyList()
    override suspend fun getClassroomsForTeacher(teacherId: String): List<Classroom> = classroomsFlow.value.filter { it.teacherId == teacherId }
    override suspend fun getClassroom(id: String): Classroom? = classroomsFlow.value.firstOrNull { it.id == id }
    override suspend fun getTopic(id: String): Topic? = topicsFlow.value.firstOrNull { it.id == id }
}
