package com.classroom.quizmaster.util

import java.security.MessageDigest
import java.util.Locale

object Idempotency {
    private const val ALGORITHM = "SHA-1"

    fun attemptId(uid: String, questionId: String, nonce: String): String =
        digest(uid, questionId, nonce)

    fun payloadSignature(payload: String, salt: String = ""): String =
        digest(payload, salt)

    fun digest(vararg parts: String): String = digest(parts.asIterable())

    fun digest(parts: Iterable<String>): String = hash(parts.joinToString(separator = "|"))

    fun stableKey(parts: Iterable<String>): String = digest(parts)

    private fun hash(material: String): String {
        val messageDigest = MessageDigest.getInstance(ALGORITHM)
        val bytes = messageDigest.digest(material.toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(Locale.US, byte) }
    }
}
