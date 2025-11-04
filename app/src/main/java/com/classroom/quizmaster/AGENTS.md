# AGENTS.md — Functional Agents (Kotlin Android • Mobile)

This document outlines the functional agents that orchestrate the core flows of the mobile LMS. The architecture is designed to be classroom-centric, offline-first, and event-driven, with Firebase as the backend.

> Each agent lists: **Responsibility • Triggers • Inputs • Outputs • Core Entities**

---

## 1) ClassroomAgent
**Responsibility:** Manage classroom lifecycle and roster. Handles creation, joining, and user role management.
**Triggers:** User creates a class; user joins a class via code; admin manages rosters.
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
**Responsibility:** Author and manage all classwork items, including assignments, quizzes, and materials.
**Triggers:** Instructor creates/updates classwork; system auto-assigns pre-tests.
**Inputs:** Class ID, classwork details (title, type, due date, points).
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
**Responsibility:** Deliver assessments (pre-test, post-test, quizzes), manage timing, and score responses against keys.
**Triggers:** Learner starts a `Classwork` item of type `PRETEST`, `POSTTEST`, or `QUIZ`.
**Inputs:** `Classwork` ID, `User` ID.
**Outputs:** `Attempt` record with detailed answers and timestamps.
**Core Entities:** `Attempt`, `Question`, `Submission`

```kotlin
interface AssessmentAgent {
  suspend fun start(classworkId: String, userId: String): Attempt
  suspend fun submit(attempt: Attempt): Result<Submission>
}
```

---

## 4) LiveSessionAgent
**Responsibility:** Manage real-time, Kahoot-style in-class activities. Prioritizes local network (LAN) for communication, falling back to Firebase RTDB.
**Triggers:** Instructor starts a `LIVE` classwork item.
**Inputs:** `Classwork` (Live), host settings.
**Outputs:** `LiveSession` state, `LiveResponse` records.
**Notes:** Handles host failover and presence tracking within the session.
**Core Entities:** `LiveSession`, `LiveResponse`

```kotlin
interface LiveSessionAgent {
  suspend fun startSession(classworkId: String, hostId: String): LiveSession
  suspend fun joinSession(joinCode: String, userId: String): Result<Unit>
  suspend fun submitResponse(response: LiveResponse): Result<Unit>
}
```

---

## 5) ScoringAnalyticsAgent
**Responsibility:** Compute learning gain by comparing pre-test and post-test scores. Analyzes item difficulty and engagement.
**Triggers:** `Submission` records for pre-tests and post-tests are available.
**Inputs:** `Attempt` records for a user/class, `Classwork` objective tags.
**Outputs:** Analytics reports (e.g., learning gain per objective).

```kotlin
interface ScoringAnalyticsAgent {
  suspend fun calculateLearningGain(preTestAttempt: Attempt, postTestAttempt: Attempt): Map<String, Float>
}
```

---

## 6) ReportExportAgent
**Responsibility:** Generate and export reports (e.g., learning gain, class summaries) in PDF or CSV format.
**Triggers:** Instructor requests to export analytics.
**Inputs:** Analytics data.
**Outputs:** `FileRef` pointing to the generated report file.

```kotlin
interface ReportExportAgent {
  suspend fun exportClassSummaryPdf(classId: String): Result<FileRef>
  suspend fun exportLearningGainCsv(classId: String): Result<FileRef>
}
```

---

## 7) DataSyncAgent
**Responsibility:** Core of the offline-first system. Manages the local data queue, syncs with Firebase Firestore/RTDB when online, and handles conflicts.
**Triggers:** Network connectivity changes; data is created/updated locally.
**Inputs:** Local data changes (writes, updates, deletes).
**Outputs:** Sync status events (`SyncStatus`).
**Notes:** Uses a background worker for periodic sync.

```kotlin
interface DataSyncAgent {
  fun start()
  fun getStatus(): StateFlow<SyncStatus>
  fun triggerSync()
}
```

---

## 8) PresenceAgent
**Responsibility:** Manage real-time user presence within a classroom using Firebase RTDB.
**Triggers:** User enters or leaves a classroom screen.
**Inputs:** `classId`, `userId`.
**Outputs:** Updates to `/presence/{classId}/{userId}` in RTDB.

```kotlin
interface PresenceAgent {
  fun goOnline(classId: String, userId: String)
  fun goOffline(classId: String, userId: String)
  fun getOnlineUsers(classId: String): Flow<List<User>>
}
```
