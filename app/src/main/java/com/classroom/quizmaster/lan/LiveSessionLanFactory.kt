package com.classroom.quizmaster.lan

import com.classroom.quizmaster.domain.model.AnswerPayload
import com.classroom.quizmaster.domain.model.LiveSnapshot

class LiveSessionLanFactory(
    private val configuration: LanConfiguration = LanConfiguration()
) {
    fun create(
        sessionId: String,
        moduleId: String,
        onJoin: suspend (String) -> LanJoinAck,
        onAnswer: suspend (AnswerPayload) -> LanAnswerAck,
        snapshotProvider: () -> LiveSnapshot?
    ): LiveSessionLanHost {
        return LiveSessionLanHost(
            sessionId = sessionId,
            moduleId = moduleId,
            config = configuration,
            onJoin = onJoin,
            onAnswer = onAnswer,
            snapshotProvider = snapshotProvider
        )
    }
}

data class LanConfiguration(
    val serverPort: Int = 40404,
    val discoveryPort: Int = 40406
)
