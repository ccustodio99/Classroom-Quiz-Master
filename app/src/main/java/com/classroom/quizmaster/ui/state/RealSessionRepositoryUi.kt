package com.classroom.quizmaster.ui.state

import androidx.compose.ui.graphics.Color
import com.classroom.quizmaster.data.lan.LanDiscoveryEvent
import com.classroom.quizmaster.data.lan.LanServiceDescriptor
import com.classroom.quizmaster.data.network.ConnectivityMonitor
import com.classroom.quizmaster.data.network.ConnectivityStatus
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
import com.classroom.quizmaster.ui.model.ConnectionQuality
import com.classroom.quizmaster.ui.model.LeaderboardRowUi
import com.classroom.quizmaster.ui.model.PlayerLobbyUi
import com.classroom.quizmaster.ui.model.QuestionDraftUi
import com.classroom.quizmaster.ui.model.QuestionTypeUi
import com.classroom.quizmaster.ui.model.StatusChipType
import com.classroom.quizmaster.ui.model.StatusChipUi
import com.classroom.quizmaster.ui.student.end.StudentEndUiState
import com.classroom.quizmaster.ui.student.entry.LanHostUi
import com.classroom.quizmaster.ui.student.entry.StudentEntryUiState
import com.classroom.quizmaster.ui.student.lobby.StudentLobbyUiState
import com.classroom.quizmaster.ui.student.play.StudentPlayUiState
import com.classroom.quizmaster.ui.student.play.SubmissionStatus
import com.classroom.quizmaster.ui.teacher.host.HostLiveUiState
import com.classroom.quizmaster.ui.teacher.launch.LaunchLobbyUiState
import javax.inject.Inject
import javax.inject.Singleton
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import com.classroom.quizmaster.util.NicknamePolicy

