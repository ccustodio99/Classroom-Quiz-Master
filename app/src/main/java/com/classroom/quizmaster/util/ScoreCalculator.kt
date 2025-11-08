package com.classroom.quizmaster.util

import kotlin.math.ceil
import kotlin.math.coerceAtLeast

object ScoreCalculator {
    private const val BASE_POINTS = 400.0
    private const val BONUS_POINTS = 600.0
    private const val MAX_POINTS = 1000

    fun score(correct: Boolean, timeLeftMillis: Long, timeLimitMillis: Long): Int =
        breakdown(correct, timeLeftMillis, timeLimitMillis).points

    fun breakdown(correct: Boolean, timeLeftMillis: Long, timeLimitMillis: Long): ScoreBreakdown {
        if (!correct) return ScoreBreakdown(0, false, 0L, timeLimitMillis)
        val safeLimit = timeLimitMillis.coerceAtLeast(1L)
        val safeLeft = timeLeftMillis.coerceAtLeast(0L)
        val ratio = safeLeft.toDouble() / safeLimit.toDouble()
        val computed = ceil(BASE_POINTS + BONUS_POINTS * ratio).toInt().coerceAtMost(MAX_POINTS)
        return ScoreBreakdown(
            points = computed,
            correct = true,
            timeLeftMillis = safeLeft,
            timeLimitMillis = safeLimit
        )
    }

    fun remainingMillis(timeLimitSeconds: Int, elapsedMillis: Long): Long {
        val limitMillis = timeLimitSeconds.coerceAtLeast(0) * 1_000L
        return (limitMillis - elapsedMillis).coerceAtLeast(0L)
    }
}

data class ScoreBreakdown(
    val points: Int,
    val correct: Boolean,
    val timeLeftMillis: Long,
    val timeLimitMillis: Long
)
