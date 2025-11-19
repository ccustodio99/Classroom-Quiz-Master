package com.classroom.quizmaster.data.demo

import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.QuizCategory
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import java.util.UUID
import timber.log.Timber

@Singleton
class SampleDataSeeder @Inject constructor(
    private val authRepository: AuthRepository,
    private val classroomRepository: ClassroomRepository,
    private val quizRepository: QuizRepository,
    private val assignmentRepository: AssignmentRepository,
    private val preferences: AppPreferencesDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun seed(): Result<Unit> = withContext(dispatcher) {
        runCatching {
            val teacherId = authRepository.authState.first { it.userId != null }.userId!!
            val alreadySeeded = preferences.sampleSeededTeachers.first().contains(teacherId)
            if (alreadySeeded) return@runCatching

            val now = Clock.System.now()
            val createdClassroomIds = mutableSetOf<String>()
            val classrooms = SAMPLE_CLASSROOMS.map { template ->
                val classroom = Classroom(
                    id = "",
                    teacherId = teacherId,
                    name = template.name,
                    grade = template.grade,
                    subject = template.subject,
                    createdAt = now,
                    updatedAt = now
                )
                val classroomId = classroomRepository.upsertClassroom(classroom)
                createdClassroomIds += classroomId
                template to classroomId
            }

            classrooms.forEach { (template, classroomId) ->
                template.topics.forEach { topicTemplate ->
                    val topicNow = Clock.System.now()
                    val topic = Topic(
                        id = "",
                        classroomId = classroomId,
                        teacherId = teacherId,
                        name = topicTemplate.name,
                        description = topicTemplate.description,
                        createdAt = topicNow,
                        updatedAt = topicNow
                    )
                    val topicId = classroomRepository.upsertTopic(topic)

                    topicTemplate.quizzes.forEach { quizTemplate ->
                        val quizNow = Clock.System.now()
                        val quizId = "seed-quiz-${UUID.randomUUID()}"
                        val questions = quizTemplate.questions.mapIndexed { index, question ->
                            Question(
                                id = "",
                                quizId = quizId,
                                type = question.type,
                                stem = question.stem,
                                choices = question.choices,
                                answerKey = question.correctAnswers,
                                explanation = question.explanation,
                                timeLimitSeconds = question.timeLimitSeconds
                            )
                        }
                        val quiz = com.classroom.quizmaster.domain.model.Quiz(
                            id = quizId,
                            teacherId = teacherId,
                            classroomId = classroomId,
                            topicId = topicId,
                            title = quizTemplate.title,
                            defaultTimePerQ = quizTemplate.defaultTimeSeconds,
                            shuffle = true,
                            createdAt = quizNow,
                            category = QuizCategory.STANDARD,
                            updatedAt = quizNow,
                            questions = questions
                        )
                        quizRepository.upsert(quiz)

                        if (quizTemplate.assignment != null) {
                            val openAt = quizNow
                            val closeAt = openAt + quizTemplate.assignment.windowHours.hours
                            val assignment = Assignment(
                                id = "seed-assignment-${UUID.randomUUID()}",
                                quizId = quizId,
                                classroomId = classroomId,
                                topicId = topicId,
                                openAt = openAt,
                                closeAt = closeAt,
                                attemptsAllowed = quizTemplate.assignment.attempts,
                                scoringMode = quizTemplate.assignment.scoringMode,
                                revealAfterSubmit = quizTemplate.assignment.revealAfterSubmit,
                                createdAt = openAt,
                                updatedAt = openAt
                            )
                            assignmentRepository.createAssignment(assignment)
                        }
                    }
                }
            }

            preferences.addSampleSeededTeacher(teacherId, createdClassroomIds)
            Timber.i("Seeded %d classrooms for %s", createdClassroomIds.size, teacherId)
        }
    }

    suspend fun clearSeededData(): Result<Unit> = withContext(dispatcher) {
        runCatching {
            val teacherId = authRepository.authState.first { it.userId != null }.userId!!
            val classroomIds = preferences.seededClassroomIds(teacherId)
            if (classroomIds.isEmpty()) return@runCatching
            classroomIds.forEach { classroomId ->
                runCatching { classroomRepository.archiveClassroom(classroomId) }
                    .onFailure { Timber.w(it, "Unable to archive seeded classroom $classroomId") }
            }
            preferences.removeSampleSeededTeacher(teacherId)
            Timber.i("Cleared %d seeded classrooms for %s", classroomIds.size, teacherId)
        }
    }

    private data class SampleClassroom(
        val name: String,
        val grade: String,
        val subject: String,
        val topics: List<SampleTopic>
    )

    private data class SampleTopic(
        val name: String,
        val description: String,
        val quizzes: List<SampleQuiz>
    )

    private data class SampleQuiz(
        val title: String,
        val defaultTimeSeconds: Int,
        val questions: List<SampleQuestion>,
        val assignment: SampleAssignment? = null
    )

    private data class SampleAssignment(
        val attempts: Int,
        val windowHours: Int,
        val scoringMode: com.classroom.quizmaster.domain.model.ScoringMode,
        val revealAfterSubmit: Boolean
    )

    private data class SampleQuestion(
        val type: QuestionType,
        val stem: String,
        val choices: List<String>,
        val correctAnswers: List<String>,
        val explanation: String,
        val timeLimitSeconds: Int = 45
    )

    private companion object {
        private val SAMPLE_CLASSROOMS = listOf(
            SampleClassroom(
                name = "Period 2 Algebra",
                grade = "7",
                subject = "Math",
                topics = listOf(
                    SampleTopic(
                        name = "Ratios & Rates",
                        description = "Use tables and diagrams to compare ratios.",
                        quizzes = listOf(
                            SampleQuiz(
                                title = "Intro to Ratios",
                                defaultTimeSeconds = 40,
                                questions = listOf(
                                    SampleQuestion(
                                        type = QuestionType.MCQ,
                                        stem = "A class has 12 girls and 9 boys. What is the ratio of boys to total students?",
                                        choices = listOf("3:4", "4:7", "9:21", "4:3"),
                                        correctAnswers = listOf("3:7"),
                                        explanation = "There are 21 students; 9:21 simplifies to 3:7."
                                    ),
                                    SampleQuestion(
                                        type = QuestionType.TF,
                                        stem = "True or False: 4:5 = 16:20",
                                        choices = listOf("True", "False"),
                                        correctAnswers = listOf("True"),
                                        explanation = "Multiply both parts of 4:5 by 4."
                                    )
                                ),
                                assignment = SampleAssignment(
                                    attempts = 2,
                                    windowHours = 72,
                                    scoringMode = com.classroom.quizmaster.domain.model.ScoringMode.BEST,
                                    revealAfterSubmit = true
                                )
                            )
                        )
                    ),
                    SampleTopic(
                        name = "Linear Equations",
                        description = "Solve and graph one-variable equations.",
                        quizzes = listOf(
                            SampleQuiz(
                                title = "Two-step Equations",
                                defaultTimeSeconds = 50,
                                questions = listOf(
                                    SampleQuestion(
                                        type = QuestionType.MCQ,
                                        stem = "Solve: 3x + 5 = 17",
                                        choices = listOf("4", "3", "2", "5"),
                                        correctAnswers = listOf("4"),
                                        explanation = "Subtract 5 then divide by 3."
                                    ),
                                    SampleQuestion(
                                        type = QuestionType.MCQ,
                                        stem = "Which equation represents the sentence: 'Six less than twice a number is 8'?",
                                        choices = listOf("2x - 6 = 8", "6x - 2 = 8", "2x + 6 = 8", "6 - 2x = 8"),
                                        correctAnswers = listOf("2x - 6 = 8"),
                                        explanation = "Twice a number is 2x, six less is subtract 6."
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            SampleClassroom(
                name = "World History Honors",
                grade = "10",
                subject = "History",
                topics = listOf(
                    SampleTopic(
                        name = "Ancient Civilizations",
                        description = "Geography and culture of early civilizations.",
                        quizzes = listOf(
                            SampleQuiz(
                                title = "Geography & Trade",
                                defaultTimeSeconds = 45,
                                questions = listOf(
                                    SampleQuestion(
                                        type = QuestionType.MCQ,
                                        stem = "Which river supported the development of Mesopotamia?",
                                        choices = listOf("Nile", "Tigris & Euphrates", "Indus", "Yellow"),
                                        correctAnswers = listOf("Tigris & Euphrates"),
                                        explanation = "Mesopotamia lay between the Tigris and Euphrates rivers."
                                    ),
                                    SampleQuestion(
                                        type = QuestionType.MCQ,
                                        stem = "Which invention most improved long-distance trade?",
                                        choices = listOf("Wheel", "Stone tools", "Paper", "Concrete"),
                                        correctAnswers = listOf("Wheel"),
                                        explanation = "The wheel enabled carts and improved transport."
                                    )
                                ),
                                assignment = SampleAssignment(
                                    attempts = 1,
                                    windowHours = 48,
                                    scoringMode = com.classroom.quizmaster.domain.model.ScoringMode.LAST,
                                    revealAfterSubmit = false
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}
