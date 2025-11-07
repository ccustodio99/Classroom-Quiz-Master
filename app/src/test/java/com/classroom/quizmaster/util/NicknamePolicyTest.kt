package com.classroom.quizmaster.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NicknamePolicyTest {

    @Test
    fun `sanitizes profanity and enforces length`() {
        val sanitized = NicknamePolicy.sanitize("badword", "salt")
        assertTrue(sanitized.startsWith("Player"))
        assertTrue(sanitized.length <= 16)
    }

    @Test
    fun `appends deterministic suffix when salt provided`() {
        val first = NicknamePolicy.sanitize("Teacher", "uid123")
        val second = NicknamePolicy.sanitize("Teacher", "uid123")
        assertEquals(first, second)
    }
}
