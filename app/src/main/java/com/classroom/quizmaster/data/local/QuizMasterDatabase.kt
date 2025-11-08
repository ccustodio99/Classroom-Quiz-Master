package com.classroom.quizmaster.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.classroom.quizmaster.data.local.converter.Converters
import com.classroom.quizmaster.data.local.dao.AssignmentDao
import com.classroom.quizmaster.data.local.dao.AttemptDao
import com.classroom.quizmaster.data.local.dao.LanSessionDao
import com.classroom.quizmaster.data.local.dao.OpLogDao
import com.classroom.quizmaster.data.local.dao.QuizDao
import com.classroom.quizmaster.data.local.dao.SessionDao
import com.classroom.quizmaster.data.local.entity.AssignmentLocalEntity
import com.classroom.quizmaster.data.local.entity.AttemptLocalEntity
import com.classroom.quizmaster.data.local.entity.LanSessionMetaEntity
import com.classroom.quizmaster.data.local.entity.OpLogEntity
import com.classroom.quizmaster.data.local.entity.ParticipantLocalEntity
import com.classroom.quizmaster.data.local.entity.QuestionEntity
import com.classroom.quizmaster.data.local.entity.QuizEntity
import com.classroom.quizmaster.data.local.entity.SessionLocalEntity
import com.classroom.quizmaster.data.local.entity.SubmissionLocalEntity

@Database(
    entities = [
        QuizEntity::class,
        QuestionEntity::class,
        SessionLocalEntity::class,
        ParticipantLocalEntity::class,
        AttemptLocalEntity::class,
        OpLogEntity::class,
        AssignmentLocalEntity::class,
        SubmissionLocalEntity::class,
        LanSessionMetaEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class QuizMasterDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun opLogDao(): OpLogDao
    abstract fun quizDao(): QuizDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun lanSessionDao(): LanSessionDao
}
