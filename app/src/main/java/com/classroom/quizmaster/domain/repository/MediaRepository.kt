package com.classroom.quizmaster.domain.repository

import java.io.File

interface MediaRepository {
    suspend fun uploadQuizAsset(
        quizId: String,
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): Result<String>

    suspend fun deleteAsset(remoteUrl: String): Result<Unit>

    suspend fun cacheAsset(remoteUrl: String): Result<File>
}
