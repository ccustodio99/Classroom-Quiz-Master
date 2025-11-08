package com.classroom.quizmaster.data.repo

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.classroom.quizmaster.BuildConfig
import com.classroom.quizmaster.data.lan.LanClient
import com.classroom.quizmaster.data.lan.LanDiscoveryEvent
import com.classroom.quizmaster.data.lan.LanHostServer
import com.classroom.quizmaster.data.lan.LanNetworkInfo
import com.classroom.quizmaster.data.lan.LanServiceDescriptor
import com.classroom.quizmaster.data.lan.NsdClient
import com.classroom.quizmaster.data.lan.WireMessage
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.dao.LanSessionDao
import com.classroom.quizmaster.data.local.dao.OpLogDao
import com.classroom.quizmaster.data.local.dao.SessionDao
import com.classroom.quizmaster.data.local.entity.AttemptLocalEntity
import com.classroom.quizmaster.data.local.entity.LanSessionMetaEntity
import com.classroom.quizmaster.data.local.entity.OpLogEntity
import com.classroom.quizmaster.data.local.entity.ParticipantLocalEntity
import com.classroom.quizmaster.data.local.entity.SessionLocalEntity
import com.classroom.quizmaster.data.remote.FirebaseSessionDataSource
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.LanMeta
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.model.SessionStatus
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.sync.FirestoreSyncWorker
import com.classroom.quizmaster.util.JoinCodeGenerator
import com.classroom.quizmaster.util.NicknamePolicy
import com.classroom.quizmaster.util.ScoreCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: QuizMasterDatabase,
    private val firebaseSessionDataSource: FirebaseSessionDataSource,
    private val lanHostServer: LanHostServer,
    private val lanClient: LanClient,
    private val nsdClient: NsdClient,
    private val lanNetworkInfo: LanNetworkInfo,
    private val json: Json,
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth
) : SessionRepository {

    private val sessionDao: SessionDao = db.sessionDao()
    private val opLogDao: OpLogDao = db.opLogDao()
    private val lanSessionDao: LanSessionDao = db.lanSessionDao()
    private val attemptDao = db.attemptDao()
    private val quizDao = db.quizDao()
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var lanToken: String? = null
    private var joinedEndpoint: LanServiceDescriptor? = null
    private var studentNickname: String? = null
    private val lanMetaState = MutableStateFlow<LanMeta?>(null)
    override val lanMeta: Flow<LanMeta?> = lanMetaState.asStateFlow()

    override val session: Flow<Session?> =
        sessionDao.observeCurrentSession().combine(lanMetaState) { entity, meta ->
            entity?.toDomain(meta)
        }

    private val participantsState = MutableStateFlow<List<Participant>>(emptyList())
    override val participants: Flow<List<Participant>> = participantsState.asStateFlow()

    override val pendingOpCount = opLogDao.observePendingCount()

    private var participantsJob: Job? = null

    init {
        scope.launch {
            lanClient.messages.collect { message ->
                when (message) {
                    is WireMessage.SessionState -> runCatching {
                        val decoded = json.decodeFromString<Session>(message.payload)
                        sessionDao.upsertSession(decoded.toEntity())
                    }
                    is WireMessage.Leaderboard -> runCatching {
                        val list = json.decodeFromString<List<Participant>>(message.leaderboardJson)
                        val sessionId = sessionDao.currentSession()?.id ?: return@runCatching
                        sessionDao.upsertParticipants(list.map { it.toEntity(sessionId) })
                    }
                    else -> {
                        // No-op for now; additional message types handled elsewhere.
                    }
                }
            }
        }
        scope.launch {
            lanSessionDao.observeLatest().collect { entity ->
                lanMetaState.value = entity?.toDomain()
            }
        }
        scope.launch {
            sessionDao.observeCurrentSession().collect { current ->
                participantsJob?.cancel()
                if (current == null) {
                    participantsState.value = emptyList()
                } else {
                    participantsJob = launch {
                        sessionDao.observeParticipants(current.id).collect { list ->
                            participantsState.value = list.mapIndexed { index, entity -> entity.toDomain(index + 1) }
                        }
                    }
                }
            }
        }
        scope.launch {
            lanHostServer.attemptSubmissions.collect { submission ->
                runCatching { handleIncomingAttempt(submission) }
                    .onFailure { Timber.e(it, "Failed to handle LAN attempt") }
            }
        }
    }

    override suspend fun startLanSession(
        quizId: String,
        classroomId: String,
        hostNickname: String
    ): Session = withContext(Dispatchers.IO) {
        val token = UUID.randomUUID().toString().replace("-", "")
        val normalizedHost = NicknamePolicy.sanitize(
            hostNickname.ifBlank { "Host" },
            firebaseAuth.currentUser?.uid.orEmpty()
        )
        val session = Session(
            id = UUID.randomUUID().toString(),
            quizId = quizId,
            teacherId = firebaseAuth.currentUser?.uid ?: "local-teacher",
            classroomId = classroomId,
            joinCode = JoinCodeGenerator.generate(),
            status = SessionStatus.LOBBY,
            currentIndex = 0,
            reveal = false,
            startedAt = Clock.System.now()
        )
        val entity = session.toEntity()
        sessionDao.replaceSession(
            entity,
            listOf(
                ParticipantLocalEntity(
                    sessionId = entity.id,
                    uid = "host",
                    nickname = normalizedHost,
                    avatar = "teacher",
                    totalPoints = 0,
                    totalTimeMs = 0,
                    joinedAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        )
        lanToken = token
        val boundPort = lanHostServer.start(token)
        startHostService(session, token, boundPort)
        firebaseSessionDataSource.publishSession(session)
        val hostIp = lanNetworkInfo.ipv4()
        lanMetaState.value = LanMeta(
            sessionId = session.id,
            token = token,
            hostIp = hostIp,
            port = boundPort,
            startedAt = Clock.System.now()
        )
        lanSessionDao.upsert(
            LanSessionMetaEntity(
                sessionId = session.id,
                token = token,
                hostIp = hostIp,
                port = boundPort,
                startedAt = lanMetaState.value!!.startedAt.toEpochMilliseconds(),
                rotationCount = 0
            )
        )
        broadcastLeaderboardSnapshot(session.id)
        session
    }

    override suspend fun updateSessionState(session: Session) = withContext(Dispatchers.IO) {
        sessionDao.upsertSession(session.toEntity())
        firebaseSessionDataSource.publishSession(session)
        broadcastSession(session)
    }

    override suspend fun submitAttemptLocally(attempt: Attempt) = withContext(Dispatchers.IO) {
        val currentSession = sessionDao.currentSession() ?: return@withContext
        attemptDao.upsertAttempt(attempt.toEntity(json, currentSession.id))
        val shouldSyncToCloud = isTeacherAccount()
        val op = if (shouldSyncToCloud) {
            val pendingPayload = PendingAttemptPayload(
                sessionId = currentSession.id,
                attempt = attempt
            )
            OpLogEntity(
                id = attempt.id,
                type = OP_TYPE_ATTEMPT,
                payloadJson = json.encodeToString(pendingPayload),
                ts = Clock.System.now().toEpochMilliseconds(),
                synced = false,
                retryCount = 0
            ).also { entry ->
                opLogDao.enqueue(entry)
                triggerImmediateSync()
            }
        } else {
            null
        }
        joinedEndpoint?.let { endpoint ->
            val nickname = NicknamePolicy.sanitize(
                studentNickname ?: "Student",
                firebaseAuth.currentUser?.uid ?: attempt.uid
            )
            scope.launch {
                lanClient.sendAttempt(endpoint, attempt.toWire(json, nickname))
            }
        }
        if (shouldSyncToCloud) {
            firebaseSessionDataSource.publishAttempt(currentSession.id, attempt)
                .onSuccess {
                    op?.let { entry -> opLogDao.markSynced(listOf(entry.id)) }
                    opLogDao.deleteSynced()
                }
        }
    }

    override suspend fun mirrorAttempt(attempt: Attempt) {
        if (!isTeacherAccount()) return
        sessionDao.currentSession()?.let {
            firebaseSessionDataSource.publishAttempt(it.id, attempt)
        }
    }

    override fun discoverHosts(): Flow<LanDiscoveryEvent> =
        nsdClient.discover()

    override suspend fun joinLanHost(service: LanServiceDescriptor, nickname: String): Result<Unit> =
        runCatching {
            val uid = firebaseAuth.currentUser?.uid
                ?: "guest-${service.joinCode}-${System.currentTimeMillis()}"
            val sanitized = NicknamePolicy.sanitize(nickname.ifBlank { "Student" }, uid)
            joinedEndpoint = service
            studentNickname = sanitized
            lanClient.connect(service, uid)
        }

    override suspend fun kickParticipant(uid: String) {
        withContext(Dispatchers.IO) {
            lanHostServer.kick(uid)
            sessionDao.currentSession()?.let { session ->
                sessionDao.deleteParticipant(session.id, uid)
                broadcastLeaderboardSnapshot(session.id)
            }
        }
    }

    override suspend fun syncPending() = withContext(Dispatchers.IO) {
        if (!isTeacherAccount()) return@withContext
        val pending = opLogDao.pending()
        if (pending.isEmpty()) return@withContext
        val syncedIds = mutableListOf<String>()
        pending.forEach { op ->
            when (op.type) {
                OP_TYPE_ATTEMPT -> {
                    val payload = json.decodeFromString<PendingAttemptPayload>(op.payloadJson)
                    val result = firebaseSessionDataSource.publishAttempt(payload.sessionId, payload.attempt)
                    if (result.isSuccess) {
                        syncedIds += op.id
                    } else {
                        result.exceptionOrNull()?.let { Timber.w(it, "Attempt sync failed") }
                        opLogDao.incrementRetry(op.id)
                    }
                }

                else -> Timber.w("Unknown op type ${op.type}")
            }
        }
        if (syncedIds.isNotEmpty()) {
            opLogDao.markSynced(syncedIds)
            opLogDao.deleteSynced()
        }
    }

    override suspend fun endSession() = withContext(Dispatchers.IO) {
        lanHostServer.stop()
        lanToken = null
        lanMetaState.value = null
        lanClient.disconnect()
        joinedEndpoint = null
        studentNickname = null
        lanSessionDao.clear()
        context.stopService(
            Intent(context, com.classroom.quizmaster.lan.QuizMasterLanHostService::class.java).apply {
                action = com.classroom.quizmaster.lan.QuizMasterLanHostService.ACTION_STOP
            }
        )
        sessionDao.clearSessions()
        sessionDao.clearParticipants()
        db.attemptDao().clear()
    }

    private suspend fun broadcastSession(session: Session) {
        lanToken?.let {
            val payload = json.encodeToString(session)
            lanHostServer.broadcast(
                WireMessage.SessionState(
                    sessionId = session.id,
                    status = session.status.name.lowercase(),
                    currentIndex = session.currentIndex,
                    reveal = session.reveal,
                    payload = payload
                )
            )
        }
    }

    private fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request: OneTimeWorkRequest = OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            "${FirestoreSyncWorker.UNIQUE_NAME}_now",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun Session.toEntity() = SessionLocalEntity(
        id = id,
        quizId = quizId,
        teacherId = teacherId,
        classroomId = classroomId,
        joinCode = joinCode,
        status = status.name,
        currentIndex = currentIndex,
        reveal = reveal,
        hideLeaderboard = hideLeaderboard,
        lockAfterQ1 = lockAfterQ1,
        startedAt = startedAt?.toEpochMilliseconds(),
        endedAt = endedAt?.toEpochMilliseconds(),
        updatedAt = Clock.System.now().toEpochMilliseconds()
    )

    private fun SessionLocalEntity.toDomain(meta: LanMeta?) = Session(
        id = id,
        quizId = quizId,
        classroomId = classroomId,
        joinCode = joinCode,
        status = SessionStatus.valueOf(status),
        currentIndex = currentIndex,
        reveal = reveal,
        startedAt = startedAt?.let(Instant::fromEpochMilliseconds),
        endedAt = endedAt?.let(Instant::fromEpochMilliseconds),
        teacherId = teacherId,
        hideLeaderboard = hideLeaderboard,
        lockAfterQ1 = lockAfterQ1,
        lanMeta = meta
    )

    private fun ParticipantLocalEntity.toDomain(rank: Int) = Participant(
        uid = uid,
        nickname = nickname,
        avatar = avatar,
        totalPoints = totalPoints,
        totalTimeMs = totalTimeMs,
        joinedAt = Instant.fromEpochMilliseconds(joinedAt),
        rank = rank
    )

    private fun Participant.toEntity(sessionId: String) = ParticipantLocalEntity(
        sessionId = sessionId,
        uid = uid,
        nickname = nickname,
        avatar = avatar,
        totalPoints = totalPoints,
        totalTimeMs = totalTimeMs,
        joinedAt = joinedAt.toEpochMilliseconds()
    )

    private fun Attempt.toEntity(json: Json, sessionId: String) = AttemptLocalEntity(
        id = id,
        sessionId = sessionId,
        uid = uid,
        questionId = questionId,
        selectedJson = json.encodeToString(selected),
        timeMs = timeMs,
        correct = correct,
        points = points,
        late = late,
        createdAt = createdAt.toEpochMilliseconds(),
        syncedAt = null,
        sequenceNumber = createdAt.toEpochMilliseconds()
    )

    private suspend fun handleIncomingAttempt(message: WireMessage.AttemptSubmit) {
        if (!isTeacherAccount()) return
        val session = sessionDao.currentSession() ?: return
        val sessionStatus = runCatching { SessionStatus.valueOf(session.status) }
            .getOrElse { SessionStatus.LOBBY }
        val attemptId = message.attemptId.ifBlank { return }
        val alreadyProcessed = attemptDao.findById(attemptId)
        if (alreadyProcessed != null) {
            Timber.d("Attempt %s already processed", message.attemptId)
            return
        }
        val question = quizDao.getQuestion(message.questionId)
        if (question == null) {
            Timber.w("Question %s not found for attempt %s", message.questionId, message.attemptId)
            return
        }
        val selected = runCatching {
            json.decodeFromString<List<String>>(message.selectedJson)
        }.getOrElse {
            Timber.e(it, "Failed to decode attempt payload")
            return
        }
        val answerKey = runCatching {
            json.decodeFromString<List<String>>(question.answerKeyJson)
        }.getOrElse {
            Timber.e(it, "Failed to decode answer key")
            return
        }
        val normalizedSelected = selected.map(String::lowercase).sorted()
        val normalizedAnswers = answerKey.map(String::lowercase).sorted()
        val correct = normalizedSelected == normalizedAnswers
        val timeLimitMs = question.timeLimitSeconds.coerceAtLeast(1) * 1_000L
        val elapsed = message.timeMs.coerceAtLeast(0L)
        val points = ScoreCalculator.score(
            correct = correct,
            timeLeftMillis = (timeLimitMs - elapsed).coerceAtLeast(0L),
            timeLimitMillis = timeLimitMs
        )
        val attempt = Attempt(
            id = attemptId,
            uid = message.uid,
            questionId = message.questionId,
            selected = selected,
            timeMs = elapsed,
            correct = correct,
            points = points,
            late = session.reveal || sessionStatus != SessionStatus.ACTIVE,
            createdAt = Clock.System.now()
        )
        submitAttemptLocally(attempt)
        upsertParticipantFromAttempt(session.id, attempt, message.nickname)
        broadcastLeaderboardSnapshot(session.id)
    }

    private suspend fun upsertParticipantFromAttempt(
        sessionId: String,
        attempt: Attempt,
        nicknameRaw: String
    ) {
        val sanitized = NicknamePolicy.sanitize(nicknameRaw.ifBlank { "Student" }, attempt.uid)
        val existing = sessionDao.getParticipant(sessionId, attempt.uid)
        val now = Clock.System.now().toEpochMilliseconds()
        val updated = if (existing == null) {
            ParticipantLocalEntity(
                sessionId = sessionId,
                uid = attempt.uid,
                nickname = sanitized,
                avatar = "student",
                totalPoints = attempt.points,
                totalTimeMs = attempt.timeMs,
                joinedAt = now
            )
        } else {
            existing.copy(
                nickname = sanitized,
                totalPoints = existing.totalPoints + attempt.points,
                totalTimeMs = existing.totalTimeMs + attempt.timeMs
            )
        }
        sessionDao.upsertParticipant(updated)
    }

    private suspend fun broadcastLeaderboardSnapshot(sessionId: String) {
        val participantsSnapshot = sessionDao.listParticipants(sessionId)
            .mapIndexed { index, entity -> entity.toDomain(index + 1) }
        if (lanToken != null) {
            lanHostServer.broadcast(
                WireMessage.Leaderboard(
                    leaderboardJson = json.encodeToString(participantsSnapshot)
                )
            )
        }
        firebaseSessionDataSource.publishParticipants(sessionId, participantsSnapshot)
            .onFailure { Timber.w(it, "Failed to publish participant snapshot") }
    }

    private fun Attempt.toWire(json: Json, nickname: String) = WireMessage.AttemptSubmit(
        attemptId = id,
        uid = uid,
        questionId = questionId,
        selectedJson = json.encodeToString(selected),
        nickname = nickname,
        timeMs = timeMs,
        nonce = id
    )

    private fun startHostService(session: Session, token: String, port: Int) {
        val intent = Intent(context, com.classroom.quizmaster.lan.QuizMasterLanHostService::class.java).apply {
            putExtra(com.classroom.quizmaster.lan.QuizMasterLanHostService.EXTRA_TOKEN, token)
            putExtra(com.classroom.quizmaster.lan.QuizMasterLanHostService.EXTRA_SERVICE_NAME, "Quiz-${session.joinCode}")
            putExtra(com.classroom.quizmaster.lan.QuizMasterLanHostService.EXTRA_JOIN_CODE, session.joinCode)
            putExtra(com.classroom.quizmaster.lan.QuizMasterLanHostService.EXTRA_PORT, port)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    @Serializable
    private data class PendingAttemptPayload(
        val sessionId: String,
        val attempt: Attempt
    )

    companion object {
        private const val OP_TYPE_ATTEMPT = "attempt"
    }

    private fun isTeacherAccount(): Boolean = firebaseAuth.currentUser?.isAnonymous != true

    private fun LanSessionMetaEntity.toDomain() = LanMeta(
        sessionId = sessionId,
        token = token,
        hostIp = hostIp,
        port = port,
        startedAt = Instant.fromEpochMilliseconds(startedAt)
    )
}





