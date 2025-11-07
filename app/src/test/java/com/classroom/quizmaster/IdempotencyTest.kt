package com.classroom.quizmaster

import com.classroom.quizmaster.util.Idempotency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class IdempotencyTest {
    @Test
    fun `same inputs yield same id`() {
        val id1 = Idempotency.attemptId("u1", "q1", "nonce")
        val id2 = Idempotency.attemptId("u1", "q1", "nonce")
        assertEquals(id1, id2)
    }

    @Test
    fun `different nonce yields different id`() {
        val id1 = Idempotency.attemptId("u1", "q1", "1")
        val id2 = Idempotency.attemptId("u1", "q1", "2")
        assertNotEquals(id1, id2)
    }
}
