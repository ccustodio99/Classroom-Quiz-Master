# Classroom Quiz Master (Android • Kotlin)

A fun, teacher‑friendly Android app that turns **Grade 11 General Mathematics** lessons into interactive modules with **Pre‑Test → Discussion → Post‑Test** and clear **learning‑gain** reports. Built with **Kotlin** for Android.

---

## ✨ Core Features
- **Module Flow (G11 Gen Math):** Pre‑Test → Lesson/Discussion → Post‑Test with parallel forms
- **Item Types:** Multiple‑choice, True/False, Numeric entry, Matching; media support (images/audio/video)
- **Delivery Modes:** Live (class code + leaderboard toggle) or Assignment (homework)
- **Reports:** Auto‑scoring, Pre vs Post comparison, objective mastery, commonly missed items; export PDF/CSV
- **Student UX:** Join via code + nickname/ID (no account), gentle timer + sfx, feedback explanations
- **Teacher UX:** Module builder, progress monitor, optional gamification (avatars/badges), printable summaries

> Localized labels (Tagalog) available for teachers: *Pagsusulit Bago ang Aralin*, *Talakayan/Aralin*, *Pagsusulit Pagkatapos ng Aralin*, *Pag‑angat ng Marka*.

---

## 🏗️ Tech Stack
- **Language:** Kotlin (JDK 17)
- **UI:** Jetpack Compose (Material 3)
- **Architecture:** MVVM + Clean Architecture (Domain / Data / UI), Unidirectional data flow
- **DI:** Hilt
- **Async:** Kotlin Coroutines + Flow
- **Persistence:** Room (local item bank, attempts, results)
- **Serialization:** Kotlinx Serialization
- **Networking (optional):** Retrofit/OkHttp (for future cloud sync)
- **PDF/CSV Export:** Android Print / PdfDocument + CSV via simple writer
- **Testing:** JUnit4/5, Turbine (Flow), MockK, Compose UI testing
- **Lint/Style:** ktlint, Detekt
- **Build:** Gradle (Kotlin DSL)

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
- **Android Studio**: Ladybug or newer
- **Android Gradle Plugin**: 8.x
- **Kotlin**: 2.0+
- **Min SDK**: 24  
- **Target SDK**: 34/35

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
- No student accounts required; join via code + nickname/ID
- Local‑first storage; optional future cloud sync
- Minimal personally identifiable information (PII) by default

---

## 🧩 Roadmap (v1 → v2)
- v1: Module builder, delivery, reports, exports, light gamification
- v2: Item bank authoring on device, team mode, advanced analytics, cloud sync, teacher portal

---

## 🤝 Contributing
1. Fork & create a feature branch
2. Write tests where sensible
3. Run linters (`ktlint`, `detekt`)
4. Open a PR with a clear description & screenshots

---

## 📜 License
MIT (or school‑specific license). Add a `LICENSE` file.
