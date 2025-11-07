package com.classroom.quizmaster.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.classroom.quizmaster.data.local.converter.Converters
import com.classroom.quizmaster.data.local.dao.AttemptDao
import com.classroom.quizmaster.data.local.dao.OpLogDao
import com.classroom.quizmaster.data.local.dao.SessionDao
import com.classroom.quizmaster.data.local.entity.AttemptLocalEntity
import com.classroom.quizmaster.data.local.entity.OpLogEntity
import com.classroom.quizmaster.data.local.entity.ParticipantLocalEntity
import com.classroom.quizmaster.data.local.entity.SessionLocalEntity

@Database(
    entities = [
        SessionLocalEntity::class,
        ParticipantLocalEntity::class,
        AttemptLocalEntity::class,
        OpLogEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class QuizMasterDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun attemptDao(): AttemptDao
    abstract fun opLogDao(): OpLogDao
}
