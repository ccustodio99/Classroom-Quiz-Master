package com.example.lms.core.network

import kotlin.test.Test
import kotlin.test.assertEquals

class LiveSessionProtocolTest {
    @Test
    fun `encode and decode round trip`() {
        val payload = QuestionPayload("q1", "What?", listOf("A", "B"), 1)
        val encoded = LiveSessionProtocol.encodeQuestion(payload)
        val decoded = LiveSessionProtocol.decodeQuestion(encoded)
        assertEquals(payload.questionId, decoded.questionId)
        assertEquals(payload.stem, decoded.stem)
        assertEquals(payload.options, decoded.options)
        assertEquals(payload.index, decoded.index)
    }
}

