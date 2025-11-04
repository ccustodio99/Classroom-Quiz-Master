package com.classroom.quizmaster.agents

import com.classroom.quizmaster.data.model.Class
import com.classroom.quizmaster.data.model.Roster
import com.classroom.quizmaster.data.model.User
import com.classroom.quizmaster.data.model.Classwork
import com.classroom.quizmaster.data.model.Submission
import com.classroom.quizmaster.data.model.Attempt
import com.classroom.quizmaster.data.model.LiveSession
import com.classroom.quizmaster.data.model.LiveResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ClassroomAgent {
  suspend fun createClass(owner: User, subject: String, section: String): Result<Class>
  suspend fun joinClass(user: User, code: String): Result<Roster>
  suspend fun getRoster(classId: String): List<Roster>
}

interface ClassworkAgent {
  suspend fun createOrUpdate(classwork: Classwork): Result<Unit>
  suspend fun getAssignments(classId: String): List<Classwork>
  suspend fun submitAssignment(submission: Submission): Result<Unit>
}

interface AssessmentAgent {
  suspend fun start(classworkId: String, userId: String): Attempt
  suspend fun submit(attempt: Attempt): Result<Submission>
}

interface LiveSessionAgent {
  suspend fun startSession(classworkId: String, hostId: String): LiveSession
  suspend fun joinSession(joinCode: String, userId: String): Result<Unit>
  suspend fun submitResponse(response: LiveResponse): Result<Unit>
}

interface ScoringAnalyticsAgent {
  suspend fun calculateLearningGain(preTestAttempt: Attempt, postTestAttempt: Attempt): Map<String, Float>
}

interface ReportExportAgent {
  suspend fun exportClassSummaryPdf(classId: String): Result<FileRef>
  suspend fun exportLearningGainCsv(classId: String): Result<FileRef>
}

interface DataSyncAgent {
  fun start()
  fun getStatus(): StateFlow<SyncStatus>
  fun triggerSync()
}

interface PresenceAgent {
  fun goOnline(classId: String, userId: String)
  fun goOffline(classId: String, userId: String)
  fun getOnlineUsers(classId: String): Flow<List<User>>
}

data class FileRef(val path: String)

enum class SyncStatus {
    UP_TO_DATE,
    SYNCING,
    OFFLINE,
    ERROR
}
