package com.classroom.quizmaster.lan

import com.classroom.quizmaster.domain.model.AnswerPayload
import com.classroom.quizmaster.domain.model.LiveSnapshot
import com.classroom.quizmaster.domain.model.Student
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LanClientMessage {
    @Serializable
    @SerialName("join_request")
    data class JoinRequest(
        val sessionId: String,
        val nickname: String
    ) : LanClientMessage

    @Serializable
    @SerialName("answer")
    data class Answer(
        val sessionId: String,
        val studentId: String,
        val answer: AnswerPayload
    ) : LanClientMessage

    @Serializable
    @SerialName("ping")
    data class Ping(val sessionId: String? = null) : LanClientMessage
}

@Serializable
sealed interface LanServerMessage {
    @Serializable
    @SerialName("join_ack")
    data class JoinAck(
        val accepted: Boolean,
        val studentId: String? = null,
        val displayName: String? = null,
        val reason: String? = null
    ) : LanServerMessage

    @Serializable
    @SerialName("answer_ack")
    data class AnswerAck(
        val accepted: Boolean,
        val reason: String? = null
    ) : LanServerMessage

    @Serializable
    @SerialName("snapshot")
    data class Snapshot(
        val snapshot: LiveSnapshot
    ) : LanServerMessage

    @Serializable
    @SerialName("announcement")
    data class Announcement(
        val sessionId: String,
        val moduleId: String,
        val host: String,
        val port: Int,
        val participants: Int
    ) : LanServerMessage
}

data class LanJoinAck(
    val accepted: Boolean,
    val studentId: String? = null,
    val displayName: String? = null,
    val reason: String? = null
)

data class LanAnswerAck(
    val accepted: Boolean,
    val reason: String? = null
)

@Serializable
data class DiscoveryRequest(val requestId: String = "scan")

data class LanParticipant(
    val student: Student,
    val socketId: Int
)
