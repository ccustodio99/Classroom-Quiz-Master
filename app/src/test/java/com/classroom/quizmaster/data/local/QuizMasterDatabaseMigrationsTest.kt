package com.classroom.quizmaster.data.local

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
    fun `migrates from v1 to v5`() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO sessions (id, quizId, teacherId, classroomId, joinCode, status, currentIndex, reveal, hideLeaderboard, lockAfterQ1, updatedAt) VALUES ('s1','q1','t1','c1','ABC123','LOBBY',0,0,0,0,0)")
            execSQL("INSERT INTO participants (uid, nickname, avatar, totalPoints, totalTimeMs, joinedAt) VALUES ('u1','Demo','avatar',0,0,0)")
            execSQL("INSERT INTO attempts (id, uid, questionId, selectedJson, timeMs, correct, points, late, createdAt) VALUES ('a1','u1','q1','[]',0,0,0,0,0)")
            close()
        }

        helper.runMigrationsAndValidate(dbName, 5, true, *QuizMasterMigrations.ALL)

        helper.openDatabase(dbName, 5).use { db ->
            db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='quizzes'").use { cursor ->
                assertTrue(cursor.moveToFirst())
            }
        }
    }
}