@Singleton
class RealSessionRepositoryUi @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val quizRepository: QuizRepository,
    private val classroomRepository: ClassroomRepository,
    private val authRepository: AuthRepository,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val startSessionUseCase: StartSessionUseCase,
    private val connectivityMonitor: ConnectivityMonitor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SessionRepositoryUi {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val hostContext = MutableStateFlow<HostContext?>(null)
    private val muteSfx = MutableStateFlow(false)
    private val studentReady = MutableStateFlow(false)
    private val selectedHost = MutableStateFlow<LanHostSnapshot?>(null)
    private val hostDescriptors = MutableStateFlow<Map<String, LanHostSnapshot>>(emptyMap())
    private val entryState = MutableStateFlow(
        StudentEntryUiState(
            avatarOptions = defaultAvatars,
            statusMessage = DEFAULT_STATUS_MESSAGE,
            networkAvailable = true
        )
    )
    private val submissionStatus = MutableStateFlow(SubmissionStatus.Idle to "")
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
    private val connectivityState = connectivityMonitor.status
        .stateIn(scope, SharingStarted.Eagerly, ConnectivityStatus.offline())

    private var discoveryJob: Job? = null

    init {
        connectivityState
            .onEach { status -> updateEntryNetworkStatus(status.isOffline) }
            .launchIn(scope)
    }

    override val launchLobby: Flow<LaunchLobbyUiState> =
        combine(sessionState, participantsState, pendingOps, hostStatusMessage, connectivityState) { session, participants, pending, message, connectivity ->
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
                    statusChips = buildStatusChips(pending, connectivity.isOffline)
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

    override val studentLobby: Flow<StudentLobbyUiState> =
        combine(sessionState, participantsState, studentReady, selectedHost, authState) { session, participants, ready, host, auth ->
            val joinCode = session?.joinCode.orEmpty()
            val hostName = host?.ui?.teacherName
                ?: session?.teacherId?.takeIf { it.isNotBlank() }
                ?: "Host"
            val status = when (session?.status) {
                SessionStatus.ACTIVE -> "Quiz in progress"
                SessionStatus.ENDED -> "Session ended"
                SessionStatus.LOBBY -> "Waiting for host"
                null -> "Searching for lobby"
            }
            val leaderboardPreview = participants.sortedByDescending { it.totalPoints }
                .take(5)
                .mapIndexed { index, participant ->
                    participant.toLeaderboardRow(index + 1, auth.userId)
                }
            StudentLobbyUiState(
                studentId = auth.userId.orEmpty(),
                hostName = hostName,
                joinCode = joinCode.ifBlank { host?.ui?.joinCode.orEmpty() },
                joinStatus = status,
                players = participants.map { it.toPlayerLobby(session?.teacherId.orEmpty()) },
                ready = ready,
                lockedMessage = session?.takeIf { it.lockAfterQ1 }?.let { "Host will lock after question 1" },
                countdownSeconds = 0,
                leaderboardPreview = leaderboardPreview,
                connectionQuality = host?.ui?.quality ?: ConnectionQuality.Fair
            )
        }
            .distinctUntilChanged()

    override val studentPlay: Flow<StudentPlayUiState> =
        combine(sessionState, quizzesState, participantsState, submissionStatus, selectedHost) { session, quizzes, participants, submission, host ->
            if (session == null) {
                StudentPlayUiState(
                    connectionQuality = host?.ui?.quality ?: ConnectionQuality.Fair,
                    submissionStatus = submission.first,
                    submissionMessage = submission.second
                )
            } else {
                val quiz = quizzes.firstOrNull { it.id == session.quizId }
                val currentQuestion = quiz?.questions?.getOrNull(session.currentIndex)
                val totalQuestions = quiz?.questions?.size?.takeIf { it > 0 } ?: quiz?.questionCount ?: 0
                val progress = if (totalQuestions == 0) 0f else (session.currentIndex + 1f) / totalQuestions
                val currentParticipant = authState.value.userId?.let { uid ->
                    participants.firstOrNull { it.uid == uid }
                }
                StudentPlayUiState(
                    question = currentQuestion?.toDraft(),
                    timerSeconds = currentQuestion?.timeLimitSeconds ?: 45,
                    selectedAnswers = emptySet(),
                    reveal = session.reveal,
                    progress = progress.coerceIn(0f, 1f),
                    leaderboard = participants.sortedByDescending { it.totalPoints }
                        .mapIndexed { index, participant ->
                            participant.toLeaderboardRow(index + 1, authState.value.userId)
                        },
                    distribution = emptyList(),
                    streak = 0,
                    totalScore = currentParticipant?.totalPoints ?: 0,
                    latencyMs = host?.ui?.latencyMs ?: 0,
                    connectionQuality = host?.ui?.quality ?: ConnectionQuality.Good,
                    submissionStatus = submission.first,
                    submissionMessage = submission.second,
                    showLeaderboard = !session.hideLeaderboard,
                    requiresManualSubmit = currentQuestion?.let { requiresManualSubmit(it.type) } ?: false
                )
            }
        }
            .distinctUntilChanged()

    override val studentEnd: Flow<StudentEndUiState> =
        combine(sessionState, participantsState, quizzesState, authState) { session, participants, quizzes, auth ->
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

    override suspend fun configureHostContext(classroomId: String, topicId: String?, quizId: String?) {
        if (classroomId.isBlank()) {
            failHostContext("Select a classroom to host")
            return
        }
        val teacherId = authState.value.userId
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
        val quizId = context.quizId ?: selectQuizForContext(context) ?: throw IllegalStateException("Select a quiz before starting")
        val hostName = authState.value.teacherProfile?.displayName
            ?: authState.value.displayName
            ?: "Host"
        startSessionUseCase(quizId, context.classroomId, hostName)
        hostContext.value = context.copy(quizId = quizId)
    }

    override suspend fun endSession() {
        sessionRepository.endSession()
        hostContext.value = null
        hostStatusMessage.value = null
        muteSfx.value = false
        studentReady.value = false
        selectedHost.value = null
        hostDescriptors.value = emptyMap()
        entryState.update {
            it.copy(
                lanHosts = emptyList(),
                selectedHostId = null,
                statusMessage = "Scan for nearby hosts",
                isJoining = false,
                isDiscovering = false
            )
        }
        submissionStatus.value = SubmissionStatus.Idle to ""
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
        submissionStatus.value = SubmissionStatus.Idle to "Waiting for question"
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
                statusMessage = "Scanning for hosts…",
                lanHosts = emptyList(),
                errorMessage = null
            )
        }
        hostDescriptors.value = emptyMap()
        discoveryJob = scope.launch {
            sessionRepository.discoverHosts().collect { event ->
                when (event) {
                    is LanDiscoveryEvent.ServiceFound -> {
                        val snapshot = event.descriptor.toSnapshot()
                        hostDescriptors.update { current -> current + (snapshot.ui.id to snapshot) }
                        entryState.update { state ->
                            state.copy(
                                lanHosts = hostDescriptors.value.values.map { it.ui },
                                lastSeenHosts = "just now",
                                isDiscovering = false,
                                statusMessage = "Hosts updated"
                            )
                        }
                    }

                    is LanDiscoveryEvent.Error -> {
                        entryState.update { state ->
                            state.copy(
                                errorMessage = event.message,
                                isDiscovering = false,
                                statusMessage = "Unable to discover hosts"
                            )
                        }
                    }

                    LanDiscoveryEvent.Timeout -> {
                        entryState.update { state ->
                            state.copy(
                                isDiscovering = false,
                                lastSeenHosts = "moments ago"
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun joinLanHost(hostId: String, nickname: String, avatarId: String?): Result<Unit> {
        val descriptor = hostDescriptors.value[hostId]?.descriptor
            ?: return Result.failure(IllegalArgumentException("Host unavailable"))
        entryState.update { it.copy(isJoining = true, errorMessage = null) }
        val sanitized = NicknamePolicy.sanitize(nickname.ifBlank { "Student" }, descriptor.serviceName)
        val result = sessionRepository.joinLanHost(descriptor, sanitized)
        if (result.isSuccess) {
            selectedHost.value = hostDescriptors.value[hostId]
            entryState.update {
                it.copy(
                    isJoining = false,
                    statusMessage = "Joined ${selectedHost.value?.ui?.teacherName ?: "host"}",
                    selectedHostId = hostId
                )
            }
            studentReady.value = true
        } else {
            entryState.update { it.copy(isJoining = false, errorMessage = result.exceptionOrNull()?.message) }
        }
        return result
    }

    override suspend fun joinWithCode(joinCode: String, nickname: String, avatarId: String?): Result<Unit> {
        val snapshot = hostDescriptors.value.values.firstOrNull { it.descriptor.joinCode.equals(joinCode, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Join code not recognized"))
        return joinLanHost(snapshot.ui.id, nickname, avatarId)
    }

    override suspend fun submitStudentAnswer(answerIds: List<String>) {
        val session = sessionState.value ?: return
        val quiz = quizzesState.value.firstOrNull { it.id == session.quizId } ?: return
        val question = quiz.questions.getOrNull(session.currentIndex) ?: return
        val userId = authState.value.userId ?: "guest-${Clock.System.now().toEpochMilliseconds()}"
        submissionStatus.value = SubmissionStatus.Sending to "Submitting…"
        val result = runCatching {
            submitAnswerUseCase(
                uid = userId,
                questionId = question.id,
                selected = answerIds,
                correctAnswers = question.answerKey,
                timeTakenMs = 0L,
                timeLimitMs = question.timeLimitSeconds.coerceAtLeast(1) * 1_000L,
                nonce = Clock.System.now().toEpochMilliseconds().toString(),
                revealHappened = session.reveal
            )
        }
        if (result.isSuccess) {
            submissionStatus.value = SubmissionStatus.Acknowledged to "Answer received"
        } else {
            submissionStatus.value = SubmissionStatus.Error to (result.exceptionOrNull()?.message ?: "Submission failed")
            throw result.exceptionOrNull() ?: IllegalStateException("Unknown error")
        }
    }

    override suspend fun clearStudentError() {
        entryState.update { it.copy(errorMessage = null) }
    }

    private fun updateEntryNetworkStatus(isOffline: Boolean) {
        entryState.update { state ->
            if (isOffline) {
                state.copy(
                    networkAvailable = false,
                    statusMessage = OFFLINE_STATUS_MESSAGE
                )
            } else {
                val restoredStatus = when (state.statusMessage) {
                    OFFLINE_STATUS_MESSAGE, "" -> DEFAULT_STATUS_MESSAGE
                    else -> state.statusMessage
                }
                state.copy(
                    networkAvailable = true,
                    statusMessage = restoredStatus
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
        val populatedAnswers = if (answerOptions.isNotEmpty()) {
            answerOptions
        } else {
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

    private fun LanServiceDescriptor.toSnapshot(): LanHostSnapshot {
        val label = serviceName.substringBefore('.').ifBlank { serviceName }
        val ui = LanHostUi(
            id = serviceName,
            teacherName = label,
            subject = "Live quiz",
            players = 0,
            latencyMs = 0,
            joinCode = joinCode,
            quality = ConnectionQuality.Good,
            lastSeen = "just now"
        )
        return LanHostSnapshot(descriptor = this, ui = ui, seenAt = Clock.System.now())
    }

    private fun avatarForNickname(name: String): AvatarOption {
        val initials = name.split(" ").filter { it.isNotBlank() }.map { it.first().uppercaseChar() }
        val label = if (initials.isNotEmpty()) initials.joinToString("") else name.take(2).uppercase()
        val palette = defaultAvatars
        val index = kotlin.math.abs(name.hashCode()) % palette.size
        val colors = palette[index].colors.ifEmpty {
            listOf(Color(0xFF6EE7B7), Color(0xFF3B82F6))
        }
        return AvatarOption(name.ifBlank { label.lowercase() }, label, colors, palette[index].iconName)
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

    private data class LanHostSnapshot(
        val descriptor: LanServiceDescriptor,
        val ui: LanHostUi,
        val seenAt: Instant
    )

    companion object {
        private const val DEFAULT_STATUS_MESSAGE = "Scan for nearby hosts"
        private const val OFFLINE_STATUS_MESSAGE = "Offline mode: LAN hosting stays available. We'll sync when you're online."
        private val defaultAvatars = listOf(
            AvatarOption("aurora", "AU", listOf(Color(0xFF34D399), Color(0xFF3B82F6)), "spark"),
            AvatarOption("zen", "ZE", listOf(Color(0xFFFDE68A), Color(0xFFF97316)), "pencil"),
            AvatarOption("luna", "LU", listOf(Color(0xFF818CF8), Color(0xFF3730A3)), "atom"),
            AvatarOption("coco", "CO", listOf(Color(0xFFFECACA), Color(0xFFFB7185)), "flag"),
            AvatarOption("mango", "MA", listOf(Color(0xFFFFF3BF), Color(0xFFF59E0B)), "compass"),
            AvatarOption("iris", "IR", listOf(Color(0xFFBBF7D0), Color(0xFF2DD4BF)), "compass")
        )
    }
}
