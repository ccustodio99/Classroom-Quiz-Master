package com.classroom.quizmaster.di

import com.classroom.quizmaster.data.repo.AssignmentRepositoryImpl
import com.classroom.quizmaster.data.repo.AuthRepositoryImpl
import com.classroom.quizmaster.data.repo.QuizRepositoryImpl
import com.classroom.quizmaster.data.repo.SessionRepositoryImpl
import com.classroom.quizmaster.domain.repository.AssignmentRepository
import com.classroom.quizmaster.domain.repository.AuthRepository
import com.classroom.quizmaster.domain.repository.QuizRepository
import com.classroom.quizmaster.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository

    @Binds
    abstract fun bindAssignmentRepository(impl: AssignmentRepositoryImpl): AssignmentRepository

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
