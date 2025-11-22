package com.classroom.quizmaster.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.classroom.quizmaster.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.prefStore: DataStore<Preferences> by preferencesDataStore("quizmaster_prefs")

@Singleton
class AppPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val LARGE_TEXT = booleanPreferencesKey("large_text")
        val LAST_TEACHER_ID = stringPreferencesKey("last_teacher_id")
        val OFFLINE_BANNER_DISMISSED = booleanPreferencesKey("offline_banner_dismissed")
        val PREFERRED_AVATAR = stringPreferencesKey("preferred_avatar")
        val PREFERRED_NICKNAME = stringPreferencesKey("preferred_nickname")
        val FEATURE_FLAGS = stringSetPreferencesKey("feature_flags")
        val LOCAL_TEACHER_ACCOUNTS = stringSetPreferencesKey("local_teacher_accounts")
        val LAST_SYNC_EPOCH = longPreferencesKey("last_sync_epoch")
        val LAN_AUTO_JOIN = booleanPreferencesKey("lan_auto_join")
        val SAMPLE_SEEDED_TEACHERS = stringSetPreferencesKey("sample_seeded_teachers")
        val SAMPLE_SEEDED_CLASSROOMS = stringSetPreferencesKey("sample_seeded_classrooms")
        val USER_ROLES = stringSetPreferencesKey("user_roles")
    }

    val highContrastEnabled: Flow<Boolean> =
        context.prefStore.data.map { it[Keys.HIGH_CONTRAST] ?: false }

    val largeTextEnabled: Flow<Boolean> =
        context.prefStore.data.map { it[Keys.LARGE_TEXT] ?: false }

    val lastTeacherId: Flow<String?> =
        context.prefStore.data.map { it[Keys.LAST_TEACHER_ID] }

    val offlineBannerDismissed: Flow<Boolean> =
        context.prefStore.data.map { it[Keys.OFFLINE_BANNER_DISMISSED] ?: false }

    val preferredAvatar: Flow<String?> =
        context.prefStore.data.map { it[Keys.PREFERRED_AVATAR] }

    val preferredNickname: Flow<String?> =
        context.prefStore.data.map { it[Keys.PREFERRED_NICKNAME] }

    val featureFlags: Flow<Set<String>> =
        context.prefStore.data.map { it[Keys.FEATURE_FLAGS] ?: emptySet() }

    val localTeacherAccounts: Flow<Set<String>> =
        context.prefStore.data.map { it[Keys.LOCAL_TEACHER_ACCOUNTS] ?: emptySet() }

    val lastSuccessfulSyncEpoch: Flow<Long> =
        context.prefStore.data.map { it[Keys.LAST_SYNC_EPOCH] ?: 0L }

    val sampleSeededTeachers: Flow<Set<String>> =
        context.prefStore.data.map { it[Keys.SAMPLE_SEEDED_TEACHERS] ?: emptySet() }

    val lanAutoJoinEnabled: Flow<Boolean> =
        context.prefStore.data.map { it[Keys.LAN_AUTO_JOIN] ?: false }

    val userRoles: Flow<Map<String, UserRole>> =
        context.prefStore.data.map { prefs ->
            (prefs[Keys.USER_ROLES] ?: emptySet())
                .mapNotNull { entry ->
                    val parts = entry.split("|", limit = 2)
                    if (parts.size == 2) {
                        val role = runCatching { UserRole.valueOf(parts[1]) }.getOrNull()
                        if (role != null) parts[0] to role else null
                    } else {
                        null
                    }
                }
                .toMap()
        }

    suspend fun setHighContrast(enabled: Boolean) {
        context.prefStore.edit { it[Keys.HIGH_CONTRAST] = enabled }
    }

    suspend fun setLargeText(enabled: Boolean) {
        context.prefStore.edit { it[Keys.LARGE_TEXT] = enabled }
    }

    suspend fun setLastTeacherId(id: String?) {
        context.prefStore.edit {
            if (id == null) it.remove(Keys.LAST_TEACHER_ID) else it[Keys.LAST_TEACHER_ID] = id
        }
    }

    suspend fun setOfflineBannerDismissed(dismissed: Boolean) {
        context.prefStore.edit { it[Keys.OFFLINE_BANNER_DISMISSED] = dismissed }
    }

    suspend fun setPreferredAvatar(avatar: String?) {
        context.prefStore.edit {
            if (avatar == null) it.remove(Keys.PREFERRED_AVATAR) else it[Keys.PREFERRED_AVATAR] = avatar
        }
    }

    suspend fun setUserRole(userId: String, role: UserRole) {
        context.prefStore.edit { prefs ->
            val existing = prefs[Keys.USER_ROLES] ?: emptySet()
            val filtered = existing.filterNot { it.startsWith("$userId|") }.toMutableSet()
            filtered += "$userId|${role.name}"
            prefs[Keys.USER_ROLES] = filtered
        }
    }

    suspend fun setPreferredNickname(nickname: String?) {
        context.prefStore.edit {
            if (nickname == null) it.remove(Keys.PREFERRED_NICKNAME) else it[Keys.PREFERRED_NICKNAME] = nickname
        }
    }

    suspend fun setFeatureFlags(flags: Set<String>) {
        context.prefStore.edit { it[Keys.FEATURE_FLAGS] = flags }
    }

    suspend fun setLocalTeacherAccounts(accounts: Set<String>) {
        context.prefStore.edit { it[Keys.LOCAL_TEACHER_ACCOUNTS] = accounts }
    }

    suspend fun updateLastSuccessfulSync(epochMillis: Long) {
        context.prefStore.edit { it[Keys.LAST_SYNC_EPOCH] = epochMillis }
    }

    suspend fun setLanAutoJoin(enabled: Boolean) {
        context.prefStore.edit { it[Keys.LAN_AUTO_JOIN] = enabled }
    }

    suspend fun addSampleSeededTeacher(teacherId: String, classroomIds: Set<String>) {
        context.prefStore.edit {
            val currentTeachers = it[Keys.SAMPLE_SEEDED_TEACHERS] ?: emptySet()
            it[Keys.SAMPLE_SEEDED_TEACHERS] = currentTeachers + teacherId
            val existingClassrooms = it[Keys.SAMPLE_SEEDED_CLASSROOMS] ?: emptySet()
            val filtered = existingClassrooms.filterNot { entry -> entry.startsWith("$teacherId|") }.toMutableSet()
            classroomIds.forEach { classroomId ->
                filtered += "$teacherId|$classroomId"
            }
            it[Keys.SAMPLE_SEEDED_CLASSROOMS] = filtered
        }
    }

    suspend fun seededClassroomIds(teacherId: String): Set<String> {
        val entries = context.prefStore.data.first()[Keys.SAMPLE_SEEDED_CLASSROOMS] ?: emptySet()
        return entries.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2 && parts[0] == teacherId) parts[1] else null
        }.toSet()
    }

    suspend fun removeSampleSeededTeacher(teacherId: String) {
        context.prefStore.edit {
            val currentTeachers = it[Keys.SAMPLE_SEEDED_TEACHERS] ?: emptySet()
            it[Keys.SAMPLE_SEEDED_TEACHERS] = currentTeachers - teacherId
            val existingClassrooms = it[Keys.SAMPLE_SEEDED_CLASSROOMS] ?: emptySet()
            it[Keys.SAMPLE_SEEDED_CLASSROOMS] = existingClassrooms.filterNot { entry ->
                entry.startsWith("$teacherId|")
            }.toSet()
        }
    }
}
