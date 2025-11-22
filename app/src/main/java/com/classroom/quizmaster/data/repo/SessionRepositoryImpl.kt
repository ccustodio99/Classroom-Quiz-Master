package com.classroom.quizmaster.data.repo

import android.content.Context
import androidx.room.withTransaction
import com.classroom.quizmaster.data.lan.LanClient
import com.classroom.quizmaster.data.lan.LanHostForegroundService
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
import com.classroom.quizmaster.data.remote.FirebaseAuthDataSource
import com.classroom.quizmaster.data.remote.FirebaseSessionDataSource
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.LanMeta
import com.classroom.quizmaster.domain.model.LearningMaterial
import com.classroom.quizmaster.domain.model.Participant
import com.classroom.quizmaster.domain.model.Session
import com.classroom.quizmaster.domain.model.SessionStatus
import com.classroom.quizmaster.domain.model.UserRole
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.LearningMaterialRepository
import com.classroom.quizmaster.domain.repository.SessionRepository
import com.classroom.quizmaster.sync.FirestoreSyncWorker
import com.classroom.quizmaster.sync.SyncScheduler
import com.classroom.quizmaster.util.JoinCodeGenerator
import com.classroom.quizmaster.util.NicknamePolicy
import com.classroom.quizmaster.util.ScoreCalculator
import com.classroom.quizmaster.util.switchMapLatest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.util.Locale
import java.util.UUID

