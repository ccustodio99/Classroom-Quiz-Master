package com.classroom.quizmaster.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "module_id") val moduleId: String,
    @ColumnInfo(name = "assessment_id") val assessmentId: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "student_name") val studentName: String,
    @ColumnInfo(name = "attempt_json") val attemptJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
