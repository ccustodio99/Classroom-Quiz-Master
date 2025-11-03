# Classroom Quiz Master (Kotlin Android • Jetpack Compose)

A teacher‑friendly **Kotlin Android** mobile app that turns **Grade 11 General Mathematics** lessons into interactive, measurable modules with a **Pre‑Test → Lesson → Post‑Test** flow and clear learning‑gain reports.

> **Platform:** Android (Kotlin) • **UI:** Jetpack Compose (Material 3) • **Arch:** MVVM + Clean Architecture • **Min SDK:** 24 • **Target SDK:** 34/35

---

## 🎓 How Classroom Quiz Master Works
Classroom Quiz Master brings interactive assessments, structured lessons, and actionable reporting together so teachers can measure learning in real time-online or offline.

## 🔐 Accounts & Access Control
- **Default Admin:** On first launch the app seeds `admin@classroom.local` / `admin123`. Use this account to approve new teachers and students.
- **Teacher & Student Signup:** Tap **Sign up** on the login screen to request access. Teachers can immediately browse the classroom manager once approved; students go straight to the join screen.
- **Admin Approval:** Pending accounts appear under the **Pending approvals** list. Approving an account promotes it to active status and unlocks the appropriate home screen.
- **Local-First Storage + Firebase Mirror:** Credentials, classes, and modules are cached offline via Room, then mirrored to Firestore when sync runs.
- **Session Awareness:** The app remembers the current account while running. Logging out returns to the authentication screen without deleting any data.

### 🌩️ Cloud Sync (Firebase + Room)
- **Structured Data:** Modules are serialized and pushed to Firestore. The `updatedAt` timestamp protects against concurrent edits (newer copies win).
- **Offline First:** Every change lands in Room immediately; Firestore sync is best-effort and safe to run even without connectivity.
- **Conflict Feedback:** If Firestore has a newer version, pushes fail fast so teachers can pull updates before overwriting a peer.
- **Learning Materials:** Binary assets (slides, docs, media) are still local by default. Point the `SyncAgent` to WebDAV/Synology to offload them if desired.

### 🧑‍🏫 Teacher Experience
1. **Create or Import a Lesson Module** - Define objectives, attach slides/media, and configure timers, randomization, and optional leaderboards across the pre/lesson/post segments.
2. **Launch a Live Session** - Tap **Start Live Delivery** to generate a short-lived class code (e.g., `845 209`). Mirror your device to a projector, TV, or screen-share so everyone sees prompts and leaderboards.
3. **Monitor and Report** - Scores, participation, and learning gains are stored locally. Export PDF or CSV reports to highlight objective mastery and commonly missed concepts.

### 👩‍🎓 Student Experience
1. **Join the Session** – Open the join screen on any phone, tablet, or laptop connected to the same Wi‑Fi or the teacher’s offline hotspot. Enter the class code and nickname or ID.
2. **Play and Engage** – Watch the shared display for questions and respond from personal devices. Scoring balances accuracy and fairness—pre/post diagnostics disable speed bonuses.
3. **Privacy & Reliability** – No mandatory accounts or cloud sync. All data stays on the teacher’s device, enabling fully offline operation for low-connectivity classrooms.

### ✨ Core Capabilities
- **Module Flow:** Pre‑Test → Lesson/Discussion → Post‑Test with parallel forms for diagnostic comparison.
- **Delivery Modes:** Live classroom sessions and asynchronous assignments.
- **Reports & Exports:** Automatic Pre vs Post analytics, objective mastery insights, and PDF/CSV exports.
- **Engagement:** Optional leaderboards, podium celebrations, and lightweight gamification (avatars/badges).
- **Localization:** Tagalog labels across key teacher-facing surfaces.

---

## 🧩 Question Types
| Purpose | Type | Description |
| --- | --- | --- |
| **Test Knowledge** | Multiple Choice | 2–4 options with one or more correct answers. |
|  | True / False | Quick factual checks with binary choices. |
|  | Numeric Entry | Students type numeric answers with tolerance settings. |
|  | Matching | Pair related terms, formulas, or definitions. |
|  | Type Answer | Short response (≤20 characters) with variant matching. |
|  | Puzzle | Arrange steps or ideas in the correct order. |
|  | Slider | Indicate a numeric value or confidence level. |
| **Gather Opinions** | Poll | Collect quick opinions or gauge understanding. |
|  | Word Cloud | Players submit one word; popular answers grow larger. |
|  | Open-Ended | Invite longer reflections or feedback. |
|  | Brainstorm | Collaborative idea generation with optional voting. |

---

## 🕹️ Live Quiz Flow (Local-First Design)
1. **Teacher Hosts the Session** – Run the module locally; all timing, scoring, and media playback stay on the teacher device.
2. **Students Join via Local Network** – Devices auto-discover the session across LAN or the teacher’s hotspot—no internet required.
3. **Play, Score, Reflect** – After each question, the leaderboard updates live. Responses feed into instant analytics comparing Pre vs Post performance.

