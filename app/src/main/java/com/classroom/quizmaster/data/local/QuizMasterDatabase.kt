package com.classroom.quizmaster.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.serialization.json.Json

@Database(
    entities = [ModuleEntity::class, AttemptEntity::class, AssignmentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QuizMasterDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun attemptDao(): AttemptDao
    abstract fun assignmentDao(): AssignmentDao

    companion object {
        fun build(context: Context, json: Json): QuizMasterDatabase {
            return Room.databaseBuilder(
                context,
                QuizMasterDatabase::class.java,
                "quiz_master.db"
            )
                .addTypeConverter(JsonConverters(json))
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
