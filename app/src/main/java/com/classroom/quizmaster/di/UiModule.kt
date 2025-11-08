package com.classroom.quizmaster.di

import com.classroom.quizmaster.ui.preview.FakeAssignmentRepository
import com.classroom.quizmaster.ui.preview.FakeQuizRepository
import com.classroom.quizmaster.ui.preview.FakeSessionRepository
import com.classroom.quizmaster.ui.state.AssignmentRepositoryUi
import com.classroom.quizmaster.ui.state.QuizRepositoryUi
import com.classroom.quizmaster.ui.state.SessionRepositoryUi
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
    fun provideQuizRepository(fake: FakeQuizRepository): QuizRepositoryUi = fake

    @Provides
    @Singleton
    fun provideSessionRepository(fake: FakeSessionRepository): SessionRepositoryUi = fake

    @Provides
    @Singleton
    fun provideAssignmentRepository(fake: FakeAssignmentRepository): AssignmentRepositoryUi = fake
}
