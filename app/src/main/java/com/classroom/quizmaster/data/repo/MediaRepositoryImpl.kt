package com.classroom.quizmaster.data.repo

import android.content.Context
import com.classroom.quizmaster.data.remote.FirebaseStorageDataSource
import com.classroom.quizmaster.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageDataSource: FirebaseStorageDataSource
) : MediaRepository {

    private val cacheDir: File by lazy {
        File(context.cacheDir, "quiz-media").apply { mkdirs() }
    }

    override suspend fun uploadQuizAsset(
        quizId: String,
        fileName: String,
        bytes: ByteArray,
        mimeType: String
    ): Result<String> = runCatching {
        val sanitizedName = fileName.ifBlank { "asset-${digest(bytes)}" }
        val path = "quiz-media/$quizId/$sanitizedName"
        storageDataSource.uploadBytes(
            path = path,
            bytes = bytes,
            mimeType = mimeType,
            customMetadata = mapOf("quizId" to quizId)
        )
    }

    override suspend fun deleteAsset(remoteUrl: String): Result<Unit> = runCatching {
        storageDataSource.deleteByUrl(remoteUrl)
    }

    override suspend fun cacheAsset(remoteUrl: String): Result<File> = runCatching {
        val target = cachedFile(remoteUrl)
        if (target.exists()) return@runCatching target
        withContext(Dispatchers.IO) {
            val bytes = storageDataSource.fetchBytes(remoteUrl, MAX_DOWNLOAD_BYTES)
            target.outputStream().use { stream -> stream.write(bytes) }
        }
        target
    }

    private fun cachedFile(remoteUrl: String): File {
        val name = storageDataSource.fileName(remoteUrl).ifBlank { digest(remoteUrl.toByteArray()) }
        return File(cacheDir, name)
    }

    private fun digest(data: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(data).joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_DOWNLOAD_BYTES = 8L * 1024 * 1024 // 8 MB
    }
}
