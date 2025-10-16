# AGENTS.md — Functional Agents & Contracts

This document describes the **functional agents** (modular services) that orchestrate the module lifecycle: **Pre‑Test → Lesson → Post‑Test → Reports**. Agents run on‑device by default, with optional adapters for cloud.

> Each agent lists: **Responsibility • Triggers • Inputs • Outputs • Failures/Notes**

---

## 1) ModuleBuilderAgent
**Responsibility:** Create/edit module packages (tests, lesson slides, settings).  
**Triggers:** Teacher taps *New Module* or *Edit*.  
**Inputs:** Item bank, media assets, teacher settings.  
**Outputs:** `Module` object persisted in Room.  
**Notes:** Validates parallel forms (Pre/Post alignment), objective mapping.

**Contract (Kotlin)**
```kotlin
interface ModuleBuilderAgent {
  suspend fun createOrUpdate(module: Module): Result<Unit>
  fun validate(module: Module): List<Violation>
}
```

---

## 2) LiveSessionAgent
**Responsibility:** Run **Live Mode** delivery with class code, pacing, leaderboard toggle.  
**Triggers:** Teacher starts *Live*.  
**Inputs:** `Module`, session settings.  
**Outputs:** Session state (participants, progress, responses).  
**Failures:** Connectivity hiccups (if using local wifi/P2P); fallback to offline local queue.

**Contract**
```kotlin
interface LiveSessionAgent {
  fun createSession(moduleId: String): SessionId
  fun join(sessionId: SessionId, nickname: String): JoinResult
  fun submit(answer: AnswerPayload): Ack
  fun snapshot(): LiveSnapshot
}
```

---

## 3) AssignmentAgent
**Responsibility:** Package module for homework; schedule availability; collect submissions.  
**Triggers:** Teacher assigns *Due Date*.  
**Inputs:** `Module`, assignment settings.  
**Outputs:** Attempt records per student; completion status.

---

## 4) AssessmentAgent
**Responsibility:** Deliver **Pre/Post** tests, timing, scoring keys, feedback gating.  
**Inputs:** Assessment blueprint, timer config.  
**Outputs:** `Attempt` (per student), item‑level scores, timestamps.  
**Notes:** Disables speed bonus for fairness in diagnostics.

**Contract**
```kotlin
interface AssessmentAgent {
  suspend fun start(assessmentId: String, student: Student): AttemptId
  suspend fun submit(attemptId: AttemptId, answers: List<AnswerPayload>): Scorecard
}
```

---

## 5) LessonAgent
**Responsibility:** Present slides, worked examples, mini‑checks; manage reveal of solution steps.  
**Inputs:** `Lesson` slides/cards, media.  
**Outputs:** Interaction logs (optional), mini‑check results.

---

## 6) ScoringAnalyticsAgent
**Responsibility:** Aggregate **Pre vs Post**; compute gains, objective mastery, common errors.  
**Inputs:** Attempts (pre/post), item metadata (objective tags).  
**Outputs:** `ClassReport`, `StudentReport`, CSV rows.  
**Failures:** Missing parallel mapping → warn ModuleBuilderAgent.

**Contract**
```kotlin
interface ScoringAnalyticsAgent {
  fun buildReports(moduleId: String): ClassReport
  fun studentReport(moduleId: String, studentId: String): StudentReport
}
```

---

## 7) ReportExportAgent
**Responsibility:** Generate **PDF** (class & per‑student) and **CSV** exports.  
**Inputs:** Reports + templates.  
**Outputs:** PDF/CSV files in app‑private storage; share intents.

---

## 8) ItemBankAgent
**Responsibility:** Manage item bank (G11 Gen Math), difficulty tags, parallel forms, explanations.  
**Inputs:** Seed content, teacher‑authored items.  
**Outputs:** Queryable items for module assembly.  
**Notes:** Local‑first; import/export JSON for sharing.

**Sample JSON Schema**
```json
{
  "id": "item-uuid",
  "type": "numeric",
  "objective": "LO2",
  "stem": "P=10,000, r=8% quarterly, t=2y. Find A.",
  "answer": "11716.59",
  "tolerance": 0.01,
  "explanation": "A=P(1+r/m)^{mt}",
  "media": []
}
```

---

## 9) GamificationAgent (lightweight)
**Responsibility:** Avatars/badges; *Top Improver* and *Star of the Day*.  
**Inputs:** Reports/attempts.  
**Outputs:** Unlock events; optional UI banners.

---

## 10) SyncAgent (optional)
**Responsibility:** Future cloud sync (modules, attempts, reports).  
**Notes:** Off by default; complies with PII minimization.

---

## 🔗 Agent Interactions (Happy Path)
```
Teacher -> ModuleBuilderAgent -> Module(Room)
Teacher -> (Live or Assignment)
  Live -> LiveSessionAgent -> AssessmentAgent(pre) -> LessonAgent -> AssessmentAgent(post)
  Assignment -> AssignmentAgent -> AssessmentAgent(pre/post)
AssessmentAgent -> ScoringAnalyticsAgent -> ReportExportAgent (PDF/CSV)
GamificationAgent listens to ScoringAnalyticsAgent events
```

---

## 🛡️ Failure & Recovery
- **Missing Post parallel items** → block publish; show which objectives need items
- **Intermittent network** (Live) → local queue & retry; continue offline
- **Corrupt media** → skip slide and log warning in report

---

## ✅ Acceptance Criteria (v1)
- Build & run on API 34 emulator; create a module; deliver pre/lesson/post; export PDF & CSV
- Reports display **Pre vs Post** and **per‑objective mastery**
- Student join via code without account; nickname/ID captured locally

---

## 📎 Appendix: Tagalog Labels
- Pre‑Test — **Pagsusulit Bago ang Aralin**  
- Discussion — **Talakayan / Aralin**  
- Post‑Test — **Pagsusulit Pagkatapos ng Aralin**  
- Learning Gain — **Pag‑angat ng Marka**  
- Mastery — **Antas ng Pagkatuto**
