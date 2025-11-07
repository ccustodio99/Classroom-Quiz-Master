# Classroom Quiz Master

Classroom Quiz Master is a LAN-first quiz experience built with Kotlin, Jetpack Compose, Room, WorkManager, Hilt, and Firebase. Teachers host realtime sessions that keep running on a school LAN while WorkManager syncs data to Firebase when a WAN connection returns. Students can join over Wi-Fi as guests and still earn points, leaderboards, and take-home reports.

## Feature Highlights

- **Dual personas** – Teacher authentication (Email or Google) coexists with anonymous student guests protected by profanity filtering, nickname length enforcement, and automatic suffix dedupe.
- **LAN-first gameplay** – Embedded Ktor WebSocket server plus NSD (`_quizmaster._tcp.`) discovery keep answer latency low even without internet. Payloads are capped and gated by join tokens.
- **Local-first persistence** – Room stores sessions/participants/attempts with an op-log table and explicit migrations, so upgrades preserve data.
- **Resilient sync** – `FirestoreSyncWorker` (scheduled with battery/network constraints) drains the op-log; App Check + StrictMode guard bad actors during development.
- **Firebase surface area** – Firestore, Storage, Analytics, Crashlytics, and App Check are wired via the BoM. Cloud Functions cover assignment scoring and CSV exports.
- **Performance & quality** – Baseline profile file, dedicated macrobenchmark module, CI with lint/ktlint/detekt/unit/instrumented tests, and tightened ProGuard rules.
- **Student-safe sync** – Anonymous guests keep attempts local/LAN-only; only authenticated teachers enqueue Firestore writes so security rules can deny score tampering.
- **Nearby fallback stub** – When NSD times out, a helper surfaces guidance (and future Wi-Fi Direct hooks) so classes can still scan QR/manual URLs.

## Architecture at a Glance

```
app/
  data/        // Room, DataStore, Firebase, LAN server+client, repositories
  domain/      // Models, repository interfaces, use cases
  ui/          // Compose navigation plus teacher & student flows
  sync/        // WorkManager worker + scheduler wiring
  util/        // Nickname policy, scoring helpers, profanity filter
functions/     // Firebase Cloud Functions (TypeScript)
security/      // Firestore + Storage rules
macrobenchmark // Macrobenchmark + startup timing tests
.github/       // GitHub Actions workflows
```

- **MVVM/UDF** keeps Compose screens stateless. ViewModels expose `StateFlow` and use `SubmitAnswerUseCase`, `JoinSessionUseCase`, `ScoreAttemptUseCase`, and `SyncPendingOpsUseCase`.
- **LAN stack**: `LanHostManager` manages the embedded WebSocket server, `NsdHelper` advertises/discovers services, `LanClient` reconnects with exponential backoff, and `QuizMasterLanHostService` keeps hosting in a foreground service.
- **Safety**: `NicknamePolicy` enforces length/profanity, `LanHostManager` rejects oversized payloads, `FirebaseAppCheck` (Play Integrity) installs on startup, and Crashlytics logging toggles per build type.

## Getting Started

1. **Install tooling** – Android Studio Koala (or newer), Android SDK 34, JDK 17, Node.js 18 for Functions, Firebase CLI.
2. **Clone & open** – `git clone <repo>` then `File > Open` inside Android Studio or run `./gradlew tasks` from a shell.
3. **Firebase setup**
   - Enable Email/Password + Google auth, Firestore, Storage, Analytics, Crashlytics, and App Check.
   - Download `google-services.json` into `app/`.
   - Provide the Web client ID in `app/src/main/res/values/strings.xml` (`google_web_client_id`).
   - Deploy rules: `firebase deploy --only firestore:rules,storage:rules`.
   - Deploy Cloud Functions from `functions/`: `npm install && npm run deploy`.
4. **Configure LAN demo**
   - Ensure teacher & student devices share the same Wi-Fi (or tethered hotspot).
   - Teacher: log in, create/seed a quiz, tap **Launch Live** to start NSD advertising and the embedded server.
   - Student: tap **Join via LAN**, choose a discovered host or paste the `ws://host:port/ws?token=...` URI/QR, verify nickname validation, and join the lobby.
   - Hosting runs inside a foreground service; when WAN returns, WorkManager syncs attempts, participants, and assignments.

### Debugging Tips

- Use `adb shell am startservice ...QuizMasterLanHostService` to restart the LAN host quickly.
- `adb shell dumpsys wifi` confirms NSD availability (location permission required on Android 13+).
- Timber logging plus StrictMode (debug only) surfaces slow I/O or leaked resources early.

## Testing & CI

| Command | Purpose |
| --- | --- |
| `./gradlew clean ktlintCheck detekt lint test assembleDebug` | Default CI verification (also run by `.github/workflows/android-ci.yml`). |
| `./gradlew connectedDebugAndroidTest` | Compose instrumentation tests (executed on the emulator job). |
| `./gradlew :macrobenchmark:connectedCheck` | Macrobenchmark & startup timing validation. |
| `./gradlew generateBaselineProfile` | Refreshes `app/src/main/baseline-prof.txt` using macrobenchmarks. |

CI now runs two jobs: **build** (lint/style/tests/assemble) and **instrumentation** (emulator + connected tests + macrobenchmark).

## Firebase Security & Cloud

- Firestore rules (`security/firestore.rules`) gate session ownership, block students from writing score fields, respect `lockAfterQ1`, and restrict participant/attempt writes to the authenticated student.
- Storage rules keep uploads teacher-only under `quiz_media/`, while students retain read access for classroom devices.
- Cloud Functions (`functions/src/index.ts`) expose:
  - `scoreAssignmentAttempt` – Callable scoring for assignments.
  - `exportSessionReport` – Callable CSV export stored at `reports/<sessionId>/session-report-<timestamp>.csv` with a signed download URL.

## LAN-only Demo Script

1. Teacher opens **Teacher Home**, taps **Create Quiz**, and saves a Fractions sample.
2. Tap **Launch Live**. The foreground host service starts, NSD advertises `_quizmaster._tcp.`, and the QR join code appears.
3. Students tap **Join via LAN**, pick the discovered host, enter nicknames (validation feedback inline), and join the lobby.
4. Host sees participants accrue in real time; leaderboard cards show sanitized names and points.
5. Answer submissions travel over LAN (payloads clamped to 2 KB). When WAN reconnects, the op-log syncs to Firestore.
6. Teachers can call the CSV export function to share results with guardians or LMS tooling.

## Configuration Notes

- Keep secrets (Firebase tokens, App Check debug providers, optional API keys) in `local.properties` or encrypted Gradle properties.
- `benchmark` build type mirrors `release` but uses debug signing for macrobenchmarks; baseline profiles live in `app/src/main/baseline-prof.txt`.
- `network_security_config` only allows local cleartext; do not broaden it for production internet endpoints.

## License

Licensed under the MIT License – see [LICENSE](LICENSE).
