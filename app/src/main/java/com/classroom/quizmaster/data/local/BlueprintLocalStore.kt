package com.classroom.quizmaster.data.local

import android.content.Context
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Offline-first snapshot store backed by a single JSON file. All blueprint agents
 * read and write through this class to guarantee atomic updates and disk
 * persistence even when the device is offline.
 */
class BlueprintLocalStore(
    context: Context,
    private val json: Json,
    storageFileName: String = "blueprint_state.json"
) {

    private val storageFile: File = File(context.filesDir, storageFileName)
    private val mutex = Mutex()
    private val _snapshot = MutableStateFlow(readFromDisk())

    val snapshot: StateFlow<LmsSnapshot> = _snapshot.asStateFlow()

    suspend fun update(transform: (LmsSnapshot) -> LmsSnapshot): LmsSnapshot =
        mutex.withLock {
            val current = _snapshot.value
            val next = transform(current)
            if (next != current) {
                _snapshot.value = next
                persist(next)
            }
            next
        }

    suspend fun clear() {
        mutex.withLock {
            _snapshot.value = LmsSnapshot()
            persist(_snapshot.value)
        }
    }

    private suspend fun persist(snapshot: LmsSnapshot) {
        val payload = json.encodeToString(LmsSnapshot.serializer(), snapshot)
        withContext(Dispatchers.IO) {
            try {
                storageFile.writeText(payload)
            } catch (io: IOException) {
                // If we fail to persist, keep working in memory – next update will retry.
            }
        }
    }

    private fun readFromDisk(): LmsSnapshot {
        return runCatching {
            if (!storageFile.exists()) {
                LmsSnapshot()
            } else {
                val text = storageFile.readText()
                json.decodeFromString(LmsSnapshot.serializer(), text)
            }
        }.getOrElse { LmsSnapshot() }
    }
}
