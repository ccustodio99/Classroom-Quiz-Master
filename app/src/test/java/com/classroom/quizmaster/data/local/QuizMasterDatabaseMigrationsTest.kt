package com.classroom.quizmaster.data.local

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.robolectric.annotation.Config
import org.robolectric.junit5.RobolectricExtension

@ExtendWith(RobolectricExtension::class)
@Config(manifest = Config.NONE)
class QuizMasterDatabaseMigrationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        QuizMasterDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )
    private val dbName = "migration-test"

    @AfterEach
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `migrates from v1 to v8 preserving indexes`() {
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                "INSERT INTO sessions (id, quizId, teacherId, classroomId, joinCode, status, currentIndex, reveal, hideLeaderboard, lockAfterQ1, updatedAt) " +
                    "VALUES ('s1','q1','t1','c1','ABC123','LOBBY',0,0,0,0,0)"
            )
            execSQL("INSERT INTO participants (uid, nickname, avatar, totalPoints, totalTimeMs, joinedAt) VALUES ('u1','Demo','a',0,0,0)")
            execSQL("INSERT INTO attempts (id, uid, questionId, selectedJson, timeMs, correct, points, late, createdAt) VALUES ('a1','u1','q1','[]',0,0,0,0,0)")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 8, true, *QuizMasterMigrations.ALL)

        helper.openDatabase(dbName, 8).use { db ->
            val sessionIndexes = mutableSetOf<String>()
            db.query("PRAGMA index_list('sessions')").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    sessionIndexes += cursor.getString(nameIndex)
                }
            }
            assertTrue("index_sessions_joinCode" in sessionIndexes)
            assertTrue("index_sessions_joinCode_status" in sessionIndexes)

            val participantIndexes = mutableSetOf<String>()
            db.query("PRAGMA index_list('participants')").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    participantIndexes += cursor.getString(nameIndex)
                }
            }
            assertTrue(participantIndexes.any { it.contains("totalPoints") })
        }
    }
}
