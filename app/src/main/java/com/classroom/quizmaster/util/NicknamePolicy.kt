package com.classroom.quizmaster.util

import java.security.MessageDigest
import java.util.Locale

object NicknamePolicy {
    private const val MIN_LENGTH = 2
    private const val MAX_LENGTH = 16
    private val nonAlphaNumeric = Regex("[^A-Za-z0-9 ]")
    private val whitespace = Regex("\\s+")

    fun sanitize(raw: String, salt: String = ""): String {
        val profanitySafe = ProfanityFilter.sanitize(raw)
        val scrubbed = profanitySafe
            .replace(nonAlphaNumeric, "")
            .replace(whitespace, " ")
            .trim()
        val bounded = when {
            scrubbed.isBlank() -> "Player"
            scrubbed.length < MIN_LENGTH -> (scrubbed + "Player").take(MIN_LENGTH)
            scrubbed.length > MAX_LENGTH -> scrubbed.take(MAX_LENGTH)
            else -> scrubbed
        }
        val suffix = suffixFromSalt(salt)
        if (suffix.isBlank()) return bounded
        val roomForSuffix = MAX_LENGTH - suffix.length - 1
        val prefix = if (roomForSuffix <= MIN_LENGTH) {
            bounded.take(MAX_LENGTH - suffix.length - 1)
        } else {
            bounded.take(roomForSuffix)
        }
        return "${prefix.trimEnd()}-$suffix"
    }

    fun validationError(raw: String): String? {
        val trimmed = raw.trim()
        return when {
            trimmed.length < MIN_LENGTH -> "Nickname must be at least $MIN_LENGTH characters"
            trimmed.length > MAX_LENGTH -> "Nickname must be at most $MAX_LENGTH characters"
            else -> null
        }
    }

    private fun suffixFromSalt(salt: String): String {
        if (salt.isBlank()) return ""
        val digest = MessageDigest.getInstance("SHA-1").digest(salt.toByteArray())
        val token = digest
            .take(2)
            .joinToString("") { byte -> "%02x".format(Locale.US, byte) }
        return token.uppercase(Locale.US)
    }
}
