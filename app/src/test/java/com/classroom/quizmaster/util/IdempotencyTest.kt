package com.classroom.quizmaster.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class IdempotencyTest {

    @Test
    fun `idempotent hash is deterministic`() {
        val first = Idempotency.attemptId("user", "question", "nonce")
        val second = Idempotency.attemptId("user", "question", "nonce")
        assertEquals(first, second)
    }

    @Test
    fun `different nonce yields different hash`() {
        val first = Idempotency.attemptId("user", "question", "one")
        val second = Idempotency.attemptId("user", "question", "two")
        assertNotEquals(first, second)
    }
}
