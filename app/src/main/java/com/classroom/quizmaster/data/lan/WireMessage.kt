package com.classroom.quizmaster.data.lan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WireMessage {
    val type: String

    @Serializable
    @SerialName("session_state")
    data class SessionState(
        val sessionId: String,
        val status: String,
        val currentIndex: Int,
        val reveal: Boolean,
        val payload: String
    ) : WireMessage {
        override val type: String = "session_state"
    }

    @Serializable
    @SerialName("question_push")
    data class QuestionPush(
        val questionId: String,
        val stem: String,
        val choicesJson: String,
        val timeLimitSeconds: Int
    ) : WireMessage {
        override val type: String = "question_push"
    }

    @Serializable
    @SerialName("attempt_submit")
    data class AttemptSubmit(
        val attemptId: String,
        val uid: String,
        val questionId: String,
        val selectedJson: String,
        val timeMs: Long,
        val nonce: String
    ) : WireMessage {
        override val type: String = "attempt_submit"
    }

    @Serializable
    @SerialName("ack")
    data class Ack(
        val attemptId: String,
        val accepted: Boolean,
        val reason: String? = null
    ) : WireMessage {
        override val type: String = "ack"
    }

    @Serializable
    @SerialName("leaderboard")
    data class Leaderboard(
        val leaderboardJson: String
    ) : WireMessage {
        override val type: String = "leaderboard"
    }

    @Serializable
    @SerialName("reveal")
    data class Reveal(
        val questionId: String,
        val correctJson: String,
        val distributionJson: String,
        val explanation: String
    ) : WireMessage {
        override val type: String = "reveal"
    }

    @Serializable
    @SerialName("system_notice")
    data class SystemNotice(
        val message: String
    ) : WireMessage {
        override val type: String = "system_notice"
    }
}
