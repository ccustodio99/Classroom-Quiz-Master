package com.classroom.quizmaster.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object QuizMasterMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN hideLeaderboard INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE sessions ADD COLUMN lockAfterQ1 INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN startedAt INTEGER")
            db.execSQL("ALTER TABLE sessions ADD COLUMN endedAt INTEGER")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS quizzes (id TEXT NOT NULL, teacherId TEXT NOT NULL, title TEXT NOT NULL, defaultTimePerQ INTEGER NOT NULL, shuffle INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_quizzes_teacherId ON quizzes(teacherId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_quizzes_createdAt ON quizzes(createdAt)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS questions (id TEXT NOT NULL, quizId TEXT NOT NULL, type TEXT NOT NULL, stem TEXT NOT NULL, choicesJson TEXT NOT NULL, answerKeyJson TEXT NOT NULL, explanation TEXT NOT NULL, mediaType TEXT, mediaUrl TEXT, timeLimitSeconds INTEGER NOT NULL, position INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_questions_quizId ON questions(quizId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_questions_type ON questions(type)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS assignments (id TEXT NOT NULL, quizId TEXT NOT NULL, classroomId TEXT NOT NULL, openAt INTEGER NOT NULL, closeAt INTEGER NOT NULL, attemptsAllowed INTEGER NOT NULL, revealAfterSubmit INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assignments_classroomId ON assignments(classroomId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assignments_quizId ON assignments(quizId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assignments_openAt ON assignments(openAt)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS submissions (assignmentId TEXT NOT NULL, uid TEXT NOT NULL, bestScore INTEGER NOT NULL, lastScore INTEGER NOT NULL, attempts INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(assignmentId, uid))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_submissions_assignment_bestScore ON submissions(assignmentId, bestScore)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_submissions_assignment_updatedAt ON submissions(assignmentId, updatedAt)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS lan_session_meta (sessionId TEXT NOT NULL, token TEXT NOT NULL, hostIp TEXT NOT NULL, port INTEGER NOT NULL, startedAt INTEGER NOT NULL, rotationCount INTEGER NOT NULL, PRIMARY KEY(sessionId))"
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Participants table upgrade to include sessionId composite key
            db.execSQL("ALTER TABLE participants ADD COLUMN sessionId TEXT NOT NULL DEFAULT ''")
            db.execSQL(
                "UPDATE participants SET sessionId = (SELECT id FROM sessions ORDER BY updatedAt DESC LIMIT 1) WHERE sessionId = ''"
            )
            db.execSQL(
                "CREATE TABLE participants_new (sessionId TEXT NOT NULL, uid TEXT NOT NULL, nickname TEXT NOT NULL, avatar TEXT NOT NULL, totalPoints INTEGER NOT NULL, totalTimeMs INTEGER NOT NULL, joinedAt INTEGER NOT NULL, PRIMARY KEY(sessionId, uid))"
            )
            db.execSQL(
                "INSERT INTO participants_new (sessionId, uid, nickname, avatar, totalPoints, totalTimeMs, joinedAt) SELECT sessionId, uid, nickname, avatar, totalPoints, totalTimeMs, joinedAt FROM participants"
            )
            db.execSQL("DROP TABLE participants")
            db.execSQL("ALTER TABLE participants_new RENAME TO participants")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_participants_session_points ON participants(sessionId, totalPoints)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_participants_session_time ON participants(sessionId, totalTimeMs)")

            // Attempts table enrichment
            db.execSQL("ALTER TABLE attempts ADD COLUMN sessionId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE attempts ADD COLUMN syncedAt INTEGER")
            db.execSQL("ALTER TABLE attempts ADD COLUMN sequenceNumber INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "UPDATE attempts SET sessionId = (SELECT id FROM sessions ORDER BY updatedAt DESC LIMIT 1) WHERE sessionId = ''"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_sessionId ON attempts(sessionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_questionId ON attempts(questionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_uid ON attempts(uid)")

            // OpLog retry column
            db.execSQL("ALTER TABLE oplog ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_teacherId ON sessions(teacherId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sessions_joinCode ON sessions(joinCode)")
        }
    }

    val ALL = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