@Singleton
class SessionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: QuizMasterDatabase,
    private val firebaseSessionDataSource: FirebaseSessionDataSource,
    private val authDataSource: FirebaseAuthDataSource,
    private val authRepository: AuthRepository,
    private val lanHostServer: LanHostServer,
    private val lanClient: LanClient,
    private val nsdClient: NsdClient,
    private val lanNetworkInfo: LanNetworkInfo,
    private val learningMaterialRepository: LearningMaterialRepository,
    private val json: Json,
    private val syncScheduler: SyncScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SessionRepository {

    private val sessionDao: SessionDao = database.sessionDao()
    private val opLogDao: OpLogDao = database.opLogDao()
    private val lanSessionDao: LanSessionDao = database.lanSessionDao()
    private val attemptDao = database.attemptDao()
    private val quizDao = database.quizDao()
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val lanMetaState = lanSessionDao.observeLatest()
        .map { entity -> entity?.toDomain() }
        .stateIn(repositoryScope, SharingStarted.Eagerly, null)

    private val outboundQueue = MutableSharedFlow<WireMessage>(extraBufferCapacity = 32)

    private var lanToken: String? = null
    private var joinedEndpoint: LanServiceDescriptor? = null
    private var studentNickname: String? = null

    override val lanMeta: Flow<LanMeta?> = lanMetaState

    override val session: Flow<Session?> =
        sessionDao.observeCurrentSession()
            .combine(lanMetaState) { entity, meta -> entity?.toDomain(meta) }
            .distinctUntilChanged()

    override val participants: Flow<List<Participant>> =
        sessionDao.observeCurrentSession()
            .switchMapLatest { entity ->
                if (entity == null) {
                    flowOf(emptyList())
                } else {
                    sessionDao.observeParticipants(entity.id)
                        .map { list ->
                            list.mapIndexed { index, participant ->
                                participant.toDomain(index + 1)
                            }
                        }
                }
            }
            .distinctUntilChanged()

    override val pendingOpCount: Flow<Int> = opLogDao.observePendingCount()

    init {
        lanClient.messages
            .onEach { message ->
                when (message) {
                    is WireMessage.SessionState -> runCatching {
                        val sessionPayload = json.decodeFromString<Session>(message.payload)
                        database.withTransaction {
                            sessionDao.upsertSession(sessionPayload.toEntity())
                        }
                    }.onFailure { Timber.e(it, "Failed to process session state message") }

                    is WireMessage.Leaderboard -> runCatching {
                        val participantsPayload = json.decodeFromString<List<Participant>>(message.leaderboardJson)
                        val currentSession = sessionDao.currentSession() ?: return@runCatching
                        database.withTransaction {
                            sessionDao.upsertParticipants(
                                participantsPayload.map { it.toEntity(currentSession.id) }
                            )
                        }
                    }.onFailure { Timber.e(it, "Failed to process leaderboard message") }

                    is WireMessage.MaterialsSnapshot -> runCatching {
                        val materials = json.decodeFromString<List<LearningMaterial>>(message.payload)
                        learningMaterialRepository.importSnapshot(message.classroomId, materials)
                    }.onFailure { Timber.e(it, "Failed to process materials snapshot") }

                    else -> Unit
                }
            }
            .launchIn(repositoryScope)

        outboundQueue
            .onEach { message ->
                lanToken?.let { token ->
                    runCatching { lanHostServer.broadcast(message) }
                        .onFailure { Timber.w(it, "Failed to broadcast wire message for token $token") }
                }
            }
            .launchIn(repositoryScope)

        lanHostServer.attemptSubmissions
            .onEach { submission ->
                runCatching { handleIncomingAttempt(submission) }
                    .onFailure { Timber.e(it, "Failed to handle LAN attempt") }
            }
            .launchIn(repositoryScope)
    }

    override suspend fun startLanSession(
        quizId: String,
        classroomId: String,
        hostNickname: String
    ): Session = withContext(ioDispatcher) {
        val token = UUID.randomUUID().toString().replace("-", "")
        val currentUid = authDataSource.currentUserId().orEmpty()
        val normalizedHost = NicknamePolicy.sanitize(
            hostNickname.ifBlank { "Host" },
            currentUid
        )
        val now = Clock.System.now()
        val session = Session(
            id = UUID.randomUUID().toString(),
            quizId = quizId,
            classroomId = classroomId,
            joinCode = JoinCodeGenerator.generate(),
            status = SessionStatus.LOBBY,
            currentIndex = 0,
            reveal = false,
            startedAt = now,
            lockAfterQ1 = false,
            hideLeaderboard = false,
            teacherId = currentUid
        )
        val sessionEntity = session.toEntity(now)
        val hostParticipant = ParticipantLocalEntity(
            sessionId = sessionEntity.id,
            uid = "host",
            nickname = normalizedHost,
            avatar = "teacher",
            totalPoints = 0,
            totalTimeMs = 0,
            joinedAt = now.toEpochMilliseconds()
        )
        val port = lanHostServer.start(token)
        val hostIp = lanNetworkInfo.ipv4()
        val lanMeta = LanMeta(
            sessionId = session.id,
            token = token,
            hostIp = hostIp,
            port = port,
            startedAt = now
        )
        database.withTransaction {
            sessionDao.replaceSession(sessionEntity, listOf(hostParticipant))
            lanSessionDao.upsert(lanMeta.toEntity())
        }
        lanToken = token
        val serviceName = serviceNameForHost(normalizedHost, session.joinCode)
        startHostService(session, token, port, serviceName, normalizedHost)
        firebaseSessionDataSource.publishSession(session)
            .onFailure { Timber.w(it, "Failed to mirror session ${session.id} to Firestore") }
        broadcastSession(session)
        broadcastLeaderboardSnapshot(session.id)
        runCatching { learningMaterialRepository.shareSnapshotForClassroom(classroomId) }
            .onFailure { Timber.w(it, "Failed to broadcast materials for $classroomId") }
        session
    }

    override suspend fun updateSessionState(session: Session) = withContext(ioDispatcher) {
        database.withTransaction { sessionDao.upsertSession(session.toEntity()) }
        firebaseSessionDataSource.publishSession(session)
            .onFailure { Timber.w(it, "Failed to publish session state ${session.id}") }
        broadcastSession(session)
    }

    override suspend fun submitAttemptLocally(attempt: Attempt) = withContext(ioDispatcher) {
        val currentSession = sessionDao.currentSession() ?: return@withContext
        val shouldSyncToCloud = isTeacherAccount()
        val opEntry = persistAttempt(currentSession.id, attempt, shouldSyncToCloud)
        if (opEntry != null) {
            triggerImmediateSync()
        }
        joinedEndpoint?.let {
            val senderUid = authDataSource.currentUserId() ?: attempt.uid
            val nickname = NicknamePolicy.sanitize(
                studentNickname ?: "Student",
                senderUid
            )
            repositoryScope.launch {
                val sent = lanClient.sendAttempt(attempt.toWire(json, nickname))
                if (!sent) {
                    Timber.w("Failed to forward attempt %s to host", attempt.id)
                }
            }
        }
        if (shouldSyncToCloud) {
            firebaseSessionDataSource.publishAttempt(currentSession.id, attempt)
                .onSuccess {
                    opEntry?.let { entry ->
                        opLogDao.markSynced(listOf(entry.id))
                        opLogDao.deleteSynced()
                    }
                }
        }
    }

    override suspend fun mirrorAttempt(attempt: Attempt) {
        if (!isTeacherAccount()) return
        sessionDao.currentSession()?.let { session ->
            firebaseSessionDataSource.publishAttempt(session.id, attempt)
        }
    }

    override fun discoverHosts(): Flow<LanDiscoveryEvent> = nsdClient.discover()

    override suspend fun joinLanHost(service: LanServiceDescriptor, nickname: String): Result<Unit> =
        try {
            val uid = authDataSource.currentUserId()
                ?: "guest-${service.joinCode}-${System.currentTimeMillis()}"
            val sanitized = NicknamePolicy.sanitize(nickname.ifBlank { "Student" }, uid)
            joinedEndpoint = service
            studentNickname = sanitized
            lanClient.connect(service, uid)
            Result.success(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }

    override suspend fun kickParticipant(uid: String) {
        withContext(ioDispatcher) {
            lanHostServer.kick(uid)
            sessionDao.currentSession()?.let { current ->
                database.withTransaction { sessionDao.deleteParticipant(current.id, uid) }
                broadcastLeaderboardSnapshot(current.id)
            }
        }
    }

    override suspend fun syncPending() = withContext(ioDispatcher) {
        if (!isTeacherAccount()) return@withContext
        val pending = opLogDao.pending()
        if (pending.isEmpty()) return@withContext
        val synced = mutableListOf<String>()
        pending.forEach { op ->
            when (op.type) {
                OP_TYPE_ATTEMPT -> {
                    val payload = json.decodeFromString<PendingAttemptPayload>(op.payloadJson)
                    val result = firebaseSessionDataSource.publishAttempt(payload.sessionId, payload.attempt)
                    if (result.isSuccess) {
                        synced += op.id
                    } else {
                        opLogDao.incrementRetry(op.id)
                        result.exceptionOrNull()?.let { Timber.w(it, "Attempt sync failed for ${op.id}") }
                    }
                }

                else -> Timber.w("Unknown op type ${op.type}")
            }
        }
        if (synced.isNotEmpty()) {
            opLogDao.markSynced(synced)
            opLogDao.deleteSynced()
        }
    }

    override suspend fun endSession() = withContext(ioDispatcher) {
        val currentEntity = sessionDao.currentSession()
        val endedSnapshot = currentEntity?.let { entity ->
            val now = Clock.System.now()
            val endedSession = entity.toDomain(lanMetaState.value).copy(
                status = SessionStatus.ENDED,
                reveal = false,
                endedAt = now
            )
            firebaseSessionDataSource.publishSession(endedSession)
                .onFailure { Timber.w(it, "Failed to publish ended session ${endedSession.id}") }
            broadcastSession(endedSession)
            endedSession
        }
        lanHostServer.stop()
        lanToken = null
        lanClient.disconnect()
        joinedEndpoint = null
        studentNickname = null
        LanHostForegroundService.stop(context)
        if (endedSnapshot != null) {
            firebaseSessionDataSource.publishParticipants(endedSnapshot.id, emptyList())
                .onFailure { Timber.w(it, "Failed to clear participants for ${endedSnapshot.id}") }
        }
        database.withTransaction {
            lanSessionDao.clear()
            sessionDao.clearParticipants()
            sessionDao.clearSessions()
            attemptDao.clear()
        }
    }

    private suspend fun broadcastSession(session: Session) {
        lanToken?.let {
            outboundQueue.emit(
                WireMessage.SessionState(
                    sessionId = session.id,
                    status = session.status.name.lowercase(),
                    currentIndex = session.currentIndex,
                    reveal = session.reveal,
                    payload = json.encodeToString(session)
                )
            )
        }
    }

    private suspend fun persistAttempt(
        sessionId: String,
        attempt: Attempt,
        shouldSyncToCloud: Boolean
    ): OpLogEntity? {
        var opEntry: OpLogEntity? = null
        database.withTransaction {
            attemptDao.upsertAttempt(attempt.toEntity(json, sessionId))
            if (shouldSyncToCloud) {
                val payload = PendingAttemptPayload(sessionId, attempt)
                opEntry = OpLogEntity(
                    id = attempt.id,
                    type = OP_TYPE_ATTEMPT,
                    payloadJson = json.encodeToString(payload),
                    ts = Clock.System.now().toEpochMilliseconds(),
                    synced = false,
                    retryCount = 0
                )
                opEntry?.let { opLogDao.enqueue(it) }
            }
        }
        return opEntry
    }

    private suspend fun handleIncomingAttempt(message: WireMessage.AttemptSubmit) {
        if (!isTeacherAccount()) return
        val currentSession = sessionDao.currentSession() ?: return
        val attemptId = message.attemptId.ifBlank { return }
        if (attemptDao.findById(attemptId) != null) {
            Timber.d("Attempt %s already processed", attemptId)
            return
        }
        val question = quizDao.getQuestion(message.questionId)
        if (question == null) {
            Timber.w("Question %s not found", message.questionId)
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
        val timeLimitMillis = question.timeLimitSeconds.coerceAtLeast(1) * 1_000L
        val elapsed = message.timeMs.coerceAtLeast(0L)
        val points = ScoreCalculator.score(
            correct = correct,
            timeLeftMillis = (timeLimitMillis - elapsed).coerceAtLeast(0L),
            timeLimitMillis = timeLimitMillis
        )
        val attempt = Attempt(
            id = attemptId,
            uid = message.uid,
            questionId = message.questionId,
            selected = selected,
            timeMs = elapsed,
            correct = correct,
            points = points,
            late = currentSession.reveal,
            createdAt = Clock.System.now()
        )
        submitAttemptLocally(attempt)
        upsertParticipantFromAttempt(currentSession.id, attempt, message.nickname)
        broadcastLeaderboardSnapshot(currentSession.id)
    }

    private suspend fun upsertParticipantFromAttempt(
        sessionId: String,
        attempt: Attempt,
        nicknameRaw: String
    ) {
        val sanitized = NicknamePolicy.sanitize(nicknameRaw.ifBlank { "Student" }, attempt.uid)
        database.withTransaction {
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
    }

    private suspend fun broadcastLeaderboardSnapshot(sessionId: String) {
        val participantsSnapshot = sessionDao.listParticipants(sessionId)
            .mapIndexed { index, entity -> entity.toDomain(index + 1) }
        if (lanToken != null) {
            outboundQueue.emit(
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

    private fun startHostService(
        session: Session,
        token: String,
        port: Int,
        serviceName: String,
        teacherName: String
    ) {
        LanHostForegroundService.start(
            context = context,
            token = token,
            joinCode = session.joinCode,
            serviceName = serviceName,
            port = port,
            teacherName = teacherName
        )
    }

    private fun serviceNameForHost(hostName: String, joinCode: String): String {
        val normalizedHost = hostName.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "quiz" }
        val normalizedJoin = joinCode.uppercase(Locale.US)
        val combined = listOf(normalizedHost.take(SERVICE_HOST_SEGMENT_LIMIT), normalizedJoin)
            .filter { it.isNotBlank() }
            .joinToString("-")
        return combined.take(MAX_SERVICE_NAME_LENGTH)
    }

    private fun triggerImmediateSync() {
        syncScheduler.enqueueNow(
            reason = FirestoreSyncWorker.REASON_QUEUE_FLUSH,
            pendingCountHint = 1
        )
    }

    private fun Session.toEntity(timestamp: Instant = Clock.System.now()): SessionLocalEntity =
        SessionLocalEntity(
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
            updatedAt = timestamp.toEpochMilliseconds()
        )

    private fun SessionLocalEntity.toDomain(meta: LanMeta?): Session = Session(
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

    private fun ParticipantLocalEntity.toDomain(rank: Int): Participant = Participant(
        uid = uid,
        nickname = nickname,
        avatar = avatar,
        totalPoints = totalPoints,
        totalTimeMs = totalTimeMs,
        joinedAt = Instant.fromEpochMilliseconds(joinedAt),
        rank = rank
    )

    private fun Participant.toEntity(sessionId: String): ParticipantLocalEntity = ParticipantLocalEntity(
        sessionId = sessionId,
        uid = uid,
        nickname = nickname,
        avatar = avatar,
        totalPoints = totalPoints,
        totalTimeMs = totalTimeMs,
        joinedAt = joinedAt.toEpochMilliseconds()
    )

    private fun Attempt.toEntity(json: Json, sessionId: String): AttemptLocalEntity = AttemptLocalEntity(
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

    private fun LanMeta.toEntity(): LanSessionMetaEntity = LanSessionMetaEntity(
        sessionId = sessionId,
        token = token,
        hostIp = hostIp,
        port = port,
        startedAt = startedAt.toEpochMilliseconds(),
        rotationCount = 0
    )

    private fun LanSessionMetaEntity.toDomain(): LanMeta = LanMeta(
        sessionId = sessionId,
        token = token,
        hostIp = hostIp,
        port = port,
        startedAt = Instant.fromEpochMilliseconds(startedAt)
    )

    private suspend fun isTeacherAccount(): Boolean =
        authRepository.authState.firstOrNull { it.isAuthenticated }?.role == UserRole.TEACHER

    @Serializable
    private data class PendingAttemptPayload(
        val sessionId: String,
        val attempt: Attempt
    )

    companion object {
        private const val OP_TYPE_ATTEMPT = "attempt"
        private const val MAX_SERVICE_NAME_LENGTH = 63
        private const val SERVICE_HOST_SEGMENT_LIMIT = 32
    }
}
