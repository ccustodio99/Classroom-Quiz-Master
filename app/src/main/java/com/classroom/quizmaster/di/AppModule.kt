package com.classroom.quizmaster.di

import android.content.Context
import androidx.room.Room
import com.classroom.quizmaster.data.local.QuizMasterDatabase
import com.classroom.quizmaster.data.local.QuizMasterMigrations
import com.classroom.quizmaster.data.local.dao.AssignmentDao
import com.classroom.quizmaster.data.local.dao.AttemptDao
import com.classroom.quizmaster.data.local.dao.ClassroomDao
import com.classroom.quizmaster.data.local.dao.LanSessionDao
import com.classroom.quizmaster.data.local.dao.OpLogDao
import com.classroom.quizmaster.data.local.dao.QuizDao
import com.classroom.quizmaster.data.local.dao.SessionDao
import com.classroom.quizmaster.data.local.dao.TopicDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QuizMasterDatabase {
        return Room.databaseBuilder(context, QuizMasterDatabase::class.java, "quizmaster.db")
            .addMigrations(*QuizMasterMigrations.ALL)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideOpLogDao(database: QuizMasterDatabase): OpLogDao = database.opLogDao()

    @Provides
    @Singleton
    fun provideClassroomDao(database: QuizMasterDatabase): ClassroomDao = database.classroomDao()

    @Provides
    @Singleton
    fun provideTopicDao(database: QuizMasterDatabase): TopicDao = database.topicDao()

    @Provides
    @Singleton
    fun provideQuizDao(database: QuizMasterDatabase): QuizDao = database.quizDao()

    @Provides
    @Singleton
    fun provideSessionDao(database: QuizMasterDatabase): SessionDao = database.sessionDao()

    @Provides
    @Singleton
    fun provideAttemptDao(database: QuizMasterDatabase): AttemptDao = database.attemptDao()

    @Provides
    @Singleton
    fun provideAssignmentDao(database: QuizMasterDatabase): AssignmentDao = database.assignmentDao()

    @Provides
    @Singleton
    fun provideLanSessionDao(database: QuizMasterDatabase): LanSessionDao = database.lanSessionDao()
}
