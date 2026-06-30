# Stage 8.44 — Retry Transition Support

## 1. Scope

Stage 8.44 добавляет backend-only retry transition support на уровне
attempt store и orchestration use case:

- retry eligibility policy на основе `ConfirmedSearchExecutionAttemptFailureReason`;
- `savePrepared` позволяет retry для `FAILED` с retry-allowed reason;
- orchestration use case пропускает retry-eligible failed attempts как new attempt flow;
- narrow unit tests.

Stage 8.44 не меняет runtime behavior, routes, API, OpenAPI, frontend,
generated clients и не подключает orchestration к runtime.

## 2. Implemented changes

Production files изменены:

- `ConfirmedSearchExecutionAttemptFailureReason.kt` — добавлен `isRetryAllowed()` method: `STALE_EXECUTION` и `SEARCH_CREATION_FAILED` return true; `EXECUTION_STATE_UNKNOWN` returns false.
- `InMemoryConfirmedSearchExecutionAttemptStore.kt` — `savePrepared` позволяет replace existing `FAILED` attempt с retry-allowed reason; добавлен private `isRetryAllowed()` extension на `ConfirmedSearchExecutionAttempt`.
- `ExecuteConfirmedSearchTransitionUseCase.kt` — добавлен `isRetryEligible()` extension; retry-eligible existing attempts filtered перед planner (passed as `null`), позволяя new attempt flow.

Test files обновлены:

- `InMemoryConfirmedSearchExecutionAttemptStoreTest.kt` — добавлены 6 retry tests.
- `ExecuteConfirmedSearchTransitionUseCaseTest.kt` — добавлены 2 use-case-level retry tests.

Не менялись:

- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- assistant routes;
- API/OpenAPI/frontend/generated clients;
- любые другие Stage 8 classes.

## 3. Retry eligibility policy

Retry eligibility определяется через `ConfirmedSearchExecutionAttemptFailureReason.isRetryAllowed()`:

| Failure reason | Retry allowed? | Reason |
|---|---|---|
| `STALE_EXECUTION` | Да | Stale attempt can be safely retried; no actual search was created. |
| `SEARCH_CREATION_FAILED` | Да | Known failure before search creation; retry is safe. |
| `EXECUTION_STATE_UNKNOWN` | Нет | Search may have been created; retry risks duplicate. |

Retry eligibility rules:

- Only `FAILED` status is eligible for retry check.
- Only retry-allowed failure reasons allow new attempt.
- All other statuses (`PREPARED`, `IN_PROGRESS`, `SUCCEEDED`, `DUPLICATE_BLOCKED`) block retry as before.
- `FAILED` without failure reason (null) blocks retry.

## 4. Store behavior

`InMemoryConfirmedSearchExecutionAttemptStore.savePrepared` updated:

| Existing attempt | Behavior |
|---|---|
| No attempt | Save new `PREPARED`. Return `Stored`. |
| `PREPARED` | Return `Duplicate`. |
| `IN_PROGRESS` | Return `Duplicate`. |
| `SUCCEEDED` | Return `Duplicate`. |
| `DUPLICATE_BLOCKED` | Return `Duplicate`. |
| `FAILED` with retry-allowed reason | **Replace** with new `PREPARED`. Return `Stored`. |
| `FAILED` with retry-blocked reason | Return `Duplicate`. |
| `FAILED` without reason | Return `Duplicate`. |

Retry replaces the existing failed attempt in the store. The store
retains only one attempt per idempotency key (in-memory, no history).

`ExecuteConfirmedSearchTransitionUseCase` updated:

- `findByIdempotencyKey(key, now)` may return stale-marked `FAILED(STALE_EXECUTION)`.
- `isRetryEligible()` checks if existing attempt is `FAILED` with retry-allowed reason.
- Retry-eligible attempts are passed as `null` to planner, triggering new attempt flow.
- Retry-blocked attempts are passed as `existingAttempt`, triggering `DuplicateDetected`.

## 5. Stale attempt interaction

Stage 8.43 stale detection interacts correctly with retry:

