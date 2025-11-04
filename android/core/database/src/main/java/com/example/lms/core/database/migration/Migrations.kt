package com.example.lms.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val PLACEHOLDER_MIGRATION = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Future schema changes go here. For v1 we intentionally keep this empty.
    }
}

