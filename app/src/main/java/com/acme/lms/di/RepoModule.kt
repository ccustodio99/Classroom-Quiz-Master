package com.acme.lms.di

import com.acme.lms.data.repo.SyncRepo
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
}
