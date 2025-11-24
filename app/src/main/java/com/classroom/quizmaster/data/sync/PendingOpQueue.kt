package com.classroom.quizmaster.data.sync

import com.classroom.quizmaster.data.local.dao.OpLogDao
import com.classroom.quizmaster.data.local.entity.OpLogEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class PendingOpQueue @Inject constructor(
    private val opLogDao: OpLogDao,
    private val json: Json
) {

    suspend inline fun <reified T> enqueue(type: String, payload: T) {
        val entry = OpLogEntity(
            id = "op-${UUID.randomUUID()}",
            type = type,
            payloadJson = json.encodeToString(payload),
            ts = Clock.System.now().toEpochMilliseconds(),
            synced = false,
            retryCount = 0
        )
        opLogDao.enqueue(entry)
    }
}
