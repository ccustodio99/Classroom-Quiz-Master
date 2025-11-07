package com.classroom.quizmaster

import com.classroom.quizmaster.util.ScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreCalculatorTest {

    @Test
    fun `awards zero when incorrect`() {
        val score = ScoreCalculator.score(false, 1000, 5000)
        assertEquals(0, score)
    }

    @Test
    fun `awards bonus when quick`() {
        val score = ScoreCalculator.score(true, 4000, 5000)
        assertEquals(880, score)
    }

    @Test
    fun `caps score at 1000`() {
        val score = ScoreCalculator.score(true, 10_000, 5_000)
        assertEquals(1000, score)
    }
}
