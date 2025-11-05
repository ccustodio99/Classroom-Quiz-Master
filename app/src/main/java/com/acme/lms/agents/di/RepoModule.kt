package com.acme.lms.agents.di

import com.acme.lms.data.repo.AnalyticsRepo
import com.acme.lms.data.repo.ClassRepo
import com.acme.lms.data.repo.ClassworkRepo
import com.acme.lms.data.repo.ReportRepo
import com.acme.lms.data.repo.SyncRepo
import com.acme.lms.data.repo.impl.AnalyticsRepoImpl
import com.acme.lms.data.repo.impl.ClassRepoImpl
import com.acme.lms.data.repo.impl.ClassworkRepoImpl
import com.acme.lms.data.repo.impl.ReportRepoImpl
import com.acme.lms.data.repo.impl.SyncRepoImpl
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
