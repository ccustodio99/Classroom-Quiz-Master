# AGENTS.md — Functional Agents (Kotlin Android • Mobile)

This document outlines the functional agents that orchestrate the core flows of the mobile LMS. The architecture is classroom-centric, offline-first, and event-driven with Firebase as the single backend.

> Each agent lists: **Responsibility • Triggers • Inputs • Outputs • Core Entities**

---

## 1) ClassroomAgent
**Responsibility:** Manage classroom lifecycle and roster. Handles creation, joining, and role management.  
**Triggers:** User creates a class; user joins via join code; admin edits roster.  
**Inputs:** User ID, class details (subject, section), join code.  
**Outputs:** Updated `Class` and `Roster` records persisted locally and synced.  
**Core Entities:** `Class`, `Roster`, `User`

```kotlin
interface ClassroomAgent {
  suspend fun createClass(owner: User, subject: String, section: String): Result<Class>
  suspend fun joinClass(user: User, code: String): Result<Roster>
  suspend fun getRoster(classId: String): List<Roster>
}
```

---

## 2) ClassworkAgent
**Responsibility:** Author and manage assignments, quizzes, materials, and live activities.  
**Triggers:** Instructor CRUD on classwork; system auto-assigns pre-tests.  
**Inputs:** Class ID, classwork metadata (title, type, due date, points).  
**Outputs:** `Classwork` records persisted and synced.  
**Core Entities:** `Classwork`, `Question`

```kotlin
interface ClassworkAgent {
  suspend fun createOrUpdate(classwork: Classwork): Result<Unit>
  suspend fun getAssignments(classId: String): List<Classwork>
  suspend fun submitAssignment(submission: Submission): Result<Unit>
}
```

---

## 3) AssessmentAgent
**Responsibility:** Deliver assessments, manage timing, and score responses against keys.  
**Triggers:** Learner starts a `Classwork` item of type `PRETEST`, `POSTTEST`, or `QUIZ`.  
**Inputs:** `Class` ID, `Classwork` ID, `User` ID.  
**Outputs:** `Attempt` records with answers and timestamps.  
**Core Entities:** `Attempt`, `Question`, `Submission`

```kotlin
interface AssessmentAgent {
  suspend fun start(classId: String, classworkId: String, userId: String): Attempt
  suspend fun submit(attempt: Attempt): Result<Submission>
}
```

---

## 4) LiveSessionAgent
**Responsibility:** Manage real-time in-class activities (Kahoot-style). Prioritises LAN/WebRTC, with Firestore fallback.  
**Triggers:** Instructor starts `LIVE` classwork.  
**Inputs:** `Class` ID, `Classwork` (live), host settings.  
**Outputs:** `LiveSession` state and `LiveResponse` records.  
**Notes:** Handles host failover and presence tracking.  
**Core Entities:** `LiveSession`, `LiveResponse`

```kotlin
interface LiveSessionAgent {
  suspend fun startSession(classId: String, classworkId: String, hostId: String): LiveSession
  suspend fun joinSession(joinCode: String, userId: String): Result<Unit>
  suspend fun submitResponse(response: LiveResponse): Result<Unit>
}
```

---

## 5) ScoringAnalyticsAgent
**Responsibility:** Compute learning gain by comparing pre-test and post-test scores; analyse item difficulty and engagement.  
**Triggers:** `Submission` records for pre/post tests exist.  
**Inputs:** `Attempt` records for a user/class, `Classwork` objective tags.  
**Outputs:** Analytics reports (e.g., learning gain per objective).

```kotlin
interface ScoringAnalyticsAgent {
  suspend fun calculateLearningGain(preTestAttempt: Attempt, postTestAttempt: Attempt): Map<String, Float>
}
```

---

## 6) ReportExportAgent
**Responsibility:** Generate and export reports (learning gain, class summaries) in PDF or CSV format.  
**Triggers:** Instructor requests export.  
**Inputs:** Analytics data.  
**Outputs:** `FileRef` pointing to the generated file.

```kotlin
interface ReportExportAgent {
  suspend fun exportClassSummaryPdf(classId: String): Result<FileRef>
  suspend fun exportLearningGainCsv(classId: String): Result<FileRef>
}
```

---

## 7) DataSyncAgent
**Responsibility:** Manage offline-first sync queue, coordinate with Firestore, and resolve conflicts.  
**Triggers:** Connectivity changes; local write queue updates.  
**Inputs:** Local data mutations.  
**Outputs:** `SyncStatus` events via `StateFlow`.  
**Notes:** Runs in a background worker for periodic sync.

```kotlin
interface DataSyncAgent {
  fun start()
  fun getStatus(): StateFlow<SyncStatus>
  fun triggerSync()
}
```

---

## 8) PresenceAgent
**Responsibility:** Track user presence in classrooms (RTDB heartbeat).  
**Triggers:** User opens or leaves classroom UI.  
**Inputs:** `classId`, `userId`.  
**Outputs:** Updates to `/presence/{classId}/{userId}` and real-time online lists.

```kotlin
interface PresenceAgent {
  fun goOnline(classId: String, userId: String)
  fun goOffline(classId: String, userId: String)
  fun getOnlineUsers(classId: String): Flow<List<User>>
}
```
