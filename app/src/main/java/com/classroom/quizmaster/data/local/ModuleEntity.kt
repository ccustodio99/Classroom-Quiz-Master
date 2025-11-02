package com.classroom.quizmaster.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "modules")
data class ModuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "module_json") val moduleJson: String,
    @ColumnInfo(name = "pre_assessment_id", index = true) val preAssessmentId: String,
    @ColumnInfo(name = "post_assessment_id", index = true) val postAssessmentId: String,
    @ColumnInfo(name = "lesson_id", index = true) val lessonId: String
)
