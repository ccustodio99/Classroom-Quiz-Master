package com.classroom.quizmaster.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storageMetadata
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class FirebaseStorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {

    suspend fun uploadBytes(
        path: String,
        bytes: ByteArray,
        mimeType: String? = null,
        customMetadata: Map<String, String> = emptyMap()
    ): String = withContext(Dispatchers.IO) {
        val metadata: StorageMetadata = storageMetadata {
            mimeType?.let { contentType = it }
            customMetadata.forEach { (key, value) -> setCustomMetadata(key, value) }
        }
        val reference = storage.reference.child(path)
        reference.putBytes(bytes, metadata).await()
        reference.downloadUrl.await().toString()
    }

    suspend fun deleteByUrl(remoteUrl: String) = withContext(Dispatchers.IO) {
        storage.getReferenceFromUrl(remoteUrl).delete().await()
        Unit
    }

    suspend fun fetchBytes(remoteUrl: String, maxSize: Long): ByteArray = withContext(Dispatchers.IO) {
        storage.getReferenceFromUrl(remoteUrl).getBytes(maxSize).await()
    }

    fun toReferencePath(remoteUrl: String): String =
        storage.getReferenceFromUrl(remoteUrl).path.trimStart('/')

    fun fileName(remoteUrl: String): String =
        Uri.parse(remoteUrl).lastPathSegment.orEmpty()
}
