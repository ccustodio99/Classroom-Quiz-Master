# Classroom Quiz Master (Kotlin Android)

Classroom Quiz Master is a Kotlin-based **Android mobile application** that simulates a Grade 11 mathematics classroom workflow end-to-end: build a module, run a live pre-test, walk through the lesson, deliver a post-test, and export analytics-driven reports. The app follows the agent contracts in [`AGENTS.md`](AGENTS.md) and is designed to run on phones or tablets—no command-line interface is required.

---

## ✨ Core Capabilities
- **Module Builder Agent** – Validates objectives, lesson coverage, and parallel pre/post assessments before publishing a module to Room.
- **Live Session Agent** – Hosts teacher-led sessions, lets students join via nickname/QR, and tracks real-time scoring and pacing controls.
- **Assessment Agent** – Starts and submits attempts, automatically scores responses (MCQ, True/False, Numeric, Matching), and synchronizes timing with the session.
- **Lesson Agent** – Presents the lesson slide deck and revealable solution steps aligned to objectives for the module.
- **Assignment Agent** – Schedules homework availability windows with retry policies and enforces submission rules.
- **Analytics & Reports** – Aggregates pre/post performance, computes objective gains, and exports class/student PDF + CSV reports from the device.
- **Gamification** – Surfaces “Top Improver” and “Star of the Day” badges based on post-test growth.

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
sdk.dir=C:\\Users\\you\\AppData\\Local\\Android\\Sdk
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
