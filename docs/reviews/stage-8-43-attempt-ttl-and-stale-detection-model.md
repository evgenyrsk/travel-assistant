# Stage 8.43 — Attempt TTL and Stale Detection Model

## 1. Scope

Stage 8.43 добавляет backend-only model/store skeleton для attempt TTL и
stale detection:

- `expiresAt` field в `ConfirmedSearchExecutionAttempt`;
- `STALE_EXECUTION` failure reason;
- narrow stale `IN_PROGRESS` detection в `InMemoryConfirmedSearchExecutionAttemptStore`;
- targeted unit tests.

Stage 8.43 не меняет runtime behavior, routes, API, OpenAPI, frontend,
generated clients и не подключает orchestration к runtime.

## 2. Implemented changes

Production files изменены:

- `ConfirmedSearchExecutionAttempt.kt` — добавлен `expiresAt: Instant`.
- `ConfirmedSearchExecutionAttemptFailureReason.kt` — добавлен `STALE_EXECUTION`.
- `ConfirmedSearchExecutionAttemptStore.kt` — `findByIdempotencyKey` теперь принимает `now: Instant`.
- `InMemoryConfirmedSearchExecutionAttemptStore.kt` — stale detection в `findByIdempotencyKey`: stale `IN_PROGRESS` attempt переводится в `FAILED(STALE_EXECUTION)`.
- `PlanConfirmedSearchExecutionAttemptUseCase.kt` — добавлен `attemptTtl` constructor parameter с default 15 минут; attempt creation вычисляет `expiresAt = now + ttl`.
- `ExecuteConfirmedSearchTransitionUseCase.kt` — `findByIdempotencyKey` call обновлён для передачи `request.now`.

Test files обновлены:

- `InMemoryConfirmedSearchExecutionAttemptStoreTest.kt` — `preparedAttempt()` helper обновлён для `expiresAt`; все `findByIdempotencyKey` calls обновлены для `now`; добавлены 5 stale detection tests.
- `PlanConfirmedSearchExecutionAttemptUseCaseTest.kt` — `attempt()` helper обновлён для `expiresAt`.
- `ExecuteConfirmedSearchTransitionUseCaseTest.kt` — `findByIdempotencyKey` calls обновлены для `now`.

Не менялись:

- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- assistant routes;
- API/OpenAPI/frontend/generated clients;
- любые другие Stage 8 classes.

## 3. TTL model

`expiresAt` добавлен как internal attempt lifecycle metadata:

| Field | Type | Purpose |
|---|---|---|
| `createdAt` | `Instant` | Attempt creation timestamp. |
| `updatedAt` | `Instant` | Last state change timestamp. |
| `expiresAt` | `Instant` | TTL boundary; attempt is stale after this time. |

TTL вычисляется в `PlanConfirmedSearchExecutionAttemptUseCase`:

- Default: `Duration.ofMinutes(15)` (aligned с pending confirmation TTL).
- Configurable через constructor parameter `attemptTtl`.
- Не использует config/env variables.
- Не использует clock abstraction (tests передают deterministic `now`).

`expiresAt` устанавливается при создании attempt (`PREPARED` и
`DUPLICATE_BLOCKED` snapshots). Store transitions (`markInProgress`,
`markSucceeded`, `markFailed`) не меняют `expiresAt`.

## 4. Stale detection behavior

Stale detection реализован в `InMemoryConfirmedSearchExecutionAttemptStore
.findByIdempotencyKey(key, now)`:

| Condition | Behavior |
|---|---|
| Attempt not found | Return `null`. |
| Attempt found, status != `IN_PROGRESS` | Return attempt as-is. |
| Attempt found, `IN_PROGRESS`, `now < expiresAt` | Return attempt as-is (non-stale). |
| Attempt found, `IN_PROGRESS`, `now >= expiresAt` | Mark as `FAILED(STALE_EXECUTION)`, persist, return failed attempt. |

Stale detection:

- only applies к `IN_PROGRESS` attempts;
- persists stale state in store (subsequent lookups return `FAILED`);
- does not create a new attempt;
- does not call `markConsumed`;
- does not create `hotelSearchId` или `show_hotel_results`.

`@Synchronized` annotation на `findByIdempotencyKey` обеспечивает
thread-safe stale detection и persist.

## 5. Failure reason model

`STALE_EXECUTION` добавлен как failure reason, not new status:

