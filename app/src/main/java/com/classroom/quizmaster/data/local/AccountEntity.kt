package com.classroom.quizmaster.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "email", index = true) val email: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "hashed_password") val hashedPassword: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "approved_at") val approvedAt: Long? = null,
    @ColumnInfo(name = "approved_by") val approvedBy: String? = null,
    @ColumnInfo(name = "last_login_at") val lastLoginAt: Long? = null
)
