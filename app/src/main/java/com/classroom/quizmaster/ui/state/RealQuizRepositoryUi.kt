package com.classroom.quizmaster.ui.state

import com.classroom.quizmaster.domain.model.Classroom
import com.classroom.quizmaster.domain.model.MediaAsset
import com.classroom.quizmaster.domain.model.MediaType
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.model.QuizCategory
import com.classroom.quizmaster.domain.model.Topic
import com.classroom.quizmaster.domain.model.DemoMode
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.model.QuizOverviewUi
import com.classroom.quizmaster.ui.model.QuizCategoryUi
import com.classroom.quizmaster.ui.model.SelectionOptionUi
import com.classroom.quizmaster.ui.teacher.home.ACTION_ASSIGNMENTS
import com.classroom.quizmaster.ui.teacher.home.ACTION_CREATE_QUIZ
import com.classroom.quizmaster.ui.teacher.home.ACTION_REPORTS
import com.classroom.quizmaster.ui.teacher.home.ACTION_CLASSROOMS
import com.classroom.quizmaster.ui.teacher.home.ClassroomOverviewUi
import com.classroom.quizmaster.ui.teacher.home.HomeActionCard
import com.classroom.quizmaster.ui.teacher.home.TeacherHomeUiState
import com.classroom.quizmaster.ui.teacher.quiz_editor.QuizEditorUiState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Singleton
class RealQuizRepositoryUi @Inject constructor(
    private val quizRepository: QuizRepository,
    private val classroomRepository: ClassroomRepository,
    private val authRepository: AuthRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : QuizRepositoryUi {

    override val teacherHome: Flow<TeacherHomeUiState> =
        combine(
            authRepository.authState,
            classroomRepository.classrooms,
            classroomRepository.topics,
            quizRepository.quizzes
        ) { auth, classrooms, topics, quizzes ->
            val displayName = auth.teacherProfile?.displayName ?: auth.displayName
            val email = auth.teacherProfile?.email ?: auth.email
            val teacherName = resolveTeacherName(displayName, email)
            val greeting = buildGreeting(teacherName)
            val isOfflineDemo = auth.userId == DemoMode.TEACHER_ID

            val activeClassrooms = classrooms.filterNot { it.isArchived }
            val activeTopics = topics.filterNot { it.isArchived }
            val activeQuizzes = quizzes.filterNot { it.isArchived }
                .filter { quiz ->
                    activeClassrooms.any { it.id == quiz.classroomId } &&
                        activeTopics.any { it.id == quiz.topicId } &&
                        quiz.category == QuizCategory.STANDARD
                }
            val defaultClassroomId = activeTopics.firstOrNull()?.classroomId
                ?: activeClassrooms.firstOrNull()?.id
            val defaultTopicId = activeTopics.firstOrNull { it.classroomId == defaultClassroomId }?.id
            val recent = buildRecentQuizzes(activeQuizzes, activeClassrooms, activeTopics)
            TeacherHomeUiState(
                greeting = greeting,
                teacherName = teacherName,
                classrooms = buildClassrooms(activeClassrooms, activeTopics, activeQuizzes),
                actionCards = defaultActionCards,
                recentQuizzes = recent,
                emptyMessage = if (recent.isEmpty()) DEFAULT_QUIZ_EMPTY_MESSAGE else "",
                isOfflineDemo = isOfflineDemo,
                defaultClassroomId = defaultClassroomId,
                defaultTopicId = defaultTopicId
            )
        }
            .flowOn(dispatcher)

    override fun quizEditorState(
        classroomId: String,
        topicId: String,
        quizId: String?
    ): Flow<QuizEditorUiState> =
        combine(
            classroomRepository.classrooms,
            classroomRepository.topics,
            quizRepository.quizzes
        ) { classrooms, topics, quizzes ->
            val activeClassrooms = classrooms.filterNot { it.isArchived }
            val activeTopics = topics.filterNot { it.isArchived }

            val classroomOptions = activeClassrooms
                .sortedBy { it.name.lowercase() }
                .map { classroom ->
                    val supporting = listOfNotNull(
                        classroom.grade.takeIf { it.isNotBlank() }?.let { "Grade $it" },
                        classroom.subject.takeIf { it.isNotBlank() }
                    ).joinToString(separator = " • ")
                    SelectionOptionUi(
                        id = classroom.id,
                        label = classroom.name,
                        supportingText = supporting
                    )
                }

            val topicsByClassroom = activeTopics
                .groupBy { it.classroomId }
                .mapValues { (_, groupedTopics) ->
                    groupedTopics
                        .sortedBy { it.name.lowercase() }
                        .map { topic ->
                            SelectionOptionUi(
                                id = topic.id,
                                label = topic.name,
                                supportingText = topic.description.takeIf { it.isNotBlank() }.orEmpty()
                            )
                        }
                }

            val existingState = quizId?.let { id ->
                quizzes.firstOrNull { it.id == id }?.toEditorState()
            }

            val validClassroomId = when {
                existingState?.classroomId?.let { id ->
                    activeClassrooms.any { it.id == id }
                } == true -> existingState.classroomId
                activeClassrooms.any { it.id == classroomId } -> classroomId
                else -> classroomOptions.firstOrNull()?.id.orEmpty()
            }

            val resolvedTopics = topicsByClassroom[validClassroomId].orEmpty()
            val validTopicId = when {
                existingState?.topicId?.let { id ->
                    resolvedTopics.any { it.id == id }
                } == true -> existingState.topicId
                resolvedTopics.any { it.id == topicId } -> topicId
                else -> resolvedTopics.firstOrNull()?.id.orEmpty()
            }

            val selectedClassroom = activeClassrooms.firstOrNull { it.id == validClassroomId }
            val baseState = existingState ?: QuizEditorUiState(
                isNewQuiz = quizId.isNullOrBlank(),
                classroomId = validClassroomId,
                topicId = validTopicId,
                quizCategory = QuizCategoryUi.Standard
            )

            baseState.copy(
                quizId = existingState?.quizId ?: quizId,
                classroomId = validClassroomId,
                topicId = validTopicId,
                grade = baseState.grade.ifBlank { selectedClassroom?.grade.orEmpty() },
                subject = baseState.subject.ifBlank { selectedClassroom?.subject.orEmpty() },
                classroomOptions = classroomOptions,
                topicsByClassroom = topicsByClassroom,
                quizCategory = existingState?.quizCategory ?: baseState.quizCategory
            )
        }
            .distinctUntilChanged()
            .onStart {
                emit(
                    QuizEditorUiState(
                        quizId = quizId,
                        classroomId = classroomId,
                        topicId = topicId
                    )
                )
            }
            .flowOn(dispatcher)

    override suspend fun persistDraft(state: QuizEditorUiState) {
        withContext(dispatcher) {
            val authState = authRepository.authState.first()
            val teacherId = authState.userId ?: error("No authenticated teacher available")

            val existing = state.quizId?.takeIf { it.isNotBlank() }?.let { quizRepository.getQuiz(it) }
            val createdAt = existing?.createdAt ?: Clock.System.now()
            val resolvedQuizId = existing?.id ?: state.quizId.orEmpty()
            val activeClassrooms = classroomRepository.classrooms.first().filterNot { it.isArchived }
            val resolvedClassroomId = state.classroomId
                .takeIf { it.isNotBlank() }
                ?: existing?.classroomId
                ?: activeClassrooms.firstOrNull()?.id
                ?: error("Create a classroom before saving a quiz")
            val activeTopics = classroomRepository.topics.first()
                .filterNot { it.isArchived }
                .filter { it.classroomId == resolvedClassroomId }
            val resolvedTopicId = state.topicId
                .takeIf { it.isNotBlank() }
                ?: existing?.topicId
                ?: activeTopics.firstOrNull()?.id
                ?: error("Create a topic inside the classroom before saving a quiz")

            val quiz = Quiz(
                id = resolvedQuizId,
                teacherId = teacherId,
                classroomId = resolvedClassroomId,
                topicId = resolvedTopicId,
                title = state.title.ifBlank { "Untitled quiz" },
                defaultTimePerQ = state.timePerQuestionSeconds,
                shuffle = state.shuffleQuestions,
                category = state.quizCategory.toDomain(),
                createdAt = createdAt,
                updatedAt = Clock.System.now(),
                questions = state.questions.mapIndexed { index, question ->
                    question.toDomainQuestion(resolvedQuizId, index)
                }
            )
            quizRepository.upsert(quiz)
        }
    }

    private fun resolveTeacherName(displayName: String?, email: String?): String {
        val trimmedDisplay = displayName?.trim().orEmpty()
        val firstName = trimmedDisplay.substringBefore(' ').takeIf { it.isNotBlank() }
        val emailPrefix = email?.substringBefore('@')?.trim()
        return sequenceOf(trimmedDisplay, firstName, emailPrefix)
            .mapNotNull { it }
            .firstOrNull { it.isNotBlank() }
            ?: "Teacher"
    }

    private fun buildGreeting(name: String): String =
        if (name.isBlank()) "Welcome back" else "Welcome back, $name"

    private fun buildClassrooms(
        classrooms: List<Classroom>,
        topics: List<Topic>,
        quizzes: List<Quiz>
    ): List<ClassroomOverviewUi> = classrooms
        .sortedByDescending { it.createdAt }
        .map { classroom ->
            val topicCount = topics.count { it.classroomId == classroom.id }
            val quizCount = quizzes.count { it.classroomId == classroom.id && it.category == QuizCategory.STANDARD }
            ClassroomOverviewUi(
                id = classroom.id,
                name = classroom.name,
                grade = classroom.grade.takeIf { it.isNotBlank() },
                topicCount = topicCount,
                quizCount = quizCount
            )
        }

    private fun buildRecentQuizzes(
        quizzes: List<Quiz>,
        classrooms: List<Classroom>,
        topics: List<Topic>
    ): List<QuizOverviewUi> =
        quizzes
            .filter { it.category == QuizCategory.STANDARD }
            .sortedByDescending { it.updatedAt }
            .take(5)
            .mapNotNull { quiz ->
                val classroom = classrooms.firstOrNull { it.id == quiz.classroomId } ?: return@mapNotNull null
                val topic = topics.firstOrNull { it.id == quiz.topicId } ?: return@mapNotNull null
                quiz.toOverview(classroom, topic)
            }

    private fun Quiz.toOverview(classroom: Classroom, topic: Topic): QuizOverviewUi = QuizOverviewUi(
        id = id,
        title = title.ifBlank { "Untitled quiz" },
        grade = classroom.grade,
        subject = classroom.subject,
        questionCount = questions.size.takeIf { it > 0 } ?: questionCount,
        averageScore = 0,
        updatedAgo = formatRelativeTime(updatedAt),
        isDraft = questions.isEmpty(),
        classroomName = classroom.name,
        topicName = topic.name
    )

    private fun Quiz.toEditorState(): QuizEditorUiState = QuizEditorUiState(
        quizId = id,
        classroomId = classroomId,
        topicId = topicId,
        title = title,
        grade = "",
        subject = "",
        questions = questions.mapIndexed { index, question ->
            question.toDraft(index)
        },
        timePerQuestionSeconds = defaultTimePerQ,
        shuffleQuestions = shuffle,
        lastSavedRelative = formatRelativeTime(updatedAt),
        isNewQuiz = false,
        quizCategory = category.toUi()
    )

    private fun Question.toDraft(index: Int): QuestionDraftUi {
        val answerOptions = choices.mapIndexed { answerIndex, choice ->
            AnswerOptionUi(
                id = "${id}_$answerIndex",
                label = ('A' + answerIndex).toString(),
                text = choice,
                correct = answerKey.contains(choice)
            )
        }
        return QuestionDraftUi(
            id = if (id.isNotBlank()) id else "q$index",
            stem = stem,
            type = when (type) {
                QuestionType.MCQ -> QuestionTypeUi.MultipleChoice
                QuestionType.TF -> QuestionTypeUi.TrueFalse
                QuestionType.FILL_IN -> QuestionTypeUi.FillIn
                QuestionType.MATCHING -> QuestionTypeUi.Match
            },
            answers = if (answerOptions.isNotEmpty()) answerOptions else defaultAnswersForType(type),
            explanation = explanation,
            mediaThumb = media?.url,
            timeLimitSeconds = timeLimitSeconds
        )
    }

    private fun defaultAnswersForType(type: QuestionType): List<AnswerOptionUi> = when (type) {
        QuestionType.MCQ -> listOf("A", "B", "C", "D")
        QuestionType.TF -> listOf("True", "False")
        QuestionType.FILL_IN -> listOf("Answer")
        QuestionType.MATCHING -> listOf("Pair 1", "Pair 2")
    }.mapIndexed { index, label ->
        AnswerOptionUi(
            id = "placeholder_$index",
            label = ('A' + index).toString(),
            text = label,
            correct = index == 0
        )
    }

    private fun QuestionDraftUi.toDomainQuestion(quizId: String, index: Int): Question {
        val choices = answers.map { it.text }
        val correctAnswers = answers.filter { it.correct }.map { it.text }
        val mediaAsset = mediaThumb?.takeIf { it.isNotBlank() }?.let {
            MediaAsset(type = MediaType.IMAGE, url = it)
        }
        return Question(
            id = id.ifBlank { "${quizId.ifBlank { "new" }}-q$index" },
            quizId = quizId,
            type = when (type) {
                QuestionTypeUi.MultipleChoice -> QuestionType.MCQ
                QuestionTypeUi.TrueFalse -> QuestionType.TF
                QuestionTypeUi.FillIn -> QuestionType.FILL_IN
                QuestionTypeUi.Match -> QuestionType.MATCHING
            },
            stem = stem,
            choices = choices,
            answerKey = correctAnswers,
            explanation = explanation,
            media = mediaAsset,
            timeLimitSeconds = timeLimitSeconds
        )
    }

    private fun formatRelativeTime(updatedAt: Instant): String {
        val now = Clock.System.now()
        val duration = (now - updatedAt).coerceAtLeast(Duration.ZERO)
        return when {
            duration < 1.minutes -> "Just now"
            duration < 1.hours -> "${duration.inWholeMinutes} min ago"
            duration < 24.hours -> "${duration.inWholeHours} hr ago"
            duration < 7.days -> "${duration.inWholeDays} d ago"
            else -> "${duration.inWholeDays / 7} wk ago"
        }
    }

    private fun QuizCategory.toUi(): QuizCategoryUi = when (this) {
        QuizCategory.STANDARD -> QuizCategoryUi.Standard
        QuizCategory.PRE_TEST -> QuizCategoryUi.PreTest
        QuizCategory.POST_TEST -> QuizCategoryUi.PostTest
    }

    private fun QuizCategoryUi.toDomain(): QuizCategory = when (this) {
        QuizCategoryUi.Standard -> QuizCategory.STANDARD
        QuizCategoryUi.PreTest -> QuizCategory.PRE_TEST
        QuizCategoryUi.PostTest -> QuizCategory.POST_TEST
    }

    private val defaultActionCards: List<HomeActionCard> = listOf(
        HomeActionCard(
            id = ACTION_CREATE_QUIZ,
            title = "Create a quiz",
            description = "Build standards-aligned quizzes with templates.",
            route = ACTION_CREATE_QUIZ,
            ctaLabel = "Create quiz",
            primary = true
        ),
        HomeActionCard(
            id = ACTION_CLASSROOMS,
            title = "Manage classrooms",
            description = "Create, edit, and archive your classrooms.",
            route = ACTION_CLASSROOMS,
            ctaLabel = "Manage"
        ),
        HomeActionCard(
            id = ACTION_ASSIGNMENTS,
            title = "Assignments",
            description = "Schedule asynchronous practice.",
            route = ACTION_ASSIGNMENTS,
            ctaLabel = "Manage"
        ),
        HomeActionCard(
            id = ACTION_REPORTS,
            title = "Reports",
            description = "Review insights and export data.",
            route = ACTION_REPORTS,
            ctaLabel = "View reports"
        )
    )

    companion object {
        private const val DEFAULT_QUIZ_EMPTY_MESSAGE = "Create your first quiz to see it here."
    }
}