| Failure reason | Usage |
|---|---|
| `SEARCH_CREATION_FAILED` | Future actual execution failure. |
| `EXECUTION_STATE_UNKNOWN` | Future ambiguous execution state. |
| `STALE_EXECUTION` | `IN_PROGRESS` attempt exceeded TTL without completion. |

`STALE_EXECUTION` uses existing `FAILED` status. Новый status не
добавлен. Это consistent с Stage 8.42 policy decision.

## 6. Retry boundary

Stage 8.43 only unblocks stale classification, not retry execution.

| State after Stage 8.43 | Retry possible? |
|---|---|
| Stale `IN_PROGRESS` → `FAILED(STALE_EXECUTION)` | Classification done. Retry creation remains blocked by existing store semantics (`savePrepared` returns `Duplicate` for existing key). |
| `FAILED(SEARCH_CREATION_FAILED)` | Retry remains blocked. |
| `FAILED(STALE_EXECUTION)` | Retry remains blocked. |

Full retry policy (allowing new attempt for existing `FAILED` key)
остаётся future stage (Stage 8.44 per Stage 8.42 recommendation).

## 7. Runtime/API safety

Use case не подключён к routes/runtime composition.

- `Application.kt` не изменён.
- `AssistantLlmRouteWiringUseCase` не изменён.
- `CreateHotelSearchUseCase` не вызывается.
- Real `hotelSearchId` не создаётся.
- `show_hotel_results` не возвращается.
- `markConsumed` не wire'ится.
- Stage 7 strict `hotel-search;` handoff remains единственным current
  automatic search creation path.
- B6 Stage 7 compatibility proof remains separate.

`ExecuteConfirmedSearchTransitionUseCase` обновлён минимально: только
передача `request.now` в `findByIdempotencyKey`. Orchestration behavior
unchanged.

## 8. Tests

Добавлены 5 new tests в `InMemoryConfirmedSearchExecutionAttemptStoreTest`:

| Test | Что проверяет |
|---|---|
| `staleInProgressIsConvertedToFailedWithStaleExecutionReason` | Stale `IN_PROGRESS` (past `expiresAt`) переводится в `FAILED(STALE_EXECUTION)` при lookup. |
| `staleInProgressIsPersistedAsFailedAndReturnedOnSubsequentLookup` | Stale state persisted; subsequent lookup returns `FAILED` без повторной conversion. |
| `nonStaleInProgressRemainsDuplicateBlocking` | Non-stale `IN_PROGRESS` (before `expiresAt`) остаётся `IN_PROGRESS` и продолжает block duplicates. |
| `staleDetectionDoesNotAffectNonInProgressAttempts` | `PREPARED` attempt past `expiresAt` не подвергается stale detection (only `IN_PROGRESS`). |
| `staleDetectionDoesNotCreateNewAttempt` | Stale detection сохраняет idempotency key, session id и command plan; не создаёт новый attempt. |

Existing tests обновлены:

- `preparedAttempt()` и `attempt()` helpers: `expiresAt` parameter added.
- Все `findByIdempotencyKey` calls: `now` parameter added.

Все tests pass (BUILD SUCCESSFUL).

## 9. Explicit non-goals

Stage 8.43 не создаёт и не меняет:

- Route wiring или runtime composition.
- `CreateHotelSearchUseCase` call.
- Real `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` wiring.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.
- New attempt statuses.
- Full retry policy (retry from `FAILED` blocked).
- B6 Stage 7 compatibility proof.

## 10. Validation

- `git status --short`: подтверждено.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: BUILD SUCCESSFUL.
- Inspection: `expiresAt` present в attempt model; `STALE_EXECUTION` present в failure reasons; stale detection implemented в store; no runtime wiring; no `CreateHotelSearchUseCase` call; no `hotelSearchId`/`show_hotel_results` leakage.

## 11. Verdict

**Passed with notes** — stale classification added, retry remains future work.

Stage 8.43 добавил attempt TTL model (`expiresAt`), `STALE_EXECUTION`
failure reason и narrow stale detection в store. Stale `IN_PROGRESS`
attempts корректно переводятся в `FAILED(STALE_EXECUTION)` при lookup.
Retry creation остаётся blocked до отдельного Stage 8.44. Runtime wiring
не добавлен. Stage 7 strict handoff unchanged.
