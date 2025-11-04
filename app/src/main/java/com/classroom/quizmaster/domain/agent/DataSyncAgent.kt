package com.classroom.quizmaster.domain.agent

import kotlinx.coroutines.flow.StateFlow

interface DataSyncAgent {
  fun start()
  fun getStatus(): StateFlow<SyncStatus>
  fun triggerSync()
}

enum class SyncStatus {
    UP_TO_DATE,
    SYNCING,
    OFFLINE,
    ERROR
}
