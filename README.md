# Classroom LMS (Android)

This repository now targets the **multi-module Android project under `android/`**.  
The previous prototype that lived in the root `app/` directory is retained only for reference and is **no longer part of the active build**.

## Repository Layout

```
android/               # Current Gradle project (multi-module, offline-first LMS)
  app/                 # Application module
  core/, feature/      # Shared domain, database, network, sync, UI feature modules
  firebase/            # Emulator configuration + Firestore rules
  docs/                # Architecture & verification notes
legacy-app/            # (optional) relocate the old sources here if you still need them
app/                   # Legacy sources kept for archival purposes – not built
AGENTS.md              # Functional agent contracts
```

> ⚠️ The root `settings.gradle.kts` now includes the `android/` build. Running root Gradle tasks will delegate to that project.

## Building & Running

All tooling for the new project lives inside `android/`.

1. **Use the bundled JDK 17** located at `android/.jdk/jdk-17.0.10`.
2. **Use the checked-in Gradle distribution** at `android/.gradle-dist/gradle-8.7`.
3. Run the build from the repository root:

```powershell
cd E:\SynologyDrive\MITStudies\Projects\Classroom-Quiz-Master
set JAVA_HOME=%CD%\android\.jdk\jdk-17.0.10
android\.gradle-dist\gradle-8.7\bin\gradle.bat -p android clean assembleDebug
```

```bash
# macOS / Linux
cd /path/to/Classroom-Quiz-Master
export JAVA_HOME="$PWD/android/.jdk/jdk-17.0.10"
$PWD/android/.gradle-dist/gradle-8.7/bin/gradle -p android clean assembleDebug
```

Android Studio (Giraffe / Ladybug or newer) can also open the `android/` directory directly; it will detect the embedded JDK and Gradle wrapper automatically.

## Continuous Integration

CI runs inside `.github/workflows/android.yml`.  
It checks out the repo, installs Temurin JDK 17, validates the Gradle wrapper, then executes:

```
./gradlew ktlintCheck detekt testDebugUnitTest assembleDebug
```
with the working directory set to `android/`. The resulting debug APK is uploaded as a workflow artifact.

## Documentation

Additional design notes live in `android/docs/`:

- `ARCHITECTURE.md` – module decomposition, data flow, offline sync, live session signaling.
- `VERIFICATION.md` – checklist for manual testing, screenshots (to be added).

Each feature module also contains its own README or KDoc where appropriate.

## Working With the Legacy Source

The legacy implementation remains in the root `app/` folder for historical reference.  
It is **not** included in any Gradle build, CI job, or Android Studio project configuration.  
If you still need it, consider moving it to `legacy-app/` locally to keep it separated from the active codebase.

---

For a deeper dive into the functional agent responsibilities (classroom, classwork, assessment, live sessions, etc.), see **`AGENTS.md`** in the project root.
