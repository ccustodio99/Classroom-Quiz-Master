package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.repo.ModuleRepository
import com.classroom.quizmaster.domain.model.Module
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FirebaseSyncAgent(
    private val moduleRepository: ModuleRepository,
    private val json: Json
) : SyncAgent {

    private val firestore = Firebase.firestore
    private val modulesCollection = firestore.collection(COLLECTION_MODULES)

    override suspend fun pushModule(moduleId: String): Result<Unit> = runCatching {
        val module = moduleRepository.getModule(moduleId)
            ?: error("Module $moduleId not found locally")

        val docRef = modulesCollection.document(moduleId)
        val remoteSnapshot = docRef.get().await()
        val remoteUpdatedAt = remoteSnapshot.getLong(FIELD_UPDATED_AT) ?: 0L
        if (remoteUpdatedAt > module.updatedAt) {
            error("Remote copy is newer than local; pull updates first.")
        }

        val payload = mapOf(
            FIELD_MODULE_JSON to json.encodeToString(Module.serializer(), module),
            FIELD_UPDATED_AT to module.updatedAt,
            FIELD_CLASSROOM_ID to module.classroom.id
        )
        docRef.set(payload, SetOptions.merge()).await()
    }

    override suspend fun pullUpdates(): Result<Int> = runCatching {
        val snapshot = modulesCollection.get().await()
        var applied = 0
        snapshot.documents.forEach { document ->
            val moduleJson = document.getString(FIELD_MODULE_JSON) ?: return@forEach
            val remoteUpdatedAt = document.getLong(FIELD_UPDATED_AT) ?: 0L
            val module = json.decodeFromString(Module.serializer(), moduleJson)
            val local = moduleRepository.getModule(module.id)
            val localUpdatedAt = local?.updatedAt ?: 0L
            if (local == null || remoteUpdatedAt > localUpdatedAt) {
                moduleRepository.upsert(
                    module.copy(
                        updatedAt = remoteUpdatedAt,
                        createdAt = module.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                )
                applied += 1
            }
        }
        applied
    }

    companion object {
        private const val COLLECTION_MODULES = "modules"
        private const val FIELD_MODULE_JSON = "moduleJson"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_CLASSROOM_ID = "classroomId"
    }
}
