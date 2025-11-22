package com.classroom.quizmaster.ui.state

import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Question
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.SessionStatus
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.ClassroomRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.domain.usecase.StartSessionUseCase
import com.classroom.quizmaster.domain.usecase.SubmitAnswerUseCase
import com.classroom.quizmaster.ui.model.AnswerOptionUi
import com.classroom.quizmaster.ui.model.AvatarOption
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.model.PlayerLobbyUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.model.StatusChipType
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.student.end.StudentEndUiState
import com.classroom.quizmaster.ui.student.entry.StudentEntryUiState
import com.classroom.quizmaster.ui.teacher.host.HostLiveUiState
import com.classroom.quizmaster.ui.teacher.launch.LaunchLobbyUiState
import com.classroom.quizmaster.util.NicknamePolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class RealSessionRepositoryUi @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val quizRepository: QuizRepository,
    private val classroomRepository: ClassroomRepository,
    private val authRepository: AuthRepository,
    private val preferences: AppPreferencesDataSource,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SessionRepositoryUi {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val hostContext = MutableStateFlow<HostContext?>(null)
    private val muteSfx = MutableStateFlow(false)
    private val studentReady = MutableStateFlow(false)
    private val entryState = MutableStateFlow(
        StudentEntryUiState(
            statusMessage = DEFAULT_STATUS_MESSAGE,
            networkAvailable = true
        )
    )
    private val hostStatusMessage = MutableStateFlow<String?>(null)

    private val sessionState = sessionRepository.session
        .stateIn(scope, SharingStarted.Eagerly, null)
    private val participantsState = sessionRepository.participants
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val pendingOps = sessionRepository.pendingOpCount
        .stateIn(scope, SharingStarted.Eagerly, 0)
    private val quizzesState = quizRepository.quizzes
        .stateIn(scope, SharingStarted.Eagerly, emptyList())
    private val authState = authRepository.authState
        .stateIn(scope, SharingStarted.Eagerly, com.classroom.quizmaster.domain.model.AuthState())
    private val preferredNickname = preferences.preferredNickname
        .stateIn(scope, SharingStarted.Eagerly, null)

    private var discoveryJob: Job? = null

    init {
        authState
            .onEach { state -> applyDefaultNickname(state) }
            .launchIn(scope)

        preferredNickname
            .onEach { saved -> applyPreferredNickname(saved) }
            .launchIn(scope)
    }

    override val launchLobby: Flow<LaunchLobbyUiState> =
        combine(sessionState, participantsState, pendingOps, hostStatusMessage) { session, participants, pending, message ->
            val base = if (session == null) {
                LaunchLobbyUiState()
            } else {
                val lanMeta = session.lanMeta
                LaunchLobbyUiState(
                    joinCode = session.joinCode.ifBlank { "----" },
                    qrSubtitle = lanMeta?.let { "${it.hostIp}:${it.port}" }.orEmpty(),
                    qrPayload = lanMeta?.let { "ws://${it.hostIp}:${it.port}/ws?token=${it.token}" }.orEmpty(),
                    discoveredPeers = participants.size,
                    players = participants.map { it.toPlayerLobby(session.teacherId) },
                    hideLeaderboard = session.hideLeaderboard,
                    lockAfterFirst = session.lockAfterQ1,
                    statusChips = buildStatusChips(pending, false)
                )
            }
            if (message.isNullOrBlank()) base else base.copy(snackbarMessage = message)
        }
            .distinctUntilChanged()

    override val hostState: Flow<HostLiveUiState> =
        combine(sessionState, quizzesState, participantsState, muteSfx) { session, quizzes, participants, muted ->
            if (session == null) {
                HostLiveUiState(muteSfx = muted)
            } else {
                val quiz = quizzes.firstOrNull { it.id == session.quizId }
                val totalQuestions = quiz?.questions?.size?.takeIf { it > 0 } ?: quiz?.questionCount ?: 0
                val currentQuestion = quiz?.questions?.getOrNull(session.currentIndex)
                HostLiveUiState(
                    questionIndex = session.currentIndex.coerceAtLeast(0),
                    totalQuestions = totalQuestions,
                    timerSeconds = currentQuestion?.timeLimitSeconds ?: 45,
                    isRevealed = session.reveal,
                    question = currentQuestion?.toDraft(),
                    distribution = emptyList(),
                    leaderboard = participants.sortedByDescending { it.totalPoints }
                        .mapIndexed { index, participant ->
                            participant.toLeaderboardRow(index + 1, authState.value.userId)
                        },
                    muteSfx = muted,
                    showLeaderboard = !session.hideLeaderboard
                )
            }
        }
            .distinctUntilChanged()

    override val studentEntry: Flow<StudentEntryUiState> = entryState.asStateFlow()

    override val studentEnd: Flow<StudentEndUiState> =
        combine(sessionState, participantsState, authState) { session, participants, auth ->
            if (session?.status != SessionStatus.ENDED) {
                StudentEndUiState()
            } else {
                val leaderboard = participants.sortedByDescending { it.totalPoints }
                    .mapIndexed { index, participant ->
                        participant.toLeaderboardRow(index + 1, auth.userId)
                    }
                val currentParticipant = auth.userId?.let { uid ->
                    leaderboard.firstOrNull { it.displayName == participants.firstOrNull { p -> p.uid == uid }?.nickname }
                }
                StudentEndUiState(
                    stars = when (currentParticipant?.rank) {
                        null -> 1
                        in 1..3 -> 3
                        in 4..10 -> 2
                        else -> 1
                    },
                    rank = currentParticipant?.rank ?: leaderboard.size,
                    team = "",
                    badges = buildList {
                        if ((currentParticipant?.rank ?: Int.MAX_VALUE) <= 3) add("Top performer")
                        if ((currentParticipant?.isYou ?: false)) add("Great effort")
                    },
                    totalScore = currentParticipant?.score ?: 0,
                    improvement = 0,
                    leaderboard = leaderboard,
                    summary = "Thanks for playing!"
                )
            }
        }
            .distinctUntilChanged()

    override val avatarOptions: Flow<List<AvatarOption>> = flowOf(emptyList())

    override suspend fun configureHostContext(classroomId: String, topicId: String?, quizId: String?) {
        if (classroomId.isBlank()) {
            failHostContext("Select a classroom to host")
            return
        }
        val auth = authState.value
        val teacherId = auth.teacherProfile?.id ?: auth.userId
        if (teacherId.isNullOrBlank()) {
            failHostContext("Sign in as a teacher to host a quiz")
            return
        }
        val classroom = classroomRepository.getClassroom(classroomId)
        if (classroom == null) {
            failHostContext("Classroom not available")
            return
        }

        var resolvedTopicId: String? = null
        val normalizedTopicId = topicId?.takeIf { it.isNotBlank() }
        if (normalizedTopicId != null) {
            val topic = classroomRepository.getTopic(normalizedTopicId)
            if (topic == null || topic.classroomId != classroom.id) {
                failHostContext("Topic not found in this classroom")
                return
            }
            resolvedTopicId = topic.id
        }

        var resolvedQuizId: String? = null
        val normalizedQuizId = quizId?.takeIf { it.isNotBlank() }
        if (normalizedQuizId != null) {
            val quiz = quizRepository.getQuiz(normalizedQuizId)
            if (quiz == null) {
                failHostContext("Quiz is unavailable")
                return
            }
            if (quiz.classroomId != classroom.id) {
                failHostContext("Quiz does not belong to this classroom")
                return
            }
            if (resolvedTopicId != null && quiz.topicId != resolvedTopicId) {
                failHostContext("Quiz does not belong to this topic")
                return
            }
            resolvedTopicId = resolvedTopicId ?: quiz.topicId
            resolvedQuizId = quiz.id
        }

        hostContext.value = HostContext(
            classroomId = classroom.id,
            topicId = resolvedTopicId,
            quizId = resolvedQuizId
        )
        hostStatusMessage.value = null
        muteSfx.value = false
    }

    override suspend fun updateLeaderboardHidden(hidden: Boolean) {
        val current = sessionState.value ?: return
        sessionRepository.updateSessionState(current.copy(hideLeaderboard = hidden))
    }

    override suspend fun updateLockAfterFirst(lock: Boolean) {
        val current = sessionState.value ?: return
        sessionRepository.updateSessionState(current.copy(lockAfterQ1 = lock))
    }

    override suspend fun updateMuteSfx(muted: Boolean) {
        muteSfx.value = muted
    }

    override suspend fun startSession() {
        val context = hostContext.value ?: return
        val quizId = context.quizId ?: selectQuizForContext(context)
        if (quizId.isNullOrBlank()) {
            failHostContext("Choose a quiz to host")
            return
        }
        val hostName = authState.value.teacherProfile?.displayName
            ?: authState.value.displayName
            ?: "Host"
        runCatching { startSessionUseCase(quizId, context.classroomId, hostName) }
            .onSuccess {
                hostContext.value = context.copy(quizId = quizId)
                hostStatusMessage.value = null
            }
            .onFailure { err -> failHostContext(err.message ?: "Unable to start lobby") }
    }

    override suspend fun endSession() {
        sessionRepository.endSession()
        hostContext.value = null
        hostStatusMessage.value = null
        muteSfx.value = false
        studentReady.value = false
        entryState.update {
            it.copy(
                statusMessage = "Scan for nearby hosts",
                isJoining = false,
                isDiscovering = false
            )
        }
    }

    override suspend fun revealAnswer() {
        val current = sessionState.value ?: return
        sessionRepository.updateSessionState(current.copy(reveal = true))
    }

    override suspend fun nextQuestion() {
        val current = sessionState.value ?: return
        val quiz = quizzesState.value.firstOrNull { it.id == current.quizId }
        val totalQuestions = quiz?.questions?.size?.takeIf { it > 0 } ?: quiz?.questionCount ?: 0
        val nextIndex = (current.currentIndex + 1).coerceAtMost((totalQuestions - 1).coerceAtLeast(0))
        sessionRepository.updateSessionState(current.copy(currentIndex = nextIndex, reveal = false))
    }

    override suspend fun kickParticipant(uid: String) {
        sessionRepository.kickParticipant(uid)
    }

    override suspend fun toggleReady(studentId: String) {
        studentReady.update { !it }
    }

    override suspend fun submitStudentAnswer(questionId: String, answers: List<String>) {
        val studentId = authState.value.userId ?: return
        val session = sessionState.value ?: return
        val now = Clock.System.now()
        submitAnswerUseCase.invoke(session.id, questionId, studentId, answers, now)
    }

    override suspend fun joinWithCode(joinCode: String, nickname: String, avatarId: String?): Result<Unit> = runCatching {
        classroomRepository.createJoinRequest(joinCode)
    }

    override suspend fun updateStudentProfile(nickname: String, avatarId: String?) {
        val sanitized = NicknamePolicy.sanitize(nickname, "Anonymous")
        entryState.update {
            it.copy(
                statusMessage = "Welcome, $sanitized"
            )
        }
    }

    override suspend fun clearStudentError() {
        entryState.update { it.copy(errorMessage = null) }
    }

    private fun applyDefaultNickname(state: com.classroom.quizmaster.domain.model.AuthState) {
        if (state.role != com.classroom.quizmaster.domain.model.UserRole.STUDENT) return
        val candidate = state.displayName?.takeIf { it.isNotBlank() }
        if (!candidate.isNullOrBlank()) {
            applyNicknameIfEmpty(candidate)
        }
    }

    private fun applyPreferredNickname(saved: String?) {
        if (!saved.isNullOrBlank()) {
            applyNicknameIfEmpty(saved)
        }
    }

    private fun applyNicknameIfEmpty(nickname: String) {
        entryState.update { state ->
            if (state.isJoining) {
                state
            } else {
                state.copy(
                    statusMessage = "Welcome back, $nickname"
                )
            }
        }
    }

    private fun selectQuizForContext(context: HostContext): String? {
        val quizzes = quizzesState.value
        return quizzes.firstOrNull { quiz ->
            quiz.classroomId == context.classroomId &&
                (context.topicId == null || quiz.topicId == context.topicId)
        }?.id
    }

    private fun buildStatusChips(pendingOps: Int, offline: Boolean): List<StatusChipUi> {
        val chips = mutableListOf(
            StatusChipUi("lan", "LAN", StatusChipType.Lan)
        )
        if (offline) {
            chips += StatusChipUi("offline", "Offline-first", StatusChipType.Offline)
            if (pendingOps > 0) {
                chips += StatusChipUi("queue", "Queue $pendingOps", StatusChipType.Cloud)
            }
        } else {
            chips += if (pendingOps > 0) {
                StatusChipUi("sync", "Syncing", StatusChipType.Cloud)
            } else {
                StatusChipUi("cloud", "Cloud", StatusChipType.Cloud)
            }
        }
        return chips
    }

    private fun Participant.toPlayerLobby(teacherId: String): PlayerLobbyUi = PlayerLobbyUi(
        id = uid,
        nickname = nickname.ifBlank { "Student" },
        avatar = avatarForNickname(nickname),
        ready = totalPoints > 0,
        tag = when {
            uid == "host" || uid == teacherId -> "Host"
            totalPoints > 0 -> "Playing"
            else -> null
        }
    )

    private fun Participant.toLeaderboardRow(rank: Int, currentUserId: String?): LeaderboardRowUi =
        LeaderboardRowUi(
            rank = rank,
            displayName = nickname.ifBlank { "Student" },
            score = totalPoints,
            delta = 0,
            avatar = avatarForNickname(nickname),
            isYou = uid == currentUserId
        )

    private fun Question.toDraft(): QuestionDraftUi {
        val answerOptions = choices.mapIndexed { index, choice ->
            AnswerOptionUi(
                id = "${id}_$index",
                label = ('A' + index).toString(),
                text = choice,
                correct = answerKey.any { it.equals(choice, ignoreCase = true) }
            )
        }
        val typeUi = when (type) {
            QuestionType.MCQ -> QuestionTypeUi.MultipleChoice
            QuestionType.TF -> QuestionTypeUi.TrueFalse
            QuestionType.FILL_IN -> QuestionTypeUi.FillIn
            QuestionType.MATCHING -> QuestionTypeUi.Match
        }
        val populatedAnswers = answerOptions.ifEmpty {
            defaultAnswersForType(typeUi)
        }
        return QuestionDraftUi(
            id = id,
            stem = stem,
            type = typeUi,
            answers = populatedAnswers,
            explanation = explanation,
            mediaThumb = media?.url,
            timeLimitSeconds = timeLimitSeconds
        )
    }

    private fun defaultAnswersForType(type: QuestionTypeUi): List<AnswerOptionUi> = when (type) {
        QuestionTypeUi.MultipleChoice -> listOf("A", "B", "C", "D")
        QuestionTypeUi.TrueFalse -> listOf("True", "False")
        QuestionTypeUi.FillIn -> listOf("Answer")
        QuestionTypeUi.Match -> listOf("Pair 1", "Pair 2")
    }.mapIndexed { index, label ->
        AnswerOptionUi(
            id = "placeholder_$index",
            label = ('A' + index).toString(),
            text = label,
            correct = index == 0
        )
    }

    private fun requiresManualSubmit(type: QuestionType): Boolean =
        when (type) {
            QuestionType.MCQ, QuestionType.TF -> false
            QuestionType.FILL_IN, QuestionType.MATCHING -> true
        }

    private fun avatarForNickname(name: String): AvatarOption {
        val initials = name.split(" ").filter { it.isNotBlank() }.map { it.first().uppercaseChar() }
        val label = if (initials.isNotEmpty()) initials.joinToString("") else name.take(2).uppercase()
        return AvatarOption(name.ifBlank { label.lowercase() }, label, emptyList(), null)
    }

    private fun failHostContext(message: String) {
        hostContext.value = null
        hostStatusMessage.value = message
    }

    private data class HostContext(
        val classroomId: String,
        val topicId: String?,
        val quizId: String?
    )

    companion object {
        private const val DEFAULT_STATUS_MESSAGE = "Scan for nearby teachers"
    }
}
