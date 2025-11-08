package com.classroom.quizmaster.data.local.converter

import androidx.room.TypeConverter
import com.classroom.quizmaster.domain.model.LanMeta
import com.classroom.quizmaster.domain.model.MediaAsset
import com.classroom.quizmaster.domain.model.QuestionType
import com.classroom.quizmaster.domain.model.ScoringMode
import com.classroom.quizmaster.domain.model.SessionStatus
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let(json::encodeToString)

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.let { json.decodeFromString<List<String>>(it) } ?: emptyList()

    @TypeConverter
    fun fromIntMap(value: Map<String, Int>?): String? = value?.let(json::encodeToString)

    @TypeConverter
    fun toIntMap(value: String?): Map<String, Int> =
        value?.let { json.decodeFromString<Map<String, Int>>(it) } ?: emptyMap()

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::fromEpochMilliseconds)

    @TypeConverter
    fun fromMediaAsset(value: MediaAsset?): String? = value?.let(json::encodeToString)

    @TypeConverter
    fun toMediaAsset(value: String?): MediaAsset? =
        value?.let { json.decodeFromString<MediaAsset>(it) }

    @TypeConverter
    fun fromLanMeta(value: LanMeta?): String? = value?.let(json::encodeToString)

    @TypeConverter
    fun toLanMeta(value: String?): LanMeta? =
        value?.let { json.decodeFromString<LanMeta>(it) }

    @TypeConverter
    fun fromQuestionType(value: QuestionType?): String? = value?.name

    @TypeConverter
    fun toQuestionType(value: String?): QuestionType? =
        value?.let { runCatching { QuestionType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromSessionStatus(value: SessionStatus?): String? = value?.name

    @TypeConverter
    fun toSessionStatus(value: String?): SessionStatus? =
        value?.let { runCatching { SessionStatus.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromScoringMode(value: ScoringMode?): String? = value?.name

    @TypeConverter
    fun toScoringMode(value: String?): ScoringMode? =
        value?.let { runCatching { ScoringMode.valueOf(it) }.getOrNull() }
}
