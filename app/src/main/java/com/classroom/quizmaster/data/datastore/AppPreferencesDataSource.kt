package com.classroom.quizmaster.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
    }

    val highContrastEnabled: Flow<Boolean> =
        context.prefStore.data.map { it[Keys.HIGH_CONTRAST] ?: false }

    val largeTextEnabled: Flow<Boolean> =
        context.prefStore.data.map { it[Keys.LARGE_TEXT] ?: false }

    val lastTeacherId: Flow<String?> =
        context.prefStore.data.map { it[Keys.LAST_TEACHER_ID] }

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
}
