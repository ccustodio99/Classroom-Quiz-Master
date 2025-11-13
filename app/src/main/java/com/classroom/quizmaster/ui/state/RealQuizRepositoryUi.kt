package com.classroom.quizmaster.ui.state

import com.classroom.quizmaster.domain.model.MediaAsset
import com.classroom.quizmaster.domain.model.MediaType
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.Quiz
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.model.QuizOverviewUi
import com.classroom.quizmaster.ui.teacher.home.ACTION_ASSIGNMENTS
import com.classroom.quizmaster.ui.teacher.home.ACTION_CREATE_QUIZ
import com.classroom.quizmaster.ui.teacher.home.ACTION_LAUNCH_SESSION
import com.classroom.quizmaster.ui.teacher.home.ACTION_REPORTS
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    private val authRepository: AuthRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : QuizRepositoryUi {

    override val teacherHome: Flow<TeacherHomeUiState> =
        combine(
            authRepository.authState,
            quizRepository.quizzes
        ) { auth, quizzes ->
            val greeting = buildGreeting(
                displayName = auth.teacherProfile?.displayName ?: auth.displayName,
                email = auth.teacherProfile?.email ?: auth.email
            )

            val recent = buildRecentQuizzes(quizzes)
            TeacherHomeUiState(
                greeting = greeting,
                classrooms = buildClassrooms(quizzes),
                actionCards = defaultActionCards,
                recentQuizzes = recent,
                emptyMessage = if (recent.isEmpty()) DEFAULT_QUIZ_EMPTY_MESSAGE else "",
                isOfflineDemo = false
            )
        }
            .flowOn(dispatcher)

    override fun quizEditorState(quizId: String?): Flow<QuizEditorUiState> {
        return if (quizId.isNullOrBlank()) {
            flowOf(QuizEditorUiState())
        } else {
            quizRepository.quizzes
                .map { quizzes ->
                    quizzes.firstOrNull { it.id == quizId }?.toEditorState()
                        ?: QuizEditorUiState(quizId = quizId, isNewQuiz = false)
                }
                .distinctUntilChanged()
                .onStart { emit(QuizEditorUiState(quizId = quizId, isNewQuiz = false)) }
                .flowOn(dispatcher)
        }
    }

    override suspend fun persistDraft(state: QuizEditorUiState) {
        withContext(dispatcher) {
            val authState = authRepository.authState.first()
            val teacherId = authState.userId ?: error("No authenticated teacher available")

            val existing = state.quizId?.takeIf { it.isNotBlank() }?.let { quizRepository.getQuiz(it) }
            val createdAt = existing?.createdAt ?: Clock.System.now()
            val resolvedQuizId = existing?.id ?: state.quizId.orEmpty()

            val quiz = Quiz(
                id = resolvedQuizId,
                teacherId = teacherId,
                title = state.title.ifBlank { "Untitled quiz" },
                defaultTimePerQ = state.timePerQuestionSeconds,
                shuffle = state.shuffleQuestions,
                createdAt = createdAt,
                updatedAt = Clock.System.now(),
                questions = state.questions.mapIndexed { index, question ->
                    question.toDomainQuestion(resolvedQuizId, index)
                }
            )
            quizRepository.upsert(quiz)
        }
    }

    private fun buildGreeting(displayName: String?, email: String?): String {
        val trimmedDisplay = displayName?.trim().orEmpty()
        val firstName = trimmedDisplay.substringBefore(' ').takeIf { it.isNotBlank() }
        val emailPrefix = email?.substringBefore('@')?.trim()
        val resolvedName = sequenceOf(trimmedDisplay, firstName, emailPrefix)
            .mapNotNull { it }
            .firstOrNull { it.isNotBlank() }
            ?: "there"
        return "Welcome back, $resolvedName"
    }

    private fun buildClassrooms(@Suppress("UNUSED_PARAMETER") quizzes: List<Quiz>): List<ClassroomOverviewUi> = emptyList()

    private fun buildRecentQuizzes(quizzes: List<Quiz>): List<QuizOverviewUi> =
        quizzes
            .sortedByDescending { it.updatedAt }
            .take(5)
            .map { quiz -> quiz.toOverview() }

    private fun Quiz.toOverview(): QuizOverviewUi = QuizOverviewUi(
        id = id,
        title = title.ifBlank { "Untitled quiz" },
        grade = "",
        subject = "",
        questionCount = questions.size.takeIf { it > 0 } ?: questionCount,
        averageScore = 0,
        updatedAgo = formatRelativeTime(updatedAt),
        isDraft = questions.isEmpty(),
        classroomName = "",
        topicName = ""
    )

    private fun Quiz.toEditorState(): QuizEditorUiState = QuizEditorUiState(
        quizId = id,
        title = title,
        grade = "",
        subject = "",
        questions = questions.mapIndexed { index, question ->
            question.toDraft(index)
        },
        timePerQuestionSeconds = defaultTimePerQ,
        shuffleQuestions = shuffle,
        lastSavedRelative = formatRelativeTime(updatedAt),
        isNewQuiz = false
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
            id = ACTION_LAUNCH_SESSION,
            title = "Launch live",
            description = "Open a LAN lobby and start hosting.",
            route = ACTION_LAUNCH_SESSION,
            ctaLabel = "Launch lobby",
            primary = true
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
