package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.UserAccount
import com.classroom.quizmaster.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthAgent {
    fun observeCurrentAccount(): Flow<UserAccount?>
    fun observePendingAccounts(): Flow<List<UserAccount>>
    suspend fun register(email: String, password: String, displayName: String, role: UserRole): Result<UserAccount>
    suspend fun login(email: String, password: String): Result<UserAccount>
    suspend fun approve(adminId: String, accountId: String): Result<UserAccount>
    suspend fun logout()
}
