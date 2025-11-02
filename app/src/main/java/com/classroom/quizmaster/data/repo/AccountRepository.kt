package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.domain.model.AccountStatus
import com.classroom.quizmaster.domain.model.UserAccount
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    suspend fun create(account: UserAccount)
    suspend fun update(account: UserAccount)
    suspend fun findByEmail(email: String): UserAccount?
    suspend fun findById(id: String): UserAccount?
    fun observeByStatus(status: AccountStatus): Flow<List<UserAccount>>
    fun observeAll(): Flow<List<UserAccount>>
}
