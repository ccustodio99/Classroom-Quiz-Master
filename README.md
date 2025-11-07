# Classroom Quiz Master

Classroom Quiz Master is a LAN-first quiz experience built with Kotlin, Jetpack Compose, Room, WorkManager, Hilt, and Firebase. Teachers host realtime sessions that continue to function on an offline school network while syncing to Firebase when connectivity is available. Students can join over Wi‑Fi without accounts and receive stars, leaderboards, and reports.

## Feature Highlights

- **Dual personas** – Teacher authentication (Email/Google) plus offline Guest students with nickname protection.
- **LAN-first gameplay** – Embedded Ktor WebSocket server, NSD advertising (`_quizmaster._tcp.`), and client discovery for sub‑150 ms answer roundtrips.
- **Local-first persistence** – Room entities for session/participant/attempt state plus op-log for offline sync via WorkManager.
- **Hardened auth & QR join** – Token-gated LAN hosts display QR URLs (`ws://ip:port/ws?token=…`) and sanitize nicknames; teachers authenticate via email/password or Google while students remain anonymous.
- **Cloud sync** – Firestore mirrors quizzes, sessions, attempts, assignments; Storage hosts rich media; Functions score homework.
- **Accessibility & safety** – High-contrast toggle, large text preference, profanity filter, leaderboard privacy controls, guardian-friendly exports.
- **DevEx** – Kotlin DSL build scripts, Compose Material 3, JUnit5 + Robolectric + Compose instrumentation tests, GitHub Actions CI.

## Architecture at a Glance

```
app/
  data/           // Room, DataStore, Firebase, LAN server+client, repositories
  domain/         // Models, repositories interfaces, use cases (score/join/sync)
  ui/             // Compose navigation, teacher & student screens, components
  sync/           // WorkManager worker + scheduler
functions/        // Firebase Cloud Functions (TypeScript) for assignment scoring
security/         // Firestore & Storage rules
.github/workflows // CI pipeline
```

- **MVVM + UDF**: ViewModels expose immutable `UiState` with Flow/StateFlow.
- **LAN**: `LanHostManager` spins up a Ktor server in-app; `NsdHelper` registers/discovers services; `LanClient` streams WebSocket JSON `WireMessage`s.
- **Data Sync**: `FirestoreSyncWorker` drains the op log when online; `SyncScheduler` enqueues periodic work from the Application.
- **Use cases**: `ScoreAttemptUseCase`, `SubmitAnswerUseCase`, `JoinSessionUseCase`, `SyncPendingOpsUseCase`.

## Getting Started

1. **Install tooling**: Android Studio Koala (or newer), Android SDK 34, JDK 17, Node.js 18 for Functions.
2. **Clone & open**: `git clone <repo>` → `File > Open` in Android Studio.
3. **Firebase setup**:
   - Create a Firebase project, enable Email/Password + Google Auth, Firestore, Storage, Analytics, Crashlytics, App Check.
   - Download `google-services.json` into `app/`.
   - Set `google_web_client_id` in `app/src/main/res/values/strings.xml` to the Web client ID from Firebase Console (used for Google Sign-In).
   - Enable Firestore offline persistence (already toggled in `FirebaseModule`).
   - Deploy security rules: `firebase deploy --only firestore:rules,storage:rules`.
   - (Optional) Deploy Cloud Functions: from `/functions` run `npm install` then `npm run deploy`.
4. **Gradle sync**: Studio will fetch dependencies. CLI alternative: `./gradlew assembleDebug`.
5. **LAN demo**:
   - Put two devices/emulators on the same Wi‑Fi or hotspot.
   - Teacher device: log in, seed sample quiz from home screen, tap *Launch Live*.
   - Student device: choose *Join via LAN*, wait for NSD discovery, tap host card.
   - If discovery stalls, tap the QR icon on the host screen and scan the `ws://` token from a student device or paste it manually.
   - Answers flow via local WebSocket even if Internet drops; reconnecting to WAN will sync attempts.

### Debugging Tips

- Toggle verbose LAN logs with `Timber`.
- Use `adb shell am startservice ...QuizMasterLanHostService` to restart hosting when testing.
- `adb shell dumpsys wifi` confirms NSD availability; enable location for Wi‑Fi Direct fallback.

## Testing & CI

| Command | Description |
| --- | --- |
| `./gradlew test` | JVM & JUnit5 + Robolectric unit tests (scoring, join code, profanity, viewmodel flows). |
| `./gradlew connectedDebugAndroidTest` | Compose instrumentation including Play screen smoke test. |
| `./gradlew lint` | Static analysis & Compose metrics. |
| `./gradlew ktlintCheck` | Kotlin style gate aligned with Android + Compose conventions. |
| `./gradlew detekt` | Structural lint (coroutines/DI/regression checks). |
| `./gradlew assembleDebug` | Build debug APK (also runs in GitHub Actions). |

The GitHub Actions workflow (`.github/workflows/android-ci.yml`) executes lint, unit tests, and assemble on every push/PR to `main`.

## Firebase Security

- Firestore rules (`security/firestore.rules`) ensure teachers CRUD only their own quizzes/sessions. Students can read active sessions and write only their participant/submission documents; hosts alone can set scoring fields.
- Storage rules (`security/storage.rules`) restrict writes under `quiz_media/` to authenticated teachers while keeping downloads open for class devices.

## Cloud Functions

`functions/src/index.ts` exposes `scoreAssignmentAttempt`, a callable function that validates assignment answers, writes submission stats, and returns the computed score. Extend this entry point with CSV/PDF export jobs or guardian notifications as needed.

## LAN-only Demo Script

1. From Teacher Home, use *Create Quiz* to enter the Fractions demo (or import your own).  
2. Tap *Launch Live* on that quiz.  
3. The host service starts the Ktor WebSocket server, registers via NSD, and surfaces the join code for LAN guests.  
4. Students tap *Join via LAN*, pick the discovered host, enter nicknames (profanity-filtered), and land in the lobby.  
5. Teacher starts the session; answer submissions stay local-first with instant leaderboard updates.  
6. Once teacher reconnects to the Internet, WorkManager sync pushes the op log to Firestore (view progress via connectivity banner).  
7. Reports screen surfaces averages, commonly missed questions, and CSV/PDF export queues (uploads resume with connectivity).

## Configuration Notes

- Place App Check provider tokens, Firebase project ID, or optional AI keys inside `local.properties` or encrypted Gradle props; never commit secrets.
- `BuildConfig` exports `_quizmaster._tcp.` service type, host, and default port (can be overridden via `gradle.properties` per flavor).
- `network_security_config` allows LAN `ws://` traffic; do not enable for production outside trusted networks.

## License

Licensed under the MIT License – see [LICENSE](LICENSE).
