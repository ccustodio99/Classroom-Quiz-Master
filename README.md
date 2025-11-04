# Mobile LMS (Kotlin + Firestore)

This repository provides a production-ready Android baseline that maps directly to the **Functional Agents** blueprint in `AGENTS.md`. It is configured for Firebase Firestore (offline-first), Firebase Auth, Storage, Functions hooks, and a LAN/WebRTC live-activity layer.

## Project Layout

```
mobile-lms/
├─ README.md
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
├─ app/
│  ├─ build.gradle.kts
│  ├─ proguard-rules.pro
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     └─ java/com/acme/lms/
│        ├─ LmsApp.kt
│        ├─ di/AppModule.kt
│        ├─ agents/
│        │  ├─ ClassroomAgent.kt (+ impl/)
│        │  └─ …
│        ├─ data/
│        │  ├─ model/ (User, Class, Roster, Classwork, Attempt…)
│        │  ├─ repo/ (AuthRepo, ClassRepo, ClassworkRepo, LiveRepo, AnalyticsRepo)
│        │  ├─ net/lan + net/webrtc (LAN broadcast + WebRTC stubs)
│        │  └─ util/ (Firestore extensions, Time)
│        └─ ui/
│           ├─ MainActivity.kt, navigation/NavGraph.kt
│           ├─ screens/{home,learn,classroom,activity,profile,lesson}
│           └─ viewmodel/{HomeViewModel,…}
├─ firebase/
│  ├─ firestore.rules
│  ├─ firestore.indexes.json
│  ├─ storage.rules
│  └─ emulators.json
├─ functions/ (Firebase Cloud Functions – TypeScript)
└─ scripts/ (CI helpers, Firestore seed)
```

> Firebase configuration files (`google-services.json`, service account secrets) are not committed. Add them locally before building.

## Key Features

- **Jetpack Compose + Material 3** UI with Navigation Compose and MVVM (Hilt-injected view models).
- **Hilt DI** modules wiring every agent to its implementation.
- **Firestore offline persistence** enabled globally in `LmsApp.kt`.
- **Functional agents** (Classroom, Classwork, Assessment, Live Session, Scoring Analytics, Report Export, Data Sync, Presence) with injectable facades and Firestore-based repositories.
- **LAN/WebRTC live activities** via `LanBroadcaster` and `WebRtcHost` stubs, with Firestore fallback for session state.
- **Firebase Analytics / Crashlytics** ready through the platform BOM (33.5.1).

## Firestore Data Model

```
/orgs/{orgId}
  users/{userId}
  classes/{classId}
    roster/{userId}
    classwork/{workId}
      submissions/{submissionId}
        attempts/{attemptId}
    liveSessions/{sessionId}
      responses/{responseId}
  presence/{userId}
  counters/{counterId}
```

Composite indexes, sharded counters, and RBAC rules are provided under `firebase/`.

## Getting Started

1. **Clone** the repo and open the `mobile-lms` root in Android Studio (Ladybug+).
2. **Add Firebase config**: download `google-services.json` from the Firebase console and drop it into `app/src/main/`.
3. **Install dependencies**: the Gradle configuration uses Android Gradle Plugin 8.6.0 and Kotlin 2.0.20. No additional steps are required beyond a Gradle sync.
4. **Deploy Firebase artifacts** (optional):

   ```bash
   firebase deploy --only firestore:rules,firestore:indexes,storage
   cd functions && npm install && npm run build && firebase deploy --only functions
   ```

5. **Run the app**:

   ```bash
   ./gradlew :app:installDebug
   ```

   or use Android Studio’s “Run” button after selecting an emulator/device.

## Testing & CI

Use the bundled script `./gradlew lintDebug testDebugUnitTest assembleDebug` (or the CI workflow under `.github/workflows/android.yml`) to verify the build. Cloud Functions, Firestore rules, and emulators remain optional but are ready for integration.

## Next Steps

- Implement LAN discovery and WebRTC signaling inside the stubs under `data/net`.
- Extend `ReportExportAgentImpl` with real PDF/CSV generation and storage.
- Flesh out view models and Compose screens with real data flows once Firebase configs are linked.
*** End Patch
