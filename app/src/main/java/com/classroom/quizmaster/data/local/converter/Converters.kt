package com.classroom.quizmaster.data.local.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let(json::encodeToString)

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        value?.let { json.decodeFromString<List<String>>(it) }
}
