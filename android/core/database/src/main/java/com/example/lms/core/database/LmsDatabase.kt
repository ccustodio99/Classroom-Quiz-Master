package com.example.lms.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lms.core.database.dao.ClassDao
import com.example.lms.core.database.dao.OutboxDao
import com.example.lms.core.database.dao.UserDao
import com.example.lms.core.database.entity.AttemptEntity
import com.example.lms.core.database.entity.ClassEntity
import com.example.lms.core.database.entity.ClassworkEntity
import com.example.lms.core.database.entity.LiveResponseEntity
import com.example.lms.core.database.entity.LiveSessionEntity
import com.example.lms.core.database.entity.OutboxEntity
import com.example.lms.core.database.entity.QuestionEntity
import com.example.lms.core.database.entity.RosterEntity
import com.example.lms.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ClassEntity::class,
        RosterEntity::class,
        ClassworkEntity::class,
        QuestionEntity::class,
        AttemptEntity::class,
        LiveSessionEntity::class,
        LiveResponseEntity::class,
        OutboxEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(StringListConverter::class)
abstract class LmsDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun classDao(): ClassDao
    abstract fun outboxDao(): OutboxDao
}

