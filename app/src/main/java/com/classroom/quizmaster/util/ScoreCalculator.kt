package com.classroom.quizmaster.util

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object ScoreCalculator {
    private const val BASE_POINTS = 400.0
    private const val BONUS_POINTS = 600.0
    private const val MAX_POINTS = 1000
    private const val MIN_TIME_LIMIT_MS = 1L

    fun score(correct: Boolean, timeLeftMillis: Long, timeLimitMillis: Long): Int =
        breakdown(correct, timeLeftMillis, timeLimitMillis).points

    fun breakdown(correct: Boolean, timeLeftMillis: Long, timeLimitMillis: Long): ScoreBreakdown {
        if (!correct) {
            val safeLimit = sanitizedLimit(timeLimitMillis)
            return ScoreBreakdown(0, false, sanitizedRemaining(0L, safeLimit), safeLimit)
        }
        val safeLimit = sanitizedLimit(timeLimitMillis)
        val safeRemaining = sanitizedRemaining(timeLeftMillis, safeLimit)
        val ratio = safeRemaining.toDouble() / safeLimit.toDouble()
        val computed = ceil(BASE_POINTS + BONUS_POINTS * ratio).toInt().coerceAtMost(MAX_POINTS)
        return ScoreBreakdown(computed, true, safeRemaining, safeLimit)
    }

    fun breakdownFromElapsed(correct: Boolean, elapsedMillis: Long, timeLimitMillis: Long): ScoreBreakdown {
        val safeLimit = sanitizedLimit(timeLimitMillis)
        val remaining = sanitizedRemaining(safeLimit - elapsedMillis, safeLimit)
        return breakdown(correct, remaining, safeLimit)
    }

    fun remainingMillis(timeLimitSeconds: Int, elapsedMillis: Long): Long {
        val limitMillis = max(timeLimitSeconds, 0) * 1_000L
        return sanitizedRemaining(limitMillis - elapsedMillis, limitMillis)
    }

    private fun sanitizedLimit(timeLimitMillis: Long): Long =
        max(timeLimitMillis, MIN_TIME_LIMIT_MS)

    private fun sanitizedRemaining(timeLeftMillis: Long, timeLimitMillis: Long): Long =
        max(0L, min(timeLeftMillis, timeLimitMillis))
}

data class ScoreBreakdown(
    val points: Int,
    val correct: Boolean,
    val timeLeftMillis: Long,
    val timeLimitMillis: Long,
    val maxPoints: Int = 1000
)
