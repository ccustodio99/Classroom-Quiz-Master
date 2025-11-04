package com.acme.lms.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val role: String = "learner",
    val org: String = "",
    val email: String = ""
)
