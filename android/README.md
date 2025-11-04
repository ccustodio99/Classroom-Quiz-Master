# ClassroomLMS Android App

`android/` contains a multi-module Kotlin Android application for **ClassroomLMS**, an offline-first classroom learning management system with Firebase integration and Kahoot-style live sessions.

## Project Structure

```
android/
├── app/                 # Application module
├── core/                # Reusable core libraries (model, common, database, network, sync)
├── feature/             # Feature modules (auth, home, learn, classroom, activity, profile, live)
├── firebase/            # Emulator configuration and security rules samples
├── gradle/              # Version catalog
├── lint/                # ktlint + Detekt configuration
└── docs/                # Architecture and verification notes
```

## Requirements

- Android Studio Giraffe+ (AGP 8.3+)
- JDK 17
- Android SDK Platform 34
- Firebase CLI (for emulator support)

## Firebase Setup

1. Create a Firebase project using the project ID `<FIREBASE_PROJECT_ID>`.
2. Download the real `google-services.json` and place it in `android/app/` (replace the placeholder).
3. Update authentication providers (Email/Password and Anonymous) in Firebase Console.
4. Run emulators locally:
   ```bash
   firebase emulators:start --import=android/firebase --project <FIREBASE_PROJECT_ID>
   ```

## Build & Run

```bash
cd android
echo "sdk.dir=/path/to/Android/Sdk" > local.properties  # or export ANDROID_SDK_ROOT
./gradlew assembleDebug
```

> **Note:** The Gradle wrapper JAR is downloaded on demand. Ensure either `curl` or `wget` is available, plus `unzip` (or `python3` as a fallback) so the wrapper can bootstrap itself automatically.

To launch on an emulator/device, install `app/build/outputs/apk/debug/app-debug.apk`.

## Quality Checks

```bash
./gradlew ktlintCheck
./gradlew detekt
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

## Seed Data

Seed classrooms and questions live in `app/src/main/assets/seed/`. Use the seed loader to bootstrap demo content in offline mode.

## CI/CD

GitHub Actions workflow (`.github/workflows/android.yml`) builds the app, runs tests and linters, and uploads the debug APK artifact.

