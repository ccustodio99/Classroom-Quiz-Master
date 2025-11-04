package com.classroom.quizmaster.domain.model

sealed class SyncStatus {
  object Idle : SyncStatus()
  object InProgress : SyncStatus()
  data class Success(val timestamp: Long) : SyncStatus()
  data class Error(val message: String) : SyncStatus()
}
