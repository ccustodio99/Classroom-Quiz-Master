package com.classroom.quizmaster.data.auth

import com.classroom.quizmaster.data.datastore.AppPreferencesDataSource
import com.classroom.quizmaster.data.local.dao.TeacherDao
import com.classroom.quizmaster.data.local.entity.TeacherEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import java.security.MessageDigest

@Singleton
class LocalAuthManager @Inject constructor(
    private val teacherDao: TeacherDao,
    private val preferences: AppPreferencesDataSource
) {

    suspend fun cacheCredentials(email: String, password: String, displayName: String) {
        val normalizedEmail = email.trim().lowercase()
        val account = LocalTeacherAccount(
            email = normalizedEmail,
            passwordHash = hash(password),
            displayName = displayName.ifBlank { normalizedEmail.substringBefore('@') }
        )
        val accounts = loadAccounts().toMutableMap()
        accounts[normalizedEmail] = account
        persistAccounts(accounts.values)
        ensureTeacherRecord(account)
    }

    suspend fun tryOfflineLogin(email: String, password: String): Boolean {
        val normalizedEmail = email.trim().lowercase()
        val account = loadAccounts()[normalizedEmail] ?: return false
        if (account.passwordHash != hash(password)) return false
        ensureTeacherRecord(account)
        activateOfflineSession(account.email)
        return true
    }

    private suspend fun ensureTeacherRecord(account: LocalTeacherAccount) {
        val teacherId = teacherIdFor(account.email)
        val existing = teacherDao.get(teacherId)
        if (existing == null) {
            teacherDao.upsert(
                TeacherEntity(
                    id = teacherId,
                    displayName = account.displayName,
                    email = account.email,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    private suspend fun activateOfflineSession(email: String) {
        val teacherId = teacherIdFor(email)
        val currentFlags = preferences.featureFlags.first()
        if (!currentFlags.contains(LOCAL_TEACHER_FLAG)) {
            preferences.setFeatureFlags(currentFlags + LOCAL_TEACHER_FLAG)
        }
        preferences.setLastTeacherId(teacherId)
    }

    private suspend fun loadAccounts(): Map<String, LocalTeacherAccount> =
        preferences.localTeacherAccounts.first()
            .mapNotNull { entry ->
                val parts = entry.split("|", ignoreCase = false, limit = 3)
                if (parts.size == 3) LocalTeacherAccount(parts[0], parts[1], parts[2]) else null
            }
            .associateBy { it.email }

    private suspend fun persistAccounts(accounts: Collection<LocalTeacherAccount>) {
        val serialized = accounts.map { "${it.email}|${it.passwordHash}|${it.displayName}" }.toSet()
        preferences.setLocalTeacherAccounts(serialized)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun teacherIdFor(email: String): String = "local-${email.lowercase()}"

    private data class LocalTeacherAccount(
        val email: String,
        val passwordHash: String,
        val displayName: String
    )

    companion object {
        const val LOCAL_TEACHER_FLAG = "local_teacher_account"
    }
}
