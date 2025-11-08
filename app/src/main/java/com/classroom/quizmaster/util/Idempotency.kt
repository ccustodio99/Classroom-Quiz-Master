package com.classroom.quizmaster.util

import java.security.MessageDigest
import java.util.Locale

object Idempotency {
    fun attemptId(uid: String, questionId: String, nonce: String): String =
        digest(uid, questionId, nonce)

    fun payloadSignature(payload: String, salt: String = ""): String =
        digest(payload, salt)

    fun digest(vararg parts: String): String {
        val material = parts.joinToString(separator = "|")
        val messageDigest = MessageDigest.getInstance("SHA-1")
        val bytes = messageDigest.digest(material.toByteArray())
        return bytes.joinToString("") { byte -> "%02x".format(Locale.US, byte) }
    }
}
