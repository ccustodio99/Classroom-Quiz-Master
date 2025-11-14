package com.classroom.quizmaster.di

import com.classroom.quizmaster.ui.state.AssignmentRepositoryUi
import com.classroom.quizmaster.ui.state.QuizRepositoryUi
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
import com.classroom.quizmaster.ui.state.RealQuizRepositoryUi
import com.classroom.quizmaster.ui.state.RealAssignmentRepositoryUi
import com.classroom.quizmaster.ui.state.RealSessionRepositoryUi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UiModule {

    @Provides
    @Singleton
    fun provideQuizRepository(real: RealQuizRepositoryUi): QuizRepositoryUi = real

    @Provides
    @Singleton
    fun provideSessionRepository(real: RealSessionRepositoryUi): SessionRepositoryUi = real

    @Provides
    @Singleton
    fun provideAssignmentRepository(real: RealAssignmentRepositoryUi): AssignmentRepositoryUi = real
}
