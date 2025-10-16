package com.classroom.quizmaster.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assignments")
data class AssignmentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "module_id") val moduleId: String,
    @ColumnInfo(name = "assignment_json") val assignmentJson: String
)
