# ClassroomLMS Architecture

## Module Overview

- `app`: Entry point, navigation graph, theming, and Hilt application setup.
- `core:model`: Domain models shared across modules.
- `core:common`: Coroutine helpers, result wrappers, and utilities.
- `core:database`: Room database schema, entities, and DAO definitions (v1 schema).
- `core:network`: Firebase integrations (Auth, Firestore, RTDB) and live session protocol helpers.
- `core:sync`: Outbox-based synchronization orchestrator, WorkManager integration points.
- `feature:*`: UI + ViewModel layers for auth, home, learning catalog, classroom management, activity history, profile, and live sessions.

## Clean Architecture Flow

```
Feature ViewModel -> UseCases (feature module) -> Repositories (core sync/network/database)
                                   ↑                             ↓
                            Firebase + Room <-> SyncOrchestrator (outbox)
```

- **Repositories** coordinate between local Room cache and Firebase sources using coroutines and `Flow`.
- **Outbox** persists pending mutations in Room and syncs via WorkManager when connectivity resumes.
- **Live Sessions** rely on NSD discovery, WebRTC data channels, and fallback Firebase RTDB channels.

## Offline-First Strategy

1. Mutations write to Room entities immediately and enqueue an `OutboxEntity`.
2. `SyncOrchestrator` observes the outbox, pushes batches to Firebase, then fetches remote deltas per org/class/topic.
3. Conflict handling policy:
   - Attempts: keep highest score and earliest passing attempt.
   - Discussion/posts: last-writer-wins with timestamps.
   - Rosters/grades: remote authoritative; stale local changes rejected.
4. WorkManager jobs back off exponentially and jitter to avoid thundering herd when many devices reconnect.

## Live Session Discovery & Transport

- **Discovery**: Android NSD publishes `_lms._udp` services; clients scan LAN for hosts.
- **Transport**: Prefer WebRTC DataChannels for <1s updates. Fall back to Firebase RTDB topics when peer-to-peer fails.
- **Protocol**: Questions encoded as compact strings via `LiveSessionProtocol`. Responses include latency and scoring payloads.
- **Failover**: Peers elect the earliest join timestamp as the new host if the original drops.

## Testing Strategy

- Unit tests in each module cover ViewModel defaults, protocol encoding, Room DAO queries, and sync orchestration.
- Compose UI tests assert feature surfaces render expected labels.
- Instrumented tests run under `HiltTestRunner` with Compose testing APIs.

