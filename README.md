# Classroom Quiz Master (Kotlin Android • Jetpack Compose)

A teacher‑friendly **Kotlin Android** mobile app that turns **Grade 11 General Mathematics** lessons into interactive, measurable modules with a **Pre‑Test → Discussion → Post‑Test** flow and clear learning‑gain reports.

> **Platform:** Android (Kotlin) • **UI:** Jetpack Compose (Material 3) • **Arch:** MVVM + Clean Architecture • **Min SDK:** 24 • **Target SDK:** 34/35

---

## ✨ Features
- **Module Flow:** Pre‑Test → Lesson/Discussion → Post‑Test (parallel forms for fair comparison)
- **Item Types:** Multiple‑choice, True/False, Numeric entry, Matching; with media (images/audio/video)
- **Delivery Modes:** Live (class code, optional leaderboard) and Assignment (homework)
- **Reports:** Auto‑scoring, **Pre vs Post** comparison, objective mastery, commonly missed items; export **PDF/CSV**
- **Students:** Join via code + nickname/ID (no accounts), gentle timer & feedback explanations
- **Teachers:** Simple module builder, progress monitor, togglable gamification (avatars/badges), printable summaries
- **Localization:** Tagalog labels available for teacher UI

---

## 🕹️ How Live Quizzes Work (Local Network First)
Classroom Quiz Master follows a host/participant model similar to Kahoot! but is optimized for intranet or offline hotspot play so teachers can keep the class engaged even when the wider internet is unreliable.

1. **Teacher Hosts the Session**
   - Build or select a module, then start a live delivery from the app. A short‑lived **Class Code** (e.g., `845 209`) appears on the teacher device.
   - Mirror the teacher screen to the class display (projector, TV, or a screen‑share app) so everyone can see questions, answer reveals, and the leaderboard.
   - All pacing, scoring, and media playback run on the host device; outbound internet calls are optional.

2. **Students Join on the Same LAN**
   - Students open the companion join screen on their phones, tablets, or laptops. When everyone is on the same Wi‑Fi—or the teacher spins up an offline hotspot—device discovery works over the local network broadcast.
   - Learners type the class code and choose a nickname (accounts optional). Rosters can be enforced for attendance.
   - Student devices act as lightweight controllers that send answer choices to the host. Color/shape cues match the shared display.

3. **Play, Score, Celebrate**
   - Questions appear on the shared screen; students respond from their devices. Correctness and response speed award points, while diagnostic pre/post tests automatically disable speed bonuses for fairness.
   - After each question, the leaderboard highlights the top five performers or teams to sustain excitement.
   - A podium animation closes the session. All response data is stored locally for instant reporting—no mandatory cloud sync.

### Why Local‑First Matters
- **Reliability:** Sessions continue even if campus internet drops; answers queue locally until the host confirms receipt.
- **Privacy:** Student nicknames and responses stay on the teacher device unless cloud sync is explicitly triggered.
- **Low Bandwidth Friendly:** Only lightweight LAN traffic is required. Remote challenges remain opt‑in for schools that prefer offline play.

---

## 🏗 Tech Stack
- **Language:** Kotlin (JDK 17)
- **UI:** Jetpack Compose (Material 3)
- **Navigation:** Navigation‑Compose
- **Architecture:** MVVM + Clean Architecture (Domain/Data/UI), unidirectional data flow
- **DI:** Hilt (optional; interface‑driven, can be swapped later)
- **Async:** Coroutines + Flow
- **Persistence:** Room (local‑first storage of modules, attempts, results)
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
        pretest/           # Pre‑test UI + logic
        lesson/            # Slides, worked examples, checks
        posttest/          # Post‑test UI + logic
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
- Local‑first storage, minimal PII
- Optional cloud sync can be disabled

---

## 🧩 Roadmap
- **v1:** Module builder, delivery (live/assignment), reports (Pre vs Post), exports, light gamification
- **v2:** Team mode, richer analytics, item‑bank authoring on device, cloud sync, teacher portal

---

## 🤝 Contributing
1. Fork → feature branch
2. Add tests where sensible
3. Run `ktlint` and `detekt`
4. Open a PR with screenshots and a concise description

---

## 📜 License
MIT (or school‑specific). Add a `LICENSE` file.
