package com.classroom.quizmaster.domain.agent

import com.classroom.quizmaster.domain.model.SyncStatus
import kotlinx.coroutines.flow.StateFlow

interface DataSyncAgent {
  fun start()
  fun getStatus(): StateFlow<SyncStatus>
  fun triggerSync()
}
