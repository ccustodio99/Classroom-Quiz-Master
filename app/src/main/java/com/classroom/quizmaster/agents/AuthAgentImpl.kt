package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.AccountRepository
import com.classroom.quizmaster.data.util.PasswordHasher
import com.classroom.quizmaster.domain.model.AccountStatus
import com.classroom.quizmaster.domain.model.UserAccount
import com.classroom.quizmaster.domain.model.UserRole
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthAgentImpl(
    private val accountRepository: AccountRepository
) : AuthAgent {

    private val currentAccount = MutableStateFlow<UserAccount?>(null)

    override fun observeCurrentAccount(): Flow<UserAccount?> = currentAccount.asStateFlow()

    override fun observePendingAccounts(): Flow<List<UserAccount>> =
        accountRepository.observeByStatus(AccountStatus.PendingApproval)

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: UserRole
    ): Result<UserAccount> {
        val normalizedEmail = email.trim().lowercase()
        if (normalizedEmail.isBlank() || password.isBlank() || displayName.isBlank()) {
            return Result.failure(IllegalArgumentException("Missing required fields"))
        }
        val existing = accountRepository.findByEmail(normalizedEmail)
        if (existing != null) {
            return Result.failure(IllegalStateException("Account already exists"))
        }
        val now = System.currentTimeMillis()
        val status = if (role == UserRole.Admin) AccountStatus.Active else AccountStatus.PendingApproval
        val account = UserAccount(
            id = UUID.randomUUID().toString(),
            email = normalizedEmail,
            displayName = displayName.trim(),
            role = role,
            status = status,
            hashedPassword = PasswordHasher.hash(password),
            createdAt = now,
            approvedAt = if (status == AccountStatus.Active) now else null,
            approvedBy = if (status == AccountStatus.Active) normalizedEmail else null,
            lastLoginAt = null
        )
        accountRepository.create(account)
        if (status == AccountStatus.Active) {
            currentAccount.value = account
        }
        return Result.success(account)
    }

    override suspend fun login(email: String, password: String): Result<UserAccount> {
        val normalizedEmail = email.trim().lowercase()
        val account = accountRepository.findByEmail(normalizedEmail)
            ?: return Result.failure(IllegalArgumentException("Account not found"))
        if (!PasswordHasher.verify(password, account.hashedPassword)) {
            return Result.failure(IllegalArgumentException("Invalid credentials"))
        }
        if (account.status != AccountStatus.Active) {
            return Result.failure(IllegalStateException("Account pending approval"))
        }
        val now = System.currentTimeMillis()
        val updated = account.copy(lastLoginAt = now)
        accountRepository.update(updated)
        currentAccount.value = updated
        return Result.success(updated)
    }

    override suspend fun approve(adminId: String, accountId: String): Result<UserAccount> {
        val admin = accountRepository.findById(adminId)
            ?: return Result.failure(IllegalArgumentException("Admin not found"))
        if (admin.role != UserRole.Admin || admin.status != AccountStatus.Active) {
            return Result.failure(IllegalStateException("Only active admin can approve accounts"))
        }
        val account = accountRepository.findById(accountId)
            ?: return Result.failure(IllegalArgumentException("Account not found"))
        if (account.status == AccountStatus.Active) {
            return Result.success(account)
        }
        val now = System.currentTimeMillis()
        val updated = account.copy(
            status = AccountStatus.Active,
            approvedAt = now,
            approvedBy = admin.id
        )
        accountRepository.update(updated)
        return Result.success(updated)
    }

    override suspend fun logout() {
        currentAccount.value = null
    }
}
