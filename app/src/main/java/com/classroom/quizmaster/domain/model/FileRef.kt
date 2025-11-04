package com.classroom.quizmaster.domain.model

import kotlinx.serialization.Serializable

/**
 * Lightweight reference to a generated report or attachment that can be
 * shared across the app without eagerly loading the file contents.
 */
@Serializable
data class FileRef(
    val path: String,
    val mimeType: String? = null,
    val displayName: String? = null
)
