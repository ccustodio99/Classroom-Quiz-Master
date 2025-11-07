package com.classroom.quizmaster.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ScoreCalculatorTest {

    @Test
    fun `returns zero when incorrect`() {
        assertEquals(0, ScoreCalculator.score(false, 10_000, 30_000))
    }

    @Test
    fun `caps score at 1000`() {
        assertEquals(1000, ScoreCalculator.score(true, 10_000, 1_000))
    }

    @Test
    fun `awards proportional score based on time left`() {
        val score = ScoreCalculator.score(true, 15_000, 30_000)
        assertEquals(700, score)
    }
}
