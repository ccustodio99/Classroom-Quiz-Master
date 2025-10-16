package com.classroom.quizmaster.data.local

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.classroom.quizmaster.domain.model.Assignment
import com.classroom.quizmaster.domain.model.Attempt
import com.classroom.quizmaster.domain.model.Module
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
class JsonConverters(private val json: Json) {
    @TypeConverter
    fun fromModule(module: Module?): String? = module?.let { json.encodeToString(Module.serializer(), it) }

    @TypeConverter
    fun toModule(value: String?): Module? = value?.let { json.decodeFromString(Module.serializer(), it) }

    @TypeConverter
    fun fromAttempt(attempt: Attempt?): String? = attempt?.let { json.encodeToString(Attempt.serializer(), it) }

    @TypeConverter
    fun toAttempt(value: String?): Attempt? = value?.let { json.decodeFromString(Attempt.serializer(), it) }

    @TypeConverter
    fun fromAssignment(assignment: Assignment?): String? = assignment?.let {
        json.encodeToString(Assignment.serializer(), it)
    }

    @TypeConverter
    fun toAssignment(value: String?): Assignment? = value?.let {
        json.decodeFromString(Assignment.serializer(), it)
    }
}
