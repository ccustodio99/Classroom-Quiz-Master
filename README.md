# Classroom Quiz Master (Kotlin CLI)

Classroom Quiz Master simulates a Grade 11 mathematics classroom workflow end-to-end: build a module, run a live pre-test, walk through the lesson, deliver a post-test, and export analytics-driven reports. The project follows the agent contracts in [`AGENTS.md`](AGENTS.md) and runs entirely on the JVM so it can be exercised from the command line or in automated tests.

---

## ✨ Core Capabilities
- **Module Builder Agent** – Validates objectives, lesson coverage, and parallel pre/post assessments before persisting.
- **Live Session Agent** – Creates teacher-led sessions, lets students join via nickname, and tracks real-time scoring.
- **Assessment Agent** – Starts and submits attempts, automatically scores responses (MCQ, True/False, Numeric, Matching).
- **Lesson Agent** – Exposes the lesson slide deck aligned to objectives for the module.
- **Assignment Agent** – Schedules homework windows with retry policies and enforces submission rules.
- **Analytics & Reports** – Aggregates pre/post performance, computes objective gains, and exports class/student TXT + CSV reports.
- **Gamification** – Surfaces “Top Improver” and “Star of the Day” badges based on post-test growth.

---

## 🏗️ Project Layout
```
app/
  src/
    main/kotlin/com/acme/quizmaster/
      agents/        # Agent interfaces + implementations
      data/          # In-memory repositories
      domain/        # Core models (Module, Assessment, Attempt, Reports...)
      util/          # Scoring helpers
      Main.kt        # Demonstration entry point
      SampleData.kt  # Helper to seed a sample module
    test/kotlin/...  # Behavioural tests covering the full agent flow
```

---

## ▶️ Running the Demo
```bash
./gradlew run
```
The CLI script builds a sample Linear Functions module, walks through a pre/post test session for two students, schedules an assignment, and exports reports under `app/build/reports/`.

---

## 🧪 Testing
```bash
./gradlew test
```
Unit tests validate module validation, scoring, analytics, reporting, and gamification behaviour using the in-memory repositories.

---

## 🔍 Key Domain Models
```kotlin
data class Module(
    val topic: String,
    val objectives: List<String>,
    val preTest: Assessment,
    val lesson: Lesson,
    val postTest: Assessment,
    val settings: ModuleSettings
)
```
See [`domain/Models.kt`](app/src/main/kotlin/com/acme/quizmaster/domain/Models.kt) for the complete data model.

---

## 🤝 Contributing
1. Fork & create a feature branch.
2. Update or add tests for new behaviour.
3. Run `./gradlew test` before submitting a PR.
4. Describe the agent responsibilities touched by the change in the PR summary.

---

## 📜 License
MIT (add a `LICENSE` file if you intend to redistribute).