### Why Local-First Matters
- **Reliability:** Sessions stay stable even if campus internet drops; submissions queue until confirmed.
- **Privacy:** Student data remains on the teacher device unless optional sync is enabled.
- **Low Bandwidth:** Requires only lightweight LAN traffic, ideal for resource-constrained schools.

### 🧠 Example Module Flow
**Module:** Simple Interest and Compound Interest (Objectives: LO1, LO2, LO3)

`Pre-Test → Lesson Slides → Interactive Quizzes → Post-Test → Report`

Each stage feeds into the performance report, highlighting mastery per objective and growth between pre and post assessments.

---

## 🧑‍🏫 Teacher Setup Checklist
1. **Create the Classroom or Subject Shell** – Tag the section, subject, and schedule so reports group correctly.
2. **Author the Lesson Package** – Define objectives, attach slide decks or reference files, and align each objective with the right assessment items.
3. **Build the Pre-Test** – Select or import diagnostic questions, tuning timers and feedback rules for fairness.
4. **Plan the Guided Lesson** – Sequence slides, worked examples, and mini activities; attach external media for live presentation or assignments.
5. **Assemble the Post-Test** – Create a parallel assessment to measure learning gains on the same objectives.
6. **Configure Interactive Activities** – Blend quizzes, polls, and brainstorms for synchronous or asynchronous engagement.

> Keeping everything inside a single module lets teachers reuse the package for live sessions or assignments while automatically tracking pre/post gains.

---

## 🏗 Tech Stack
- **Language:** Kotlin (JDK 17)
- **UI:** Jetpack Compose (Material 3)
- **Navigation:** Navigation‑Compose
- **Architecture:** MVVM + Clean Architecture (Domain/Data/UI), unidirectional data flow
- **DI:** Hilt (optional; interface-driven, can be swapped later)
- **Async:** Coroutines + Flow
- **Persistence:** Room (local-first storage of modules, attempts, results)
- **Serialization:** Kotlinx Serialization
- **Networking (optional):** Retrofit/OkHttp (future cloud sync)
- **Export:** Android Print/PdfDocument; CSV writer
- **Testing:** JUnit, MockK, Turbine, Compose UI tests
- **Quality:** ktlint, Detekt
- **Build:** Gradle (Kotlin DSL)

---

## 📂 Project Structure (suggested)
```
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml
    java/com/<org>/<app>/
      App.kt
      MainActivity.kt
      navigation/          # Nav graph + routes
      ui/theme/            # Material 3 theme
      feature/
        pretest/           # Pre-test UI + logic
        lesson/            # Slides, worked examples, checks
        posttest/          # Post-test UI + logic
      domain/
        model/             # Module, Item, Objective, Attempt, Report...
        usecase/           # BuildModule, ScoreAttempt, BuildReports...
      data/
        local/             # Room entities/DAO
        repo/              # Repositories
        remote/            # Retrofit (optional)
  proguard-rules.pro
```

---

## ▶️ Getting Started
1. **Open in Android Studio** (Ladybug or newer) and let Gradle sync.
2. Ensure **JDK 17**.
3. Run the **app** configuration on an emulator (API 34/35) or device.

```bash
# Quality & tests
./gradlew ktlintCheck detekt test connectedAndroidTest
```

> If you downloaded the provided starter zip, drop these files (**README.md**, **AGENTS.md**) in the project root.

---

## ⚙️ Optional Local Config
Create `app/src/main/assets/app-config.json` to toggle features:
```json
{
  "leaderboardEnabledByDefault": false,
  "feedbackMode": "after-section",
  "locale": "en-PH",
  "cloudSync": false
}
```

---

## 📊 Core Data Models (simplified)
```kotlin
data class Module(
  val id: String,
  val subject: String = "G11 General Mathematics",
  val topic: String,
  val objectives: List<String>,   // e.g., ["LO1","LO2","LO3"]
  val preTest: Assessment,
  val lesson: Lesson,
  val postTest: Assessment,
  val settings: ModuleSettings
)

data class Assessment(
  val id: String,
  val items: List<Item>,
  val timePerItemSec: Int = 60
)

sealed interface Item {
  val id: String
  val objective: String
}

data class NumericItem(
  override val id: String,
  override val objective: String,
  val prompt: String,
  val answer: Double,
  val tolerance: Double = 0.01,
  val explanation: String
) : Item
```
See **AGENTS.md** for agent contracts and flows.

---

## 🔒 Privacy & Classroom Safety
- No student accounts required by default
- Local-first storage, minimal PII
- Optional cloud sync can be disabled

---

## 🧩 Roadmap
- **v1:** Module builder, delivery (live/assignment), reports (Pre vs Post), exports, light gamification
- **v2:** Team mode, richer analytics, item-bank authoring on device, cloud sync, teacher portal

---

## 🤝 Contributing
1. Fork → feature branch
2. Add tests where sensible
3. Run `ktlint` and `detekt`
4. Open a PR with screenshots and a concise description

---

## 📜 License
MIT (or school-specific). Add a `LICENSE` file.