```
1. First call: PREPARED → IN_PROGRESS (via markInProgress).
2. Time passes beyond expiresAt.
3. Second call: findByIdempotencyKey detects stale IN_PROGRESS.
   → Marks as FAILED(STALE_EXECUTION).
   → Returns FAILED(STALE_EXECUTION).
4. isRetryEligible(): FAILED + STALE_EXECUTION.isRetryAllowed() = true.
5. planningInput = null → planner creates new PREPARED.
6. savePrepared: existing FAILED(STALE_EXECUTION) is retry-allowed.
   → Replaces with new PREPARED.
   → Returns Stored.
7. markInProgress: PREPARED → IN_PROGRESS.
8. Returns Transitioned.
```

## 6. Limitations

| Limitation | Detail |
|---|---|
| Single attempt per key | Store хранит только один attempt per idempotency key. Retry replaces the failed attempt; no attempt history preserved. |
| No durable history | In-memory only. Failed attempts are lost on process restart. |
| No retry counter | No tracking of retry count. Unlimited retries for retry-allowed reasons. |
| No retry policy config | Retry eligibility is hardcoded in `isRetryAllowed()`. |

Эти limitations acceptable для Stage 8 scope. Durable attempt history и
retry counters — separate future concern.

## 7. Runtime/API safety

Use case не подключён к routes/runtime composition.

- `Application.kt` не изменён.
- `AssistantLlmRouteWiringUseCase` не изменён.
- `CreateHotelSearchUseCase` не вызывается.
- Real `hotelSearchId` не создаётся.
- `show_hotel_results` не возвращается.
- `markConsumed` не wire'ится.
- Stage 7 strict `hotel-search;` handoff unchanged.
- B6 Stage 7 compatibility proof remains separate.

## 8. Tests

Store-level retry tests (6 new):

| Test | Что проверяет |
|---|---|
| `failedWithStaleExecutionAllowsRetrySave` | `FAILED(STALE_EXECUTION)` allows `savePrepared` replacement. |
| `failedWithSearchCreationFailedAllowsRetrySave` | `FAILED(SEARCH_CREATION_FAILED)` allows retry. |
| `failedWithExecutionStateUnknownBlocksRetry` | `FAILED(EXECUTION_STATE_UNKNOWN)` returns `Duplicate`. |
| `staleInProgressFollowedByRetrySaveIsAllowed` | Stale detection marks `FAILED(STALE_EXECUTION)`, then retry `savePrepared` succeeds. |
| `succeededAttemptBlocksRetry` | `SUCCEEDED` returns `Duplicate` for retry attempt. |
| `retrySaveDoesNotCreateRealHotelSearchIdOrExposeForbiddenTokens` | Retry save result has no `createdSearchId`; no forbidden tokens in result text. |

Use-case-level retry tests (2 new):

| Test | Что проверяет |
|---|---|
| `retryAfterStaleInProgressReturnsTransitioned` | First call creates `IN_PROGRESS`; after TTL expires, second call detects stale, retries, returns `Transitioned` with new `IN_PROGRESS` attempt. |
| `retryBlockedAfterExecutionStateUnknown` | `FAILED(EXECUTION_STATE_UNKNOWN)` in store causes use case to return `DuplicateDetected` with `FAILED` reason. |

Existing tests updated: no changes needed to existing test helpers.

Все 225 tests pass (BUILD SUCCESSFUL).

## 9. Explicit non-goals

Stage 8.44 не создаёт и не меняет:

- Route wiring или runtime composition.
- `CreateHotelSearchUseCase` call.
- Real `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` wiring.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.
- New attempt statuses.
- Response mapping.
- Durable attempt history.
- Retry counters.
- B6 Stage 7 compatibility proof.

## 10. Validation

- `git status --short`: подтверждено.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: BUILD SUCCESSFUL (225 tests).
- Inspection: `isRetryAllowed()` present в failure reason; `savePrepared` handles retry-allowed `FAILED`; use case filters retry-eligible attempts; no runtime wiring; no `CreateHotelSearchUseCase` call; no `hotelSearchId`/`show_hotel_results` leakage.

## 11. Verdict

**Passed with notes** — retry supported in-memory, durable history remains
future work.

Stage 8.44 добавил retry transition support: `FAILED(STALE_EXECUTION)` и
`FAILED(SEARCH_CREATION_FAILED)` allow retry через `savePrepared`;
`FAILED(EXECUTION_STATE_UNKNOWN)` blocks retry. Orchestration use case
корректно пропускает retry-eligible failed attempts как new attempt flow.
Retry replaces existing attempt in store (no history). Runtime wiring не
добавлен. Stage 7 strict handoff unchanged.
