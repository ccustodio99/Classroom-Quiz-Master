# AGENTS.md — Functional Agents (Kotlin Android • Mobile)

This app is modularized into **functional agents** (service‑like components) that orchestrate the flow:
**Pre‑Test → Lesson → Post‑Test → Reports**. Implementations are **on‑device** by default with optional cloud adapters.

> Each agent lists: **Responsibility • Triggers • Inputs • Outputs • Notes/Failures**

---

## 1) ModuleBuilderAgent
**Responsibility:** Create/edit module packages (pre‑test, lesson slides, post‑test, settings).  
**Triggers:** Teacher selects *New Module* or *Edit*.  
**Inputs:** Item bank (local), media, teacher settings.  
**Outputs:** `Module` persisted via Room.  
**Notes:** Validates **parallel forms** and objective mapping.

```kotlin
interface ModuleBuilderAgent {
  suspend fun createOrUpdate(module: Module): Result<Unit>
  fun validate(module: Module): List<Violation>
}
```

---

## 2) AssessmentAgent
**Responsibility:** Deliver assessments (pre/post), timing, scoring keys, feedback gating.  
**Inputs:** `Assessment`, timer config.  
**Outputs:** `Attempt` with item‑level results + timestamps.  
**Notes:** Pre/Post tests disable speed bonuses for diagnostic fairness.

```kotlin
interface AssessmentAgent {
  suspend fun start(assessmentId: String, student: Student): AttemptId
  suspend fun submit(attemptId: AttemptId, answers: List<AnswerPayload>): Scorecard
}
```

---

## 3) LessonAgent
**Responsibility:** Present slides, worked examples, mini checks; manage reveal of solution steps.  
**Inputs:** `Lesson` slides/cards, media assets.  
**Outputs:** Interaction logs (optional), mini‑check results.

```kotlin
interface LessonAgent {
  fun start(lessonId: String): LessonSessionId
  fun next(): LessonSlide
  fun recordCheck(answer: Any): CheckResult
}
```

---

## 4) LiveSessionAgent
**Responsibility:** Live delivery with class code, pacing, optional leaderboard.  
**Inputs:** `Module`, session settings.  
**Outputs:** Session state (participants, progress, responses).  
**Failures:** Network hiccups → local queue & retry.

```kotlin
interface LiveSessionAgent {
  fun createSession(moduleId: String): SessionId
  fun join(sessionId: SessionId, nickname: String): JoinResult
  fun submit(answer: AnswerPayload): Ack
  fun snapshot(): LiveSnapshot
}
```

---

## 5) AssignmentAgent
**Responsibility:** Package module for homework; set availability; collect submissions.  
**Inputs:** `Module`, assignment settings.  
**Outputs:** Attempts per student; completion status.

```kotlin
interface AssignmentAgent {
  fun assign(moduleId: String, dueEpochMs: Long): AssignmentId
  fun status(assignmentId: AssignmentId): AssignmentStatus
}
```

---

## 6) ScoringAnalyticsAgent
**Responsibility:** Compute **Pre vs Post** gains, per‑objective mastery, and common errors.  
**Inputs:** Attempts (pre/post), item metadata with objective tags.  
**Outputs:** `ClassReport`, `StudentReport`, CSV rows.

```kotlin
interface ScoringAnalyticsAgent {
  fun buildClassReport(moduleId: String): ClassReport
  fun buildStudentReport(moduleId: String, studentId: String): StudentReport
}
```

---

## 7) ReportExportAgent
**Responsibility:** Generate **PDF** (class & per‑student) and **CSV** exports.  
**Inputs:** Reports + templates.  
**Outputs:** Files in app‑private storage; share intents.

```kotlin
interface ReportExportAgent {
  suspend fun exportClassPdf(report: ClassReport): FileRef
  suspend fun exportStudentPdf(report: StudentReport): FileRef
  suspend fun exportCsv(rows: List<CsvRow>): FileRef
}
```

---

## 8) ItemBankAgent
**Responsibility:** Manage the G11 Gen Math item bank: difficulty tags, parallel forms, explanations.  
**Inputs:** Seed content & teacher‑authored items.  
**Outputs:** Queryable items for module assembly.  
**Notes:** Local‑first; supports import/export JSON for sharing.

```kotlin
interface ItemBankAgent {
  fun query(objectives: List<String>, limit: Int = 20): List<Item>
  fun upsert(items: List<Item>): Result<Unit>
}
```

**Item JSON (example)**
```json
{
  "id": "item-uuid",
  "type": "numeric",
  "objective": "LO2",
  "prompt": "P=10,000, r=8% quarterly, t=2y. Find A.",
  "answer": 11716.59,
  "tolerance": 0.01,
  "explanation": "A=P(1+r/m)^{mt}",
  "media": []
}
```

---

## 9) GamificationAgent (lightweight)
**Responsibility:** Avatars/badges; *Top Improver* & *Star of the Day*.  
**Inputs:** Reports/attempts.  
**Outputs:** Unlock events; optional banners.

```kotlin
interface GamificationAgent {
  fun onReportsAvailable(report: ClassReport)
  fun unlocksFor(studentId: String): List<Badge>
}
```

---

## 10) SyncAgent (optional)
**Responsibility:** Future cloud sync for modules, attempts, reports.  
**Notes:** Off by default; minimal PII.

```kotlin
interface SyncAgent {
  suspend fun pushModule(moduleId: String): Result<Unit>
  suspend fun pullUpdates(): Result<Int>
}
```

---

## 🔗 Interactions (Happy Path)
```
Teacher -> ModuleBuilderAgent -> Module(Room)
Teacher -> (Live or Assignment)
  Live -> LiveSessionAgent -> AssessmentAgent(pre) -> LessonAgent -> AssessmentAgent(post)
  Assignment -> AssignmentAgent -> AssessmentAgent(pre/post)
AssessmentAgent -> ScoringAnalyticsAgent -> ReportExportAgent (PDF/CSV)
GamificationAgent listens to ScoringAnalyticsAgent events
```

---

## ✅ Acceptance Criteria (v1)
- Build & run; create a module; deliver pre/lesson/post; export PDF & CSV
- Reports display **Pre vs Post** and per‑objective mastery
- Students join via code; nickname/ID captured locally

---

## 📎 Tagalog Labels
- Pre‑Test — **Pagsusulit Bago ang Aralin**
- Discussion — **Talakayan / Aralin**
- Post‑Test — **Pagsusulit Pagkatapos ng Aralin**
- Learning Gain — **Pag‑angat ng Marka**
- Mastery — **Antas ng Pagkatuto**
