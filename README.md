#Classroom Quiz Master (Android • Kotlin)

A fun, teacher‑friendly Android app that turns **Grade 11 General Mathematics** lessons into interactive modules with **Pre‑Test → Discussion → Post‑Test** and clear **learning‑gain** reports. Built with **Kotlin** for Android.

---

## ✨ Core Features

* **Module Flow (G11 Gen Math):** Pre‑Test → Lesson/Discussion → Post‑Test with parallel forms
* **Item Types:** Multiple‑choice, True/False, Numeric entry, Matching; media support (images/audio/video)
* **Delivery Modes:** Live (class code + leaderboard toggle) or Assignment (homework)
* **Reports:** Auto‑scoring, Pre vs Post comparison, objective mastery, commonly missed items; export PDF/CSV
* **Student UX:** Join via code + nickname/ID (no account), gentle timer + sfx, feedback explanations
* **Teacher UX:** Module builder, progress monitor, optional gamification (avatars/badges), printable summaries

> Localized labels (Tagalog) available for teachers: *Pagsusulit Bago ang Aralin*, *Talakayan/Aralin*, *Pagsusulit Pagkatapos ng Aralin*, *Pag‑angat ng Marka*.

---

## 🏗️ Tech Stack

* **Language:** Kotlin (JDK 17)
* **UI:** Jetpack Compose (Material 3)
* **Architecture:** MVVM + Clean Architecture (Domain / Data / UI), Unidirectional data flow
* **DI:** Hilt
* **Async:** Kotlin Coroutines + Flow
* **Persistence:** Room (local item bank, attempts, results)
* **Serialization:** Kotlinx Serialization
* **Networking (optional):** Retrofit/OkHttp (for future cloud sync)
* **PDF/CSV Export:** Android Print / PdfDocument + CSV via opencsv‑style writer
* **Testing:** JUnit4/5, Turbine (Flow), MockK, Compose UI testing
* **Lint/Style:** ktlint, Detekt
* **Build:** Gradle (Kotlin DSL)

---

## 📂 Suggested Project Structure

```
app/
  build.gradle.kts
  src/
    main/
      AndroidManifest.xml
      java/com/acme/quizmaster/
        App.kt
        di/...
        navigation/...
        ui/
          teacher/...
          student/...
          components/...
        feature/
          modulebuilder/...
          delivery/ (live | assignment)
          assessment/ (pre | post)
          lesson/
          reports/
        domain/
          model/ (Module, Item, Objective, Attempt, Report ...)
          usecase/ (...)
        data/
          repo/ (...)
          local/ (Room DAO, Entities, Migrations)
          remote/ (Retrofit services — optional)
    androidTest/...
    test/...
```

---

## 🔧 Requirements

* **Android Studio**: Ladybug or newer
* **Android Gradle Plugin**: 8.x
* **Kotlin**: 2.0+
* **Min SDK**: 24
* **Target SDK**: 34/35

---

## ▶️ Getting Started

```bash
# 1) Clone
# git clone https://github.com/<org>/<repo>.git

# 2) Open in Android Studio and let it sync

# 3) Create local.properties (if needed) and set JDK 17

# 4) Build & run (Pixel 6 API 34 emulator recommended)
```

### Optional: Local Config

Create `app/src/main/assets/app-config.json` for toggles.

```json
{
  "leaderboardEnabledByDefault": false,
  "feedbackMode": "after-section",  
  "locale": "en-PH",                     
  "cloudSync": false                      
}
```

---

## 🧪 Testing

```bash
./gradlew ktlintCheck detekt test connectedAndroidTest
```

---

## 📊 Data Model (simplified)

```kotlin
data class Module(
  val id: String,
  val subject: String = "G11 General Mathematics",
  val topic: String,
  val objectives: List<String>, // e.g., ["LO1","LO2","LO3"]
  val preTest: Assessment,
  val lesson: Lesson,
  val postTest: Assessment,
  val settings: ModuleSettings
)
```

(See `AGENTS.md` for agent contracts and flows.)

---

## 🔒 Privacy & Classroom Safety

* No student accounts required; join via code + nickname/ID
* Local‑first storage; optional future cloud sync
* Minimal personally identifiable information (PII) by default

---

## 🧩 Roadmap (v1 → v2)

* v1: Module builder, delivery, reports, exports, light gamification
* v2: Item bank authoring on device, team mode, advanced analytics, cloud sync, teacher portal

---

## 🤝 Contributing

1. Fork & create a feature branch
2. Write tests where sensible
3. Run linters (`ktlint`, `detekt`)
4. Open a PR with a clear description & screenshots

---

## 📜 License

MIT (or school‑specific license). Add a `LICENSE` file.

---

# AGENTS.md — Functional Agents & Contracts

This document describes the **functional agents** (modular services) that orchestrate the module lifecycle: **Pre‑Test → Lesson → Post‑Test → Reports**. Agents can run on‑device (default) with optional adapters for cloud.

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

* **Missing Post parallel items** → block publish; show which objectives need items
* **Intermittent network** (Live) → local queue & retry; continue offline
* **Corrupt media** → skip slide and log warning in report

---

## ✅ Acceptance Criteria (v1)

* Build & run on API 34 emulator; create a module; deliver pre/lesson/post; export PDF & CSV
* Reports display **Pre vs Post** and **per‑objective mastery**
* Student join via code without account; nickname/ID captured locally

---

## 📎 Appendix: Tagalog Labels

* Pre‑Test — **Pagsusulit Bago ang Aralin**
* Discussion — **Talakayan / Aralin**
* Post‑Test — **Pagsusulit Pagkatapos ng Aralin**
* Learning Gain — **Pag‑angat ng Marka**
* Mastery — **Antas ng Pagkatuto**
