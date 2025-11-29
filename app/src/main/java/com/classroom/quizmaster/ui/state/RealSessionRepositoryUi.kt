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
import com.classroom.quizmaster.data.lan.LanDiscoveryEvent
import com.classroom.quizmaster.data.lan.LanServiceDescriptor
import com.classroom.quizmaster.config.FeatureToggles
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
import com.classroom.quizmaster.ui.student.lobby.StudentLobbyUiState
import com.classroom.quizmaster.ui.student.play.StudentPlayUiState
import com.classroom.quizmaster.ui.student.play.SubmissionStatus
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton
import com.classroom.quizmaster.ui.model.defaultAvatarOptions


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
    private val discoveredHosts = MutableStateFlow<Map<String, LanServiceDescriptor>>(emptyMap())
    private val teacherName = MutableStateFlow("Host")
    private var discoveryJob: Job? = null
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
    private val remainingSeconds = MutableStateFlow(0)
    private var countdownJob: Job? = null
    private var lastQuestionId: String? = null

    init {
        authState
            .onEach { state -> applyDefaultNickname(state) }
            .launchIn(scope)

        preferredNickname
            .onEach { saved -> applyPreferredNickname(saved) }
            .launchIn(scope)

        combine(sessionState, quizzesState) { session, quizzes -> session to quizzes }
            .onEach { (session, quizzes) ->
                syncTimer(session, quizzes)
                session?.teacherId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { id ->
                        scope.launch {
                            authRepository.getTeacher(id).firstOrNull()?.let { teacher ->
                                teacherName.value = teacher.displayName
                            }
                        }
                    }
            }
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
                    ?: quiz?.questions?.lastOrNull()
                val lanEndpoint = session.lanMeta?.let { meta ->
                    listOfNotNull(meta.hostIp.takeIf { it.isNotBlank() }, meta.port.takeIf { it > 0 }?.toString())
                        .joinToString(":")
                }.orEmpty()
                val timeLimit = currentQuestion?.timeLimitSeconds ?: 45
                HostLiveUiState(
                    questionIndex = session.currentIndex.coerceAtLeast(0),
                    totalQuestions = totalQuestions,
                    timerSeconds = remainingSeconds.value.coerceAtMost(timeLimit),
                    isRevealed = session.reveal,
                    question = currentQuestion?.toDraft(),
                    distribution = emptyList(),
                    leaderboard = participants.sortedByDescending { it.totalPoints }
                        .mapIndexed { index, participant ->
                            participant.toLeaderboardRow(index + 1, authState.value.userId)
                        },
                    muteSfx = muted,
                    showLeaderboard = !session.hideLeaderboard,
                    joinCode = session.joinCode,
                    lanEndpoint = lanEndpoint
                )
            }
        }
            .distinctUntilChanged()

    override val studentEntry: Flow<StudentEntryUiState> = entryState.asStateFlow()

    override val studentLobby: Flow<StudentLobbyUiState> =
        combine(sessionState, participantsState, authState, studentReady) { session, participants, auth, ready ->
            if (session == null) {
                StudentLobbyUiState()
            } else {
                val youId = auth.userId
                val sortedParticipants = participants.sortedByDescending { it.totalPoints }
                val playerCards = sortedParticipants.mapIndexed { index, participant ->
                    val avatar = defaultAvatarOptions.firstOrNull { it.id == participant.avatar }
                        ?: defaultAvatarOptions.first()
                    PlayerLobbyUi(
                        id = participant.uid,
                        nickname = participant.nickname,
                        avatar = avatar,
                        ready = participant.uid == youId && ready,
                        tag = when {
                            participant.uid == youId -> "You"
                            index == 0 -> "Leader"
                            else -> null
                        }
                    )
                }
                StudentLobbyUiState(
                    studentId = youId.orEmpty(),
                    hostName = teacherName.value,
                    joinCode = session.joinCode,
                    joinStatus = when (session.status) {
                        SessionStatus.LOBBY -> "Waiting for host"
                        SessionStatus.ACTIVE -> "Live"
                        SessionStatus.ENDED -> "Finished"
                    },
                    players = playerCards,
                    ready = ready,
                    lockedMessage = if (session.lockAfterQ1) "Host will lock after question 1" else null,
                    countdownSeconds = 0,
                    leaderboardPreview = sortedParticipants.take(3).mapIndexed { index, participant ->
                        participant.toLeaderboardRow(index + 1, youId)
                    }
                )
            }
        }
            .distinctUntilChanged()

    override val studentPlay: Flow<StudentPlayUiState> =
        combine(sessionState, quizzesState, participantsState, authState, pendingOps) { session, quizzes, participants, auth, pending ->
            if (session == null) {
                StudentPlayUiState()
            } else {
                val quiz = quizzes.firstOrNull { it.id == session.quizId }
                val question = quiz?.questions?.getOrNull(session.currentIndex)
                    ?: quiz?.questions?.lastOrNull()
                val timeLimit = question?.timeLimitSeconds ?: 30
                val youId = auth.userId
                val leaderboard = participants.sortedByDescending { it.totalPoints }
                    .mapIndexed { index, participant -> participant.toLeaderboardRow(index + 1, youId) }
                val player = participants.firstOrNull { it.uid == youId }
                val requiresManualSubmit = when (question?.type) {
                    QuestionType.FILL_IN, QuestionType.MATCHING -> true
                    else -> false
                }
                StudentPlayUiState(
                    question = question?.toDraft(),
                    timerSeconds = remainingSeconds.value.coerceAtMost(timeLimit),
                    reveal = session.reveal,
                    progress = if (question == null || timeLimit <= 0) 0f else remainingSeconds.value.toFloat() / timeLimit.toFloat(),
                    leaderboard = leaderboard,
                    distribution = emptyList(),
                    streak = player?.rank ?: 0,
                    totalScore = player?.totalPoints ?: 0,
                    latencyMs = (pending * 50).coerceAtMost(500),
                    connectionQuality = when {
                        pending == 0 -> com.classroom.quizmaster.ui.model.ConnectionQuality.Excellent
                        pending < 3 -> com.classroom.quizmaster.ui.model.ConnectionQuality.Good
                        pending < 6 -> com.classroom.quizmaster.ui.model.ConnectionQuality.Fair
                        else -> com.classroom.quizmaster.ui.model.ConnectionQuality.Weak
                    },
                    submissionStatus = if (pending == 0) SubmissionStatus.Idle else SubmissionStatus.Sending,
                    submissionMessage = if (pending == 0) "" else "Syncing...",
                    requiresManualSubmit = requiresManualSubmit
                )
            }
        }
            .distinctUntilChanged()

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

    override val avatarOptions: Flow<List<AvatarOption>> = flowOf(defaultAvatarOptions)

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

    override suspend fun refreshLanHosts() {
        discoveryJob?.cancel()
        entryState.update {
            it.copy(
                isDiscovering = true,
                statusMessage = "Scanning for nearby hosts",
                errorMessage = null
            )
        }
        discoveryJob = scope.launch {
            sessionRepository.discoverHosts().collect { event ->
                when (event) {
                    is LanDiscoveryEvent.ServiceFound -> {
                        val descriptor = event.descriptor
                        discoveredHosts.update { hosts ->
                            hosts + mapOf(
                                descriptor.serviceName to descriptor,
                                descriptor.token to descriptor,
                                descriptor.joinCode to descriptor
                            ).filterKeys { it.isNotBlank() }
                        }
                        entryState.update {
                            it.copy(
                                isDiscovering = false,
                                statusMessage = "Found ${descriptor.joinCode.ifBlank { descriptor.serviceName }}",
                                lastSeenHosts = "just now",
                                networkAvailable = true
                            )
                        }
                    }

                    is LanDiscoveryEvent.Error -> entryState.update {
                        it.copy(
                            isDiscovering = false,
                            errorMessage = event.message,
                            networkAvailable = false
                        )
                    }

                    LanDiscoveryEvent.Timeout -> entryState.update {
                        it.copy(
                            isDiscovering = false,
                            statusMessage = "No hosts detected",
                            lastSeenHosts = "just now"
                        )
                    }
                }
            }
        }
    }

    override suspend fun joinLanHost(hostId: String, nickname: String, avatarId: String?): Result<Unit> {
        val descriptor = discoveredHosts.value.entries.firstOrNull { (key, _) ->
            key.equals(hostId, ignoreCase = true)
        }?.value ?: return Result.failure(IllegalStateException("Host not found; refresh and try again"))

        entryState.update { it.copy(isJoining = true, errorMessage = null) }
        val sanitized = NicknamePolicy.sanitize(nickname.ifBlank { "Student" }, descriptor.joinCode)
        val result = sessionRepository.joinLanHost(descriptor, sanitized)
        entryState.update { state ->
            state.copy(
                isJoining = false,
                statusMessage = result.fold(
                    onSuccess = { "Joined ${descriptor.joinCode.ifBlank { "host" }}" },
                    onFailure = { state.statusMessage }
                ),
                errorMessage = result.exceptionOrNull()?.message
            )
        }
        return result
    }

    override suspend fun submitStudentAnswer(answerIds: List<String>) {
        val studentId = authState.value.userId ?: return
        val session = sessionState.value ?: return
        val question = quizzesState.value
            .firstOrNull { it.id == session.quizId }
            ?.questions
            ?.getOrNull(session.currentIndex)
            ?: return

        // Answers in the UI are identified by "questionId_index" but scoring expects the
        // actual choice text. Normalize the submitted ids back to their choice strings
        // so they can be compared to the question answer key.
        val selectedChoices = answerIds.mapNotNull { answerId ->
            val index = answerId.substringAfterLast('_').toIntOrNull() ?: return@mapNotNull null
            question.choices.getOrNull(index)
        }

        submitAnswerUseCase(
            uid = studentId,
            questionId = question.id,
            selected = selectedChoices,
            correctAnswers = question.answerKey,
            timeTakenMs = 0L, // This should be calculated
            timeLimitMs = question.timeLimitSeconds * 1000L,
            nonce = "",
            revealHappened = session.reveal
        )
    }

    override suspend fun joinWithCode(joinCode: String, nickname: String, avatarId: String?): Result<Unit> = runCatching {
        val sanitized = NicknamePolicy.sanitize(
            nickname.ifBlank { authState.value.displayName ?: "Student" },
            joinCode
        )
        val descriptor = withTimeoutOrNull(5_000) {
            sessionRepository.discoverHosts()
                .filterIsInstance<com.classroom.quizmaster.data.lan.LanDiscoveryEvent.ServiceFound>()
                .firstOrNull { it.descriptor.joinCode.equals(joinCode, ignoreCase = true) }
                ?.descriptor
        } ?: error("Session not found on this network")
        sessionRepository.joinLanHost(descriptor, sanitized).getOrThrow()
    }

    override suspend fun syncSession() {
        sessionRepository.refreshCurrentSession()
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
            entryState.update { it.copy(statusMessage = "Welcome, $candidate") }
        }
    }

    private fun applyPreferredNickname(saved: String?) {
        if (!saved.isNullOrBlank()) {
            entryState.update { it.copy(statusMessage = "Welcome back, $saved") }
        }
    }

    private fun selectQuizForContext(context: HostContext): String? {
        val quizzes = quizzesState.value
        return quizzes.firstOrNull { quiz ->
            quiz.classroomId == context.classroomId &&
                (context.topicId == null || quiz.topicId == context.topicId)
        }?.id
    }

    private fun syncTimer(
        session: com.classroom.quizmaster.domain.model.Session?,
        quizzes: List<com.classroom.quizmaster.domain.model.Quiz>
    ) {
        if (session == null) {
            countdownJob?.cancel()
            countdownJob = null
            lastQuestionId = null
            remainingSeconds.value = 0
            return
        }
        val quiz = quizzes.firstOrNull { it.id == session.quizId }
        val currentQuestion = quiz?.questions?.getOrNull(session.currentIndex)
            ?: quiz?.questions?.lastOrNull()
        val questionId = currentQuestion?.id
        val timeLimit = currentQuestion?.timeLimitSeconds ?: 0
        val reveal = session.reveal
        if (questionId == null || timeLimit <= 0) {
            countdownJob?.cancel()
            countdownJob = null
            lastQuestionId = null
            remainingSeconds.value = 0
            return
        }
        if (questionId != lastQuestionId || reveal) {
            lastQuestionId = questionId
            countdownJob?.cancel()
            countdownJob = null
            remainingSeconds.value = if (reveal) 0 else timeLimit
        }
        if (countdownJob == null && !reveal) {
            startCountdown(timeLimit)
        }
    }

    private fun startCountdown(timeLimit: Int) {
        countdownJob?.cancel()
        remainingSeconds.value = timeLimit
        countdownJob = scope.launch {
            while (remainingSeconds.value > 0) {
                delay(1_000)
                remainingSeconds.update { current ->
                    val next = (current - 1).coerceAtLeast(0)
                    next
                }
            }
        }
    }

    private fun buildStatusChips(pendingOps: Int, offline: Boolean): List<StatusChipUi> {
        val chips = mutableListOf<StatusChipUi>()
        if (FeatureToggles.LIVE_ENABLED) {
            chips += StatusChipUi("local", "Local network", StatusChipType.Lan)
        }
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
        nickname = nickname.ifBlank { authState.value.displayName ?: "Student" },
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
            id = "auto_choice_$index",
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
        return AvatarOption(name.ifBlank { label.lowercase() }, label, emptyList(), "")
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
