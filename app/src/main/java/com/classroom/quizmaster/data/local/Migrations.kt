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

            db.execSQL("ALTER TABLE attempts ADD COLUMN sessionId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE attempts ADD COLUMN syncedAt INTEGER")
            db.execSQL("ALTER TABLE attempts ADD COLUMN sequenceNumber INTEGER NOT NULL DEFAULT 0")
            db.execSQL(
                "UPDATE attempts SET sessionId = (SELECT id FROM sessions ORDER BY updatedAt DESC LIMIT 1) WHERE sessionId = ''"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_sessionId ON attempts(sessionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_questionId ON attempts(questionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_uid ON attempts(uid)")

            db.execSQL("ALTER TABLE oplog ADD COLUMN retryCount INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_teacherId ON sessions(teacherId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sessions_joinCode ON sessions(joinCode)")
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE quizzes ADD COLUMN questionCount INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE assignments ADD COLUMN scoringMode TEXT NOT NULL DEFAULT 'BEST'")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS teachers (id TEXT NOT NULL, displayName TEXT NOT NULL, email TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_teachers_email ON teachers(email)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_teachers_createdAt ON teachers(createdAt)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS classrooms (id TEXT NOT NULL, teacherId TEXT NOT NULL, name TEXT NOT NULL, grade TEXT NOT NULL, subject TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(id))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_classrooms_teacherId ON classrooms(teacherId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_classrooms_teacherId_name ON classrooms(teacherId, name)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_classrooms_createdAt ON classrooms(createdAt)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_classroomId ON sessions(classroomId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_joinCode_status ON sessions(joinCode, status)")

            db.execSQL("DROP INDEX IF EXISTS index_participants_sessionId_totalPoints")
            db.execSQL("DROP INDEX IF EXISTS index_participants_sessionId_totalTimeMs")
            db.execSQL("DROP INDEX IF EXISTS index_participants_session_totalPoints_totalTimeMs")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_participants_sessionId_totalPoints_totalTimeMs ON participants(sessionId, totalPoints, totalTimeMs)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_attempts_sessionId_questionId ON attempts(sessionId, questionId)")

            db.execSQL("CREATE INDEX IF NOT EXISTS index_oplog_synced ON oplog(synced)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_oplog_ts ON oplog(ts)")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE classrooms ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE classrooms SET updatedAt = createdAt WHERE updatedAt = 0")
            db.execSQL("ALTER TABLE classrooms ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE classrooms ADD COLUMN archivedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_classrooms_isArchived ON classrooms(isArchived)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS topics (id TEXT NOT NULL, classroomId TEXT NOT NULL, teacherId TEXT NOT NULL, name TEXT NOT NULL, description TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isArchived INTEGER NOT NULL, archivedAt INTEGER, PRIMARY KEY(id))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_classroomId ON topics(classroomId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_teacherId ON topics(teacherId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_topics_classroomId_name ON topics(classroomId, name)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_topics_isArchived ON topics(isArchived)")

            db.execSQL("ALTER TABLE quizzes ADD COLUMN classroomId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE quizzes ADD COLUMN topicId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE quizzes ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE quizzes ADD COLUMN archivedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_quizzes_classroomId ON quizzes(classroomId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_quizzes_topicId ON quizzes(topicId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_quizzes_isArchived ON quizzes(isArchived)")

            db.execSQL("ALTER TABLE assignments ADD COLUMN topicId TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE assignments ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE assignments ADD COLUMN archivedAt INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assignments_topicId ON assignments(topicId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assignments_isArchived ON assignments(isArchived)")
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE quizzes ADD COLUMN category TEXT NOT NULL DEFAULT 'STANDARD'")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS learning_materials (" +
                    "id TEXT NOT NULL, " +
                    "teacherId TEXT NOT NULL, " +
                    "classroomId TEXT NOT NULL, " +
                    "classroomName TEXT NOT NULL, " +
                    "topicId TEXT NOT NULL, " +
                    "topicName TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "body TEXT NOT NULL, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL, " +
                    "isArchived INTEGER NOT NULL, " +
                    "archivedAt INTEGER, " +
                    "PRIMARY KEY(id)" +
                    ")"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_materials_teacherId ON learning_materials(teacherId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_materials_classroomId ON learning_materials(classroomId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_materials_topicId ON learning_materials(topicId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_learning_materials_isArchived ON learning_materials(isArchived)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS material_attachments (" +
                    "id TEXT NOT NULL, " +
                    "materialId TEXT NOT NULL, " +
                    "displayName TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "uri TEXT NOT NULL, " +
                    "mimeType TEXT, " +
                    "sizeBytes INTEGER NOT NULL, " +
                    "downloadedAt INTEGER, " +
                    "metadataJson TEXT NOT NULL, " +
                    "PRIMARY KEY(id)" +
                    ")"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_material_attachments_materialId ON material_attachments(materialId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_material_attachments_type ON material_attachments(type)")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE classrooms ADD COLUMN students TEXT NOT NULL DEFAULT '[]'")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS students (" +
                    "id TEXT NOT NULL, " +
                    "displayName TEXT NOT NULL, " +
                    "email TEXT NOT NULL, " +
                    "createdAt INTEGER NOT NULL, " +
                    "PRIMARY KEY(id)" +
                    ")"
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_students_email ON students(email)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_students_createdAt ON students(createdAt)")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS join_requests (" +
                    "id TEXT NOT NULL, " +
                    "studentId TEXT NOT NULL, " +
                    "classroomId TEXT NOT NULL, " +
                    "teacherId TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "createdAt INTEGER NOT NULL, " +
                    "resolvedAt INTEGER, " +
                    "PRIMARY KEY(id)" +
                    ")"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_join_requests_studentId ON join_requests(studentId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_join_requests_classroomId ON join_requests(classroomId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_join_requests_teacherId ON join_requests(teacherId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_join_requests_status ON join_requests(status)")
        }
    }

    val ALL = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12
    )
}
