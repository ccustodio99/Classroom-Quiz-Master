# Classroom Quiz Master

Classroom Quiz Master is a LAN-first formative assessment app built with Kotlin, Jetpack Compose, Room, WorkManager, Hilt, and Firebase. Teachers host secure quizzes over the local network so that classes can keep learning even when the WAN is unreliable; when connectivity returns a WorkManager-backed op-log mirrors results to Firestore.

## Feature highlights

- **Dual personas** – Teacher authentication (email/password or Google) coexists with anonymous student guests. Nickname policy, profanity filtering, and salted suffixes prevent collisions or unsafe names.
- **LAN-first gameplay** – A Ktor WebSocket server advertises through Android NSD (`_quizmaster._tcp.`). Payloads are capped, token-gated, and acknowledged under 150 ms median on loopback.
- **Local-first persistence** – Room stores teachers, classrooms, quizzes, sessions, participants, attempts, assignments, submissions, and LAN metadata. Explicit migrations and schema JSON keep upgrades deterministic.
- **Resilient sync** – `FirestoreSyncWorker` drains the op-log under network constraints, with exponential backoff and manual triggers. Sync success timestamps live in DataStore.
- **Firebase surface area** – Auth, Firestore, Storage, Functions, Analytics, and Crashlytics ship through the BoM. Cloud Functions score assignments idempotently and generate signed CSV/PDF reports.
- **Quality gates** – Baseline profile, macrobenchmark module, detekt/ktlint/lint, Robolectric + Compose instrumentation tests, Firebase emulator tests, and GitHub Actions automation keep regressions out.

## Project layout

```
app/
  data/        // Room, repositories, LAN transport, Firebase adapters
  domain/      // Models, repository contracts, use cases
  sync/        // WorkManager worker & scheduler
  ui/          // Compose navigation plus teacher & student flows
  util/        // Join codes, scoring, profanity & nickname policy
functions/     // Firebase Cloud Functions (TypeScript + Jest tests)
security/      // Firestore & Storage security rules
macrobenchmark // Baseline profile & startup benchmarks
.github/       // CI workflows
```

## Requirements

- Android Studio Koala (AGP 8.7.x) with Android SDK 34
- JDK 17
- Node.js 18 + Firebase CLI for Functions & rules tests
- Kotlin 1.9.25 with Compose Material 3

## Getting started

1. **Clone and open** – `git clone <repo>` then `./gradlew tasks` or open the project in Android Studio.
2. **Configure Firebase**
  - Enable Email/Password, Google, anonymous auth, Firestore, Storage, Crashlytics, and Analytics.
   - Place `google-services.json` inside `app/`.
   - Supply the OAuth web client ID via `google_web_client_id` if using Google sign-in (see `strings.xml`).
   - Deploy security rules: `firebase deploy --only firestore:rules,storage:rules`.
   - Deploy Cloud Functions: `npm --prefix functions ci && npm --prefix functions run build && firebase deploy --only functions`.
3. **LAN demo setup**
   - Put teacher & student devices on the same Wi-Fi or hotspot. Location permission must be granted on Android 13+ for NSD.
   - Teacher: Sign in, create a quiz, then tap **Launch Live** to advertise the `_quizmaster._tcp.` service and spin up the foreground host service.
   - Students: Choose **Join via LAN**, pick the discovered host or enter the join code, validate the nickname, and join the lobby.
   - Hosting stays active in a foreground service; when WAN returns, WorkManager mirrors pending operations.

## Testing matrix

| Layer | Command | Notes |
| --- | --- | --- |
| Unit (JUnit5 + Robolectric) | `./gradlew testDebugUnitTest` | Scoring, nickname policy, join-code generation, profanity filtering, Room DAOs, migrations, op-log dedupe, LAN loopback latency, Student join ViewModel. |
| Compose instrumentation | `./gradlew connectedDebugAndroidTest` | Host controls (Start/Reveal/Next), leaderboard accessibility, timer descriptions, student join validation. |
| Managed device regression | `./gradlew pixel6Api34DebugAndroidTest` | Runs the Compose instrumentation suite on a Gradle-managed Pixel 6 API 34 virtual device. |
| Firebase emulator (rules/functions) | `npm test` in `functions/` | Jest tests cover Firestore security rules plus `scoreAttempt` and `exportReport` Cloud Functions via dependency injection. |
| Cloud Functions build | `npm run build` in `functions/` | TypeScript compilation check. |

The CI workflow (`.github/workflows/android-ci.yml`) orchestrates the commands above on every push or pull request. Artifacts include lint reports, unit-test results, the debug APK, and Cloud Functions coverage.

## LAN runbook

1. Teacher starts the LAN host. `LanHostForegroundService` advertises NSD, binds the Ktor WebSocket server, and surfaces the QR join code.
2. Students connect via `LanClient`. Payloads are serialized JSON (`WireMessage`) under 16 KB.
3. Attempts are scored locally using `ScoreCalculator` and persisted with an op-log entry when the teacher is authenticated.
4. When the network returns, `FirestoreSyncWorker` flushes pending attempts. Success timestamps update `AppPreferencesDataSource` so the UI can show sync freshness.
5. Teachers can end the session, which shuts down NSD, clears LAN metadata, and stops the foreground service.

## Accessibility & localization

- String resources cover high-contrast, large text, TalkBack hints, and major teacher/student flows (`values/strings.xml`).
- Tagalog translations live in `values-tl/strings.xml`; Compose surfaces TalkBack-friendly labels for timers, buttons, and leaderboard rows.

## Firebase rules & cloud functions

- `security/firestore.rules` enforces teacher ownership, anonymous student permissions, and blocks tampering with score fields.
- `functions/src/index.ts` exports:
  - `scoreAttempt` – Firestore trigger that validates attempts, applies scoring modes (`best`, `last`, `avg`), respects attempt caps, and updates submissions idempotently.
  - `exportReport` – Callable function that aggregates attempts, computes accuracy, emits CSV/PDF artifacts with signed URLs, and limits access to the owning teacher.
- Jest tests (`functions/__tests__`) load the Firestore rules with `@firebase/rules-unit-testing` and execute the callable/trigger logic with dependency injection.

## Helpful commands

- Full verification: `./gradlew clean ktlintCheck detekt lintRelease lintDebug testDebugUnitTest assembleDebug`
- Compose instrumentation: `./gradlew connectedDebugAndroidTest`
- Gradle managed device: `./gradlew pixel6Api34DebugAndroidTest`
- Firebase emulators (optional manual run): `firebase emulators:start --only firestore,functions,storage`
- Cloud Functions tests: `npm --prefix functions test`

## License

Licensed under the MIT License. See [LICENSE](LICENSE).
