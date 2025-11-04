package com.example.lms.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object StringListConverter {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    @JvmStatic
    fun fromList(value: List<String>?): String? = value?.let { json.encodeToString(it) }

    @TypeConverter
    @JvmStatic
    fun toList(value: String?): List<String> = value?.let { json.decodeFromString(it) } ?: emptyList()
}

