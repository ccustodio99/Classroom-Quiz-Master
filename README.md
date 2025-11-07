# Classroom Quiz Master (Android)

A minimal Android app written in Kotlin + Jetpack Compose that showcases a "role alignment" flow for the Classroom Quiz Master project. The current build focuses on a single screen that lets facilitators, students, or observers pick their workflow before starting a quiz session.

## Highlights

- **Modern stack** - Android Gradle Plugin 8.3, Kotlin 1.9, Material 3, and Jetpack Compose.
- **Composable UI** - `QuizDashboard` demonstrates state handling, previews, and theming.
- **Ready for IDE import** - Includes Gradle wrapper, instrumentation/unit test stubs, and baseline resources/icons.

## Getting Started

1. Install Android Studio Iguana+ (or the latest stable) with the Android SDK 34 platform and JDK 17.
2. Clone this repository and open it in Android Studio (`File > Open`).
3. Let Gradle sync. If you prefer the command line, run `./gradlew tasks` from the project root once JDK 17 is available.
4. Choose a device/emulator running Android 7.0 (API 24) or newer and press **Run**.

## Project Structure

```
Classroom-Quiz-Master/
|- app/                 # Android application module
|  |- src/main/java    # Kotlin sources (MainActivity + Compose UI)
|  |- src/main/res     # Resources, themes, icons, and strings
|  |- src/test         # JVM unit test stubs
|  |- src/androidTest  # Instrumented test stubs
|- gradle/              # Gradle wrapper metadata
|- build.gradle.kts     # Top-level Gradle configuration
|- settings.gradle.kts  # Module inclusion + repositories
```

## Next Ideas

- Hook the role selection to real navigation flows once backend pieces are in place.
- Add persistence (e.g., DataStore) so the previously selected role is remembered.
- Extend the UI with sample quiz cards or live session summaries.
