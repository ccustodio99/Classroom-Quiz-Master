package com.classroom.quizmaster.data.model

data class SyncStatus(
    val inProgress: Boolean,
    val lastSuccessAt: Long?,
    val pendingItems: Int
)
