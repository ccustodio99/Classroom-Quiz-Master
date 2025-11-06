package com.classroom.quizmaster.agents.di

import com.classroom.quizmaster.data.repo.AnalyticsRepo
import com.classroom.quizmaster.data.repo.ClassRepo
import com.classroom.quizmaster.data.repo.ClassworkRepo
import com.classroom.quizmaster.data.repo.ReportRepo
import com.classroom.quizmaster.data.repo.SyncRepo
import com.classroom.quizmaster.data.repo.impl.AnalyticsRepoImpl
import com.classroom.quizmaster.data.repo.impl.ClassRepoImpl
import com.classroom.quizmaster.data.repo.impl.ClassworkRepoImpl
import com.classroom.quizmaster.data.repo.impl.ReportRepoImpl
import com.classroom.quizmaster.data.repo.impl.SyncRepoImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepoModule {
    @Binds
    fun bindSyncRepo(impl: SyncRepoImpl): SyncRepo

    @Binds
    fun bindClassRepo(impl: ClassRepoImpl): ClassRepo

    @Binds
    fun bindClassworkRepo(impl: ClassworkRepoImpl): ClassworkRepo

    @Binds
    fun bindReportRepo(impl: ReportRepoImpl): ReportRepo

    @Binds
    fun bindAnalyticsRepo(impl: AnalyticsRepoImpl): AnalyticsRepo
}
