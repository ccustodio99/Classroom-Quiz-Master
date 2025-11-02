package com.classroom.quizmaster.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ModuleEntity::class,
        AttemptEntity::class,
        AssignmentEntity::class,
        AccountEntity::class,
        ClassroomEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class QuizMasterDatabase : RoomDatabase() {
    abstract fun moduleDao(): ModuleDao
    abstract fun attemptDao(): AttemptDao
    abstract fun assignmentDao(): AssignmentDao
    abstract fun accountDao(): AccountDao
    abstract fun classroomDao(): ClassroomDao

    companion object {
        fun build(context: Context): QuizMasterDatabase {
            return Room.databaseBuilder(
                context,
                QuizMasterDatabase::class.java,
                "quiz_master.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
