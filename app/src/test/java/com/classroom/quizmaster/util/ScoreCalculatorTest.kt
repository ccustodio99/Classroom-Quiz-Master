package com.classroom.quizmaster.util

import kotlin.test.assertEquals
import org.junit.Test

class ScoreCalculatorTest {

    @Test
    fun `returns zero for incorrect answers`() {
        val breakdown = ScoreCalculator.breakdown(correct = false, timeLeftMillis = 12_000, timeLimitMillis = 30_000)
        assertEquals(0, breakdown.points)
        assertEquals(30_000, breakdown.timeLimitMillis)
        assertEquals(0, breakdown.timeLeftMillis)
    }

    @Test
    fun `awards proportional points based on remaining time`() {
        val breakdown = ScoreCalculator.breakdown(correct = true, timeLeftMillis = 15_000, timeLimitMillis = 30_000)
        assertEquals(700, breakdown.points)
        assertEquals(15_000, breakdown.timeLeftMillis)
    }

    @Test
    fun `caps score at 1000`() {
        val breakdown = ScoreCalculator.breakdown(correct = true, timeLeftMillis = 60_000, timeLimitMillis = 5_000)
        assertEquals(1000, breakdown.points)
    }

    @Test
    fun `breakdown from elapsed matches direct calculation`() {
        val direct = ScoreCalculator.breakdown(correct = true, timeLeftMillis = 18_000, timeLimitMillis = 24_000)
        val elapsedBased = ScoreCalculator.breakdownFromElapsed(correct = true, elapsedMillis = 6_000, timeLimitMillis = 24_000)
        assertEquals(direct.points, elapsedBased.points)
        assertEquals(direct.timeLeftMillis, elapsedBased.timeLeftMillis)
    }
}
