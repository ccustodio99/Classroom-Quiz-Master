package com.classroom.quizmaster.data.repo

import android.content.Context
import android.net.Uri
import com.classroom.quizmaster.domain.repository.MediaRepository
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage
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
        val ref = storage.reference.child("quiz-media/$quizId/$sanitizedName")
        val metadata = storageMetadata { contentType = mimeType }
        withContext(Dispatchers.IO) {
            ref.putBytes(bytes, metadata).await()
            ref.downloadUrl.await().toString()
        }
    }

    override suspend fun deleteAsset(remoteUrl: String): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            storage.getReferenceFromUrl(remoteUrl).delete().await()
        }
    }

    override suspend fun cacheAsset(remoteUrl: String): Result<File> = runCatching {
        val target = cachedFile(remoteUrl)
        if (target.exists()) return@runCatching target
        withContext(Dispatchers.IO) {
            target.outputStream().use { stream ->
                storage.getReferenceFromUrl(remoteUrl).getBytes(MAX_DOWNLOAD_BYTES).await().let(stream::write)
            }
        }
        target
    }

    private fun cachedFile(remoteUrl: String): File {
        val name = Uri.parse(remoteUrl).lastPathSegment?.ifBlank { null } ?: digest(remoteUrl.toByteArray())
        return File(cacheDir, name)
    }

    private fun digest(data: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(data).joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_DOWNLOAD_BYTES = 8L * 1024 * 1024 // 8 MB
    }
}
