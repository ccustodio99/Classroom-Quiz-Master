package com.classroom.quizmaster.util

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.Test

class IdempotencyTest {

    @Test
    fun `digest is deterministic for identical inputs`() {
        val first = Idempotency.digest("teacher", "quiz", "session")
        val second = Idempotency.digest("teacher", "quiz", "session")
        assertEquals(first, second)
    }

    @Test
    fun `attemptId combines uid question and nonce`() {
        val id = Idempotency.attemptId("u1", "q1", "nonce")
        val other = Idempotency.attemptId("u1", "q1", "different")
        assertNotEquals(id, other)
        assertEquals(id.length, other.length)
    }

    @Test
    fun `payloadSignature uses optional salt`() {
        val plain = Idempotency.payloadSignature("payload")
        val salted = Idempotency.payloadSignature("payload", "salt")
        assertNotEquals(plain, salted)
    }
}
