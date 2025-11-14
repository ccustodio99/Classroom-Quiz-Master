package com.classroom.quizmaster.util

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Test

class NicknamePolicyTest {

    @After
    fun tearDown() {
        ProfanityFilter.clearCustomWords()
    }

    @Test
    fun `sanitize removes profanity and enforces bounds`() {
        val sanitized = NicknamePolicy.sanitize("  F*** Wizard  ")
        assertEquals("Player", sanitized)
    }

    @Test
    fun `sanitize appends salted suffix when provided`() {
        val sanitized = NicknamePolicy.sanitize("Nova", "user-1234")
        assertTrue(sanitized.startsWith("Nova"))
        assertEquals(1, sanitized.count { it == '-' })
        assertEquals(3, sanitized.substringAfter('-').length)
    }

    @Test
    fun `validation enforces minimum length`() {
        assertEquals("Nickname must be at least 2 characters", NicknamePolicy.validationError("A"))
    }

    @Test
    fun `validation detects profanity`() {
        ProfanityFilter.addCustomWords(listOf("wizard"))
        assertEquals("Please choose a different nickname", NicknamePolicy.validationError("wizard"))
    }

    @Test
    fun `validation passes for clean nickname`() {
        assertNull(NicknamePolicy.validationError("Quiz Hero"))
    }
}
