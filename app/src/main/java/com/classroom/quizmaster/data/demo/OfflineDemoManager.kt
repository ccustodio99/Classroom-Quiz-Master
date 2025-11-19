package com.classroom.quizmaster.data.demo

import androidx.room.withTransaction
import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.entity.AssignmentLocalEntity
import com.classroom.quizmaster.data.local.entity.ClassroomEntity
import com.classroom.quizmaster.data.local.entity.QuestionEntity
import com.classroom.quizmaster.data.local.entity.QuizEntity
import com.classroom.quizmaster.data.local.entity.TeacherEntity
import com.classroom.quizmaster.data.local.entity.TopicEntity
import com.classroom.quizmaster.domain.model.DemoMode
import com.classroom.quizmaster.domain.model.QuizCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.days

@Singleton
class OfflineDemoManager @Inject constructor(
    private val database: QuizMasterDatabase,
    private val preferences: AppPreferencesDataSource,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun enableDemoMode(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            seedEntities()
            val currentFlags = preferences.featureFlags.first()
            if (!currentFlags.contains(DemoMode.OFFLINE_FLAG)) {
                preferences.setFeatureFlags(currentFlags + DemoMode.OFFLINE_FLAG)
            }
            preferences.setLastTeacherId(DemoMode.TEACHER_ID)
        }
    }

    private suspend fun seedEntities() {
        val teacherDao = database.teacherDao()
        val classroomDao = database.classroomDao()
        val topicDao = database.topicDao()
        val quizDao = database.quizDao()
        val assignmentDao = database.assignmentDao()
        val now = Clock.System.now()
        val teacherCreatedAt = now - 120.days
        val classroomCreatedAt = now - 30.days
        val topicCreatedAt = now - 7.days
        val quizCreatedAt = now - 5.days
        val assignmentOpenAt = now - 2.days
        val assignmentCloseAt = now + 5.days

        database.withTransaction {
            if (teacherDao.get(DemoMode.TEACHER_ID) == null) {
                teacherDao.upsert(
                    TeacherEntity(
                        id = DemoMode.TEACHER_ID,
                        displayName = DemoMode.TEACHER_NAME,
                        email = DemoMode.TEACHER_EMAIL,
                        createdAt = teacherCreatedAt.toEpochMilliseconds()
                    )
                )
            }

            val classroomEntity = ClassroomEntity(
                id = DEMO_CLASSROOM_ID,
                teacherId = DemoMode.TEACHER_ID,
                name = "Period 1 Algebra",
                grade = "8",
                subject = "Math",
                createdAt = classroomCreatedAt.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds(),
                isArchived = false,
                archivedAt = null
            )
            classroomDao.upsert(classroomEntity)

            val topicEntity = TopicEntity(
                id = DEMO_TOPIC_ID,
                classroomId = DEMO_CLASSROOM_ID,
                teacherId = DemoMode.TEACHER_ID,
                name = "Fractions Fundamentals",
                description = "Unit 1: Fractions and ratios",
                createdAt = topicCreatedAt.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds(),
                isArchived = false,
                archivedAt = null
            )
            topicDao.upsert(topicEntity)

            val questions = buildDemoQuestions(DEMO_QUIZ_ID, now)
            val quizEntity = QuizEntity(
                id = DEMO_QUIZ_ID,
                teacherId = DemoMode.TEACHER_ID,
                classroomId = DEMO_CLASSROOM_ID,
                topicId = DEMO_TOPIC_ID,
                title = "Fractions Quick Check",
                defaultTimePerQ = 45,
                shuffle = true,
                questionCount = questions.size,
                category = QuizCategory.STANDARD.name,
                createdAt = quizCreatedAt.toEpochMilliseconds(),
                updatedAt = now.toEpochMilliseconds(),
                isArchived = false,
                archivedAt = null
            )
            quizDao.upsertQuizWithQuestions(quizEntity, questions)

            if (assignmentDao.getAssignment(DEMO_ASSIGNMENT_ID) == null) {
                assignmentDao.upsertAssignments(
                    listOf(
                        AssignmentLocalEntity(
                            id = DEMO_ASSIGNMENT_ID,
                            quizId = DEMO_QUIZ_ID,
                            classroomId = DEMO_CLASSROOM_ID,
                            topicId = DEMO_TOPIC_ID,
                            openAt = assignmentOpenAt.toEpochMilliseconds(),
                            closeAt = assignmentCloseAt.toEpochMilliseconds(),
                            attemptsAllowed = 3,
                            scoringMode = "BEST",
                            revealAfterSubmit = true,
                            createdAt = assignmentOpenAt.toEpochMilliseconds(),
                            updatedAt = assignmentOpenAt.toEpochMilliseconds(),
                            isArchived = false,
                            archivedAt = null
                        )
                    )
                )
            }
        }
    }

    private fun buildDemoQuestions(quizId: String, updatedAt: Instant): List<QuestionEntity> {
        val stems = listOf(
            "Which fraction is equivalent to 1/2?" to listOf("2/4", "3/6", "2/3", "3/5"),
            "True or false: 3/4 is greater than 2/3." to listOf("True", "False"),
            "Select all fractions that simplify to 1/3." to listOf("2/6", "3/9", "4/6", "5/15"),
            "What is 5/8 + 1/8?" to listOf("6/16", "6/8", "5/16", "3/4"),
            "Order the fractions from least to greatest." to listOf("1/4", "1/3", "2/5", "3/4")
        )
        return stems.mapIndexed { index, (stem, choices) ->
            val correctChoices = when (index) {
                0 -> listOf("2/4", "3/6")
                1 -> listOf("True")
                2 -> listOf("2/6", "3/9", "5/15")
                3 -> listOf("6/8")
                else -> listOf("1/4", "1/3", "2/5", "3/4")
            }
            QuestionEntity(
                id = "${quizId}-q${index + 1}",
                quizId = quizId,
                type = if (index == 1) "TF" else "MCQ",
                stem = stem,
                choicesJson = json.encodeToString(choices),
                answerKeyJson = json.encodeToString(correctChoices),
                explanation = demoExplanation(index),
                mediaType = null,
                mediaUrl = null,
                timeLimitSeconds = if (index == 4) 60 else 45,
                position = index,
                updatedAt = updatedAt.toEpochMilliseconds()
            )
        }
    }

    private fun demoExplanation(index: Int): String = when (index) {
        0 -> "Multiply numerator and denominator by the same number to check equivalence."
        1 -> "Compare each fraction's decimal value to determine which is larger."
        2 -> "Reduce each fraction to simplest form to see if it equals 1/3."
        3 -> "Use a common denominator of 8 before adding."
        else -> "Convert each fraction to a decimal to order them quickly."
    }

    companion object {
        private const val DEMO_CLASSROOM_ID = "demo-classroom"
        private const val DEMO_TOPIC_ID = "demo-topic"
        private const val DEMO_QUIZ_ID = "demo-quiz"
        private const val DEMO_ASSIGNMENT_ID = "demo-assignment"
    }
}
