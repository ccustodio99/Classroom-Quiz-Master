package com.classroom.quizmaster.data.repo

import com.classroom.quizmaster.data.local.AccountDao
import com.classroom.quizmaster.data.local.AccountEntity
import com.classroom.quizmaster.domain.model.AccountStatus
import com.classroom.quizmaster.domain.model.UserAccount
import com.classroom.quizmaster.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepositoryImpl(
    private val dao: AccountDao
) : AccountRepository {
    override suspend fun create(account: UserAccount) {
        dao.insert(account.toEntity())
    }

    override suspend fun update(account: UserAccount) {
        dao.update(account.toEntity())
    }

    override suspend fun findByEmail(email: String): UserAccount? =
        dao.findByEmail(email)?.toDomain()

    override suspend fun findById(id: String): UserAccount? =
        dao.findById(id)?.toDomain()

    override fun observeByStatus(status: AccountStatus): Flow<List<UserAccount>> {
        return dao.observeByStatus(status.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeAll(): Flow<List<UserAccount>> {
        return dao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }
}

private fun AccountEntity.toDomain(): UserAccount =
    UserAccount(
        id = id,
        email = email,
        displayName = displayName,
        role = runCatching { UserRole.valueOf(role) }.getOrElse { UserRole.Student },
        status = runCatching { AccountStatus.valueOf(status) }.getOrElse { AccountStatus.PendingApproval },
        hashedPassword = hashedPassword,
        createdAt = createdAt,
        approvedAt = approvedAt,
        approvedBy = approvedBy,
        lastLoginAt = lastLoginAt
    )

private fun UserAccount.toEntity(): AccountEntity =
    AccountEntity(
        id = id,
        email = email.lowercase(),
        displayName = displayName,
        role = role.name,
        status = status.name,
        hashedPassword = hashedPassword,
        createdAt = createdAt,
        approvedAt = approvedAt,
        approvedBy = approvedBy,
        lastLoginAt = lastLoginAt
    )
