package com.classroom.quizmaster.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "module_json") val moduleJson: String
)
