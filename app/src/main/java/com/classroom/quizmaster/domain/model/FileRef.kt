package com.classroom.quizmaster.domain.model

import java.io.File

data class FileRef(val path: String) {
    fun toFile(): File = File(path)
}
