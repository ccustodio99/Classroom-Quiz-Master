package com.classroom.quizmaster.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.classroom.quizmaster.data.local.entity.AssignmentLocalEntity
import com.classroom.quizmaster.data.local.entity.OpLogEntity
import com.classroom.quizmaster.data.local.entity.ParticipantLocalEntity
import com.classroom.quizmaster.data.local.entity.SessionLocalEntity
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QuizMasterDaoTest {

    private lateinit var database: QuizMasterDatabase
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, QuizMasterDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `sessionDao orders participants by score then time`() = runTest {
        val session = SessionLocalEntity(
            id = "s1",
            quizId = "q1",
            teacherId = "t1",
            classroomId = "c1",
            joinCode = "ABC123",
            status = "LOBBY",
            currentIndex = 0,
            reveal = false,
            hideLeaderboard = false,
            lockAfterQ1 = false,
            startedAt = null,
            endedAt = null,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        val participants = listOf(
            ParticipantLocalEntity("s1", "u1", "Nova", "a", 500, 12_000, 0),
            ParticipantLocalEntity("s1", "u2", "Kai", "b", 600, 15_000, 0),
            ParticipantLocalEntity("s1", "u3", "Luz", "c", 600, 10_000, 0)
        )
        val sessionDao = database.sessionDao()
        sessionDao.replaceSession(session, participants)

        val ordered = sessionDao.listParticipants("s1")
        assertEquals(listOf("u3", "u2", "u1"), ordered.map { it.uid })
    }

    @Test
    fun `attemptDao upsert and find`() = runTest {
        val attemptDao = database.attemptDao()
        val attempt = com.classroom.quizmaster.data.local.entity.AttemptLocalEntity(
            id = "a1",
            sessionId = "s1",
            uid = "u1",
            questionId = "q1",
            selectedJson = "[\"A\"]",
            timeMs = 1_200,
            correct = true,
            points = 900,
            late = false,
            createdAt = 0,
            syncedAt = null,
            sequenceNumber = 0
        )
        attemptDao.upsertAttempt(attempt)
        val loaded = attemptDao.getAttempt("s1", "u1", "q1")
        assertNotNull(loaded)
        assertEquals(900, loaded.points)
    }

    @Test
    fun `assignmentDao observes assignments ordered by openAt`() = runTest {
        val assignmentDao = database.assignmentDao()
        val first = AssignmentLocalEntity(
            id = "a1",
            quizId = "q1",
            classroomId = "c1",
            topicId = "t1",
            openAt = 1_000,
            closeAt = 2_000,
            attemptsAllowed = 2,
            scoringMode = "BEST",
            revealAfterSubmit = true,
            createdAt = 1_000,
            updatedAt = 1_000,
            isArchived = false,
            archivedAt = null
        )
        val second = first.copy(id = "a2", openAt = 2_000, updatedAt = 2_000)
        assignmentDao.upsertAssignments(listOf(first, second))

        val observed = assignmentDao.observeAssignments().first()
        assertEquals(listOf("a2", "a1"), observed.map { it.id })
    }

    @Test
    fun `opLogDao enqueue is idempotent`() = runTest {
        val opLogDao = database.opLogDao()
        val op = OpLogEntity(
            id = "attempt-1",
            type = "attempt",
            payloadJson = "{}",
            ts = 100,
            synced = false,
            retryCount = 0
        )
        opLogDao.enqueue(op)
        opLogDao.enqueue(op.copy(ts = 200))

        val pending = opLogDao.pending()
        assertEquals(1, pending.size)
        assertEquals(0, pending.first().retryCount)
        opLogDao.markSynced(listOf(op.id))
        opLogDao.deleteSynced()
        assertTrue(opLogDao.pending().isEmpty())
    }
}
