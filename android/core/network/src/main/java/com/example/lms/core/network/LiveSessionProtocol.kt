package com.example.lms.core.network

data class QuestionPayload(
    val questionId: String,
    val stem: String,
    val options: List<String>,
    val index: Int,
)

data class AnswerPayload(
    val questionId: String,
    val userId: String,
    val answer: String,
    val latencyMs: Long,
)

object LiveSessionProtocol {
    fun encodeQuestion(payload: QuestionPayload): String = buildString {
        append(payload.index)
        append("|")
        append(payload.questionId)
        append("|")
        append(payload.stem)
        append("|")
        append(payload.options.joinToString(separator = ";"))
    }

    fun decodeQuestion(data: String): QuestionPayload {
        val parts = data.split("|", limit = 4)
        val options = if (parts.size == 4) parts[3].split(";").filter { it.isNotEmpty() } else emptyList()
        return QuestionPayload(
            index = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            questionId = parts.getOrNull(1).orEmpty(),
            stem = parts.getOrNull(2).orEmpty(),
            options = options,
        )
    }
}

