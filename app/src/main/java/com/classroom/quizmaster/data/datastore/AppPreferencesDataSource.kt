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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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

    suspend fun addSampleSeededTeacher(teacherId: String) {
        context.prefStore.edit {
            val current = it[Keys.SAMPLE_SEEDED_TEACHERS] ?: emptySet()
            it[Keys.SAMPLE_SEEDED_TEACHERS] = current + teacherId
        }
    }
}
