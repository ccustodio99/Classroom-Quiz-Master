package com.classroom.quizmaster.ui.teacher.classrooms.detail

import androidx.lifecycle.SavedStateHandle
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.QuizCategory
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.testing.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherClassroomDetailViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val classroomId = "class-1"
    private val teacherId = "teacher-1"

    @Test
    fun `diagnostic tests include topic name from active topics`() = runTest {
        val classroomRepo = FakeClassroomRepository()
        val quizRepo = FakeQuizRepository()
        val savedState = SavedStateHandle(mapOf(TeacherClassroomDetailViewModel.CLASSROOM_ID_KEY to classroomId))
        val now = Instant.parse("2024-01-01T00:00:00Z")

        classroomRepo.sendClassrooms(
            listOf(
                Classroom(
                    id = classroomId,
                    teacherId = teacherId,
                    name = "Algebra",
                    grade = "8",
                    subject = "Math",
                    createdAt = now,
                    updatedAt = now
                )
            )
        )
        classroomRepo.sendTopics(
            listOf(
                Topic(
                    id = "topic-a",
                    classroomId = classroomId,
                    teacherId = teacherId,
                    name = "Intro to Fractions",
                    description = "",
                    createdAt = now,
                    updatedAt = now
                ),
                Topic(
                    id = "topic-b",
                    classroomId = classroomId,
                    teacherId = teacherId,
                    name = "Decimals",
                    description = "",
                    createdAt = now,
                    updatedAt = now
                ),
                Topic(
                    id = "topic-archived",
                    classroomId = classroomId,
                    teacherId = teacherId,
                    name = "Legacy",
                    description = "",
                    createdAt = now,
                    updatedAt = now,
                    isArchived = true
                )
            )
        )
        val preTest = Quiz(
            id = "pre-1",
            teacherId = teacherId,
            classroomId = classroomId,
            topicId = "topic-a",
            title = "Pre assessment",
            defaultTimePerQ = 30,
            shuffle = true,
            createdAt = now,
            updatedAt = now,
            questionCount = 10,
            category = QuizCategory.PRE_TEST
        )
        val postTest = preTest.copy(
            id = "post-1",
            topicId = "topic-b",
            title = "Post assessment",
            updatedAt = now + 1.days,
            category = QuizCategory.POST_TEST
        )
        val archivedTopicQuiz = preTest.copy(
            id = "archived-topic",
            topicId = "topic-archived",
            category = QuizCategory.PRE_TEST,
            updatedAt = now + 2.days
        )
        quizRepo.sendQuizzes(listOf(preTest, postTest, archivedTopicQuiz))

        val viewModel = TeacherClassroomDetailViewModel(classroomRepo, quizRepo, savedState)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.preTest).isNotNull()
        assertThat(state.preTest?.id).isEqualTo("pre-1")
        assertThat(state.preTest?.topicName).isEqualTo("Intro to Fractions")
        assertThat(state.postTest).isNotNull()
        assertThat(state.postTest?.topicName).isEqualTo("Decimals")
    }

    @Test
    fun `deleteTest archives quiz through repository`() = runTest {
        val classroomRepo = FakeClassroomRepository()
        val quizRepo = FakeQuizRepository()
        val savedState = SavedStateHandle(mapOf(TeacherClassroomDetailViewModel.CLASSROOM_ID_KEY to classroomId))
        val viewModel = TeacherClassroomDetailViewModel(classroomRepo, quizRepo, savedState)

        viewModel.deleteTest("quiz-42")
        advanceUntilIdle()

        assertThat(quizRepo.deletedIds).containsExactly("quiz-42")
    }
}

private class FakeClassroomRepository : ClassroomRepository {
    private val _classrooms = MutableStateFlow<List<Classroom>>(emptyList())
    private val _topics = MutableStateFlow<List<Topic>>(emptyList())

    override val classrooms: Flow<List<Classroom>> = _classrooms
    override val topics: Flow<List<Topic>> = _topics
    override val archivedClassrooms: Flow<List<Classroom>> = MutableStateFlow(emptyList())
    override val archivedTopics: Flow<List<Topic>> = MutableStateFlow(emptyList())

    fun sendClassrooms(value: List<Classroom>) {
        _classrooms.value = value
    }

    fun sendTopics(value: List<Topic>) {
        _topics.value = value
    }

    override suspend fun refresh() = Unit
    override suspend fun upsertClassroom(classroom: Classroom): String = error("Not needed in tests")
    override suspend fun archiveClassroom(classroomId: String, archivedAt: Instant) = Unit
    override suspend fun upsertTopic(topic: Topic): String = error("Not needed in tests")
    override suspend fun archiveTopic(topicId: String, archivedAt: Instant) = Unit
    override suspend fun getClassroom(id: String): Classroom? = _classrooms.value.firstOrNull { it.id == id }
    override suspend fun getTopic(id: String): Topic? = _topics.value.firstOrNull { it.id == id }
}

private class FakeQuizRepository : QuizRepository {
    private val _quizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val deletedIds = mutableListOf<String>()

    override val quizzes: Flow<List<Quiz>> = _quizzes

    fun sendQuizzes(value: List<Quiz>) {
        _quizzes.value = value
    }

    override suspend fun refresh() = Unit
    override suspend fun upsert(quiz: Quiz) = Unit
    override suspend fun delete(id: String) {
        deletedIds += id
    }

    override suspend fun getQuiz(id: String): Quiz? = _quizzes.value.firstOrNull { it.id == id }
    override suspend fun seedDemoData() = Unit
}
