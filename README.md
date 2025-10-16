# Classroom Quiz Master (Kotlin Android)

Classroom Quiz Master is a Kotlin-based **Android mobile application** that simulates a Grade 11 mathematics classroom workflow end-to-end: build a module, run a live pre-test, walk through the lesson, deliver a post-test, and export analytics-driven reports. The app follows the agent contracts in [`AGENTS.md`](AGENTS.md) and is designed to run on phones or tablets—no command-line interface is required.

---

## ✨ Core Capabilities
- **Module Builder Agent** – Creates and validates module packages, ensuring alignment between objectives, lessons, and assessments before saving to the local database.
- **Item Bank Agent** – Manages the collection of questions, supporting various types (MCQ, T/F, Numeric), difficulty tagging, and sharing via JSON.
- **Live Session Agent** – Hosts real-time, teacher-led sessions where students can join via nickname or QR code, with controls for pacing and live scoring.
- **Assignment Agent** – Schedules modules as homework with defined availability windows and submission policies.
- **Assessment Agent** – Delivers pre-tests and post-tests, handles submission, and performs automatic scoring for all question types.
- **Lesson Agent** – Presents lesson materials, including slide decks and interactive examples with revealable solutions.
- **Scoring & Analytics Agent** – Aggregates pre-test vs. post-test results to compute learning gains and identify areas of difficulty.
- **Report Export Agent** – Generates and shares detailed class and student reports in both PDF and CSV formats.
- **Gamification Agent** – Awards badges for achievements like "Top Improver" and "Star of the Day" based on performance growth.
- **Sync Agent (Optional)** – Provides a mechanism for future cloud synchronization of application data, disabled by default.

---

## 🏗️ Project Layout
```
app/
  src/
    main/
      AndroidManifest.xml
      java|kotlin/com/acme/quizmaster/
        agents/        # Agent interfaces + implementations
        data/          # Local Room database + repositories
        domain/        # Core models (Module, Assessment, Attempt, Reports...)
        ui/            # Android Activities/Fragments/Compose screens for teacher + student flows
        util/          # Scoring helpers
      res/             # Layouts, drawables, strings, themes
    androidTest/       # Instrumented tests covering end-to-end agent flows on device
    test/              # JVM unit tests
```

---

## ▶️ Building & Running the App
Before opening the project, make sure the Android SDK location is configured. Either export the `ANDROID_HOME` environment
variable or create a `local.properties` file at the project root that points to your SDK installation:

```
sdk.dir=C:\Users\you\AppData\Local\Android\Sdk
```

The repository's `.gitignore` excludes `local.properties`, so each developer can reference their own SDK path without breaking
the build for others.

1. Open the project in **Android Studio Flamingo or newer** (or import via the included Gradle wrapper).
2. Allow Gradle sync to complete so dependencies and Room schemas are generated.
3. Connect an Android device or start an API 34 emulator.
4. Click **Run ▶** on the `app` configuration.

When the app launches, teachers can assemble a module, guide students through pre-test → lesson → post-test flows, and share reports from app-private storage using Android share intents.

---

## 🧪 Testing
```bash
./gradlew test
./gradlew connectedAndroidTest
```
Unit tests validate module validation, live session tracking, scoring, analytics, reporting, and gamification behaviour using the in-memory repositories. Instrumented tests execute the same flows on an emulator/device to confirm UI and database integration. Ensure `adb` is available and an emulator is running before invoking `connectedAndroidTest`.

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
2. Update or add unit *and* instrumented tests for new behaviour.
3. Run `./gradlew test` and `./gradlew connectedAndroidTest` before submitting a PR (Android Studio bundles a compatible Gradle wrapper).
4. Describe the agent responsibilities and UI flows touched by the change in the PR summary.

---

## 📜 License
MIT (add a `LICENSE` file if you intend to redistribute).
