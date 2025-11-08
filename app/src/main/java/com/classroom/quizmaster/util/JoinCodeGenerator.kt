package com.classroom.quizmaster.util

import java.util.Locale
import kotlin.random.Random

object JoinCodeGenerator {
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private const val MIN_LENGTH = 6
    private const val MAX_LENGTH = 8
    private const val MAX_ATTEMPTS = 12
    private val VALID_REGEX = "^[A-Z0-9]{${MIN_LENGTH},${MAX_LENGTH}}$".toRegex()
    private val bannedSubstrings = setOf(
        "ASS",
        "SEX",
        "FUK",
        "FUC",
        "CUM",
        "P00",
        "POO",
        "DIE",
        "GOD",
        "BAD"
    )

    fun generate(length: Int = MIN_LENGTH, random: Random = Random.Default): String {
        require(length in MIN_LENGTH..MAX_LENGTH) {
            "Join code length must be between $MIN_LENGTH and $MAX_LENGTH"
        }
        return build(length, random)
    }

    fun generate(lengthRange: IntRange, random: Random = Random.Default): String {
        require(!lengthRange.isEmpty()) { "Length range must not be empty" }
        val safeRange = lengthRange.first.coerceAtLeast(MIN_LENGTH)..lengthRange.last.coerceAtMost(MAX_LENGTH)
        val actualLength = random.nextInt(safeRange.first, safeRange.last + 1)
        return build(actualLength, random)
    }

    fun isValid(code: String): Boolean =
        code.length in MIN_LENGTH..MAX_LENGTH &&
            code.uppercase(Locale.US).all { it in ALPHABET }

    fun normalize(code: String): String =
        code.uppercase(Locale.US)
            .filter { it in ALPHABET }
            .take(MAX_LENGTH)

    fun parseOrNull(raw: String): String? = normalize(raw).takeIf(::isValid)

    fun requireValid(code: String) {
        require(VALID_REGEX.matches(code.uppercase(Locale.US))) {
            "Join code must be $MIN_LENGTH-$MAX_LENGTH uppercase characters"
        }
    }

    private fun build(length: Int, random: Random): String {
        repeat(MAX_ATTEMPTS - 1) {
            val candidate = buildCandidate(length, random)
            if (isSafe(candidate)) return candidate
        }
        val fallback = buildCandidate(length, random)
        return sanitizeUnsafe(fallback)
    }

    private fun buildCandidate(length: Int, random: Random): String =
        buildString(length) {
            repeat(length) {
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }

    private fun isSafe(code: String): Boolean =
        bannedSubstrings.none { banned -> code.contains(banned) }

    private fun sanitizeUnsafe(code: String): String {
        var sanitized = code
        bannedSubstrings.forEach { banned ->
            sanitized = sanitized.replace(banned, "X".repeat(banned.length))
        }
        return sanitized
    }
}
