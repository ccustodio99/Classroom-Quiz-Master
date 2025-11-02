package com.classroom.quizmaster.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classrooms")
data class ClassroomEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "owner_id", index = true) val ownerId: String?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "subject") val subject: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "grade_level") val gradeLevel: String,
    @ColumnInfo(name = "section") val section: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "profile_json") val profileJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?
)
