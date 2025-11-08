package com.classroom.quizmaster.util

private const val MIN_LENGTH = 2
private const val MAX_LENGTH = 16
private const val FALLBACK_NAME = "Player"
private const val SUFFIX_LENGTH = 3

object NicknamePolicy {
    private val nonAlphaNumeric = Regex("[^A-Za-z0-9 ]")
    private val whitespace = Regex("\\s+")

    fun sanitize(raw: String, salt: String = ""): String {
        val profanitySafe = ProfanityFilter.sanitize(raw, FALLBACK_NAME)
        val stripped = profanitySafe
            .replace(nonAlphaNumeric, "")
            .replace(whitespace, " ")
            .trim()
        val bounded = ensureBounds(stripped)
        val suffix = suffixFromSalt(salt)
        if (suffix.isEmpty()) return bounded
        val available = (MAX_LENGTH - suffix.length - 1).coerceAtLeast(MIN_LENGTH)
        val prefix = bounded.take(available).trimEnd()
        return if (prefix.isBlank()) "$FALLBACK_NAME-$suffix" else "$prefix-$suffix"
    }

    fun validationError(raw: String): String? {
        val trimmed = raw.trim()
        return when {
            trimmed.isEmpty() -> "Nickname is required"
            trimmed.length < MIN_LENGTH -> "Nickname must be at least $MIN_LENGTH characters"
            trimmed.length > MAX_LENGTH -> "Nickname must be at most $MAX_LENGTH characters"
            ProfanityFilter.containsProfanity(trimmed) -> "Please choose a different nickname"
            else -> null
        }
    }

    private fun ensureBounds(value: String): String {
        if (value.isBlank()) return FALLBACK_NAME
        val sanitized = when {
            value.length < MIN_LENGTH -> value.padEnd(MIN_LENGTH, 'x')
            value.length > MAX_LENGTH -> value.take(MAX_LENGTH)
            else -> value
        }
        return sanitized.trim().ifBlank { FALLBACK_NAME }
    }

    private fun suffixFromSalt(salt: String): String {
        if (salt.isBlank()) return ""
        val digest = Idempotency.digest(listOf(salt)).uppercase()
        return digest.take(SUFFIX_LENGTH)
    }
}
