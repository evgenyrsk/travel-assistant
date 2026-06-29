# Stage 8.38 — In-memory attempt store skeleton

## Цель Stage 8.38

Добавить backend-only process-local `ConfirmedSearchExecutionAttemptStore`
skeleton для будущего confirmed-search execution flow.

Store должен хранить attempt state между future calls и поддерживать typed
transitions для:

- no attempt -> `PREPARED`;
- `PREPARED` -> `IN_PROGRESS`;
- `IN_PROGRESS` -> `SUCCEEDED`;
- `IN_PROGRESS` -> `FAILED`;
- duplicate in-progress detection;
- duplicate succeeded detection;
- duplicate failed behavior.

Stage 8.38 не подключает store к routes/runtime composition и не запускает
actual search execution.

## Что было добавлено

Добавлены internal application-layer типы:

- `ConfirmedSearchExecutionAttemptStore`;
- `ConfirmedSearchExecutionAttemptStoreResult`;
- `ConfirmedSearchExecutionAttemptFailureReason`;
- `InMemoryConfirmedSearchExecutionAttemptStore`.

`ConfirmedSearchExecutionAttempt` расширен optional `failureReason`, чтобы
failed attempt мог хранить sanitized internal failure category без provider или
exception leakage.

## Production files

Добавлены:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionAttemptStore.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionAttemptStoreResult.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionAttemptFailureReason.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/InMemoryConfirmedSearchExecutionAttemptStore.kt`.

Изменен:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionAttempt.kt`.

Не менялись:

- `Application.kt`;
- assistant routes;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- `PlanConfirmedSearchCreationUseCase`;
- `BuildConfirmedSearchCreationCommandUseCase`;
- `PlanConfirmedSearchExecutionUseCase`;
- `PlanConfirmedSearchExecutionGuardUseCase`;
- `PlanConfirmedSearchExecutionAttemptUseCase`.

## Tests

Добавлен targeted test:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/InMemoryConfirmedSearchExecutionAttemptStoreTest.kt`.

Покрытие:

- save/find prepared attempt by idempotency key;
- duplicate prepared detection;
- `PREPARED` -> `IN_PROGRESS`;
- duplicate `IN_PROGRESS` detection;
- `IN_PROGRESS` -> `SUCCEEDED`;
- duplicate `SUCCEEDED` returns same stored success reference;
- `IN_PROGRESS` -> `FAILED`;
- duplicate `FAILED` returns stored failed state;
- invalid transition rejection without mutation;
- missing attempt transition rejection;
- process-local behavior;
- no pending store mutation / no `markConsumed`;
- no runtime search execution markers leakage.

## Store interface / implementation

Interface:

- `ConfirmedSearchExecutionAttemptStore`.

Process-local implementation:

- `InMemoryConfirmedSearchExecutionAttemptStore`.

The implementation uses a process-local `ConcurrentHashMap` keyed by
`ConfirmedSearchExecutionIdempotencyKey`. It is not durable storage, not a
repository, not a database adapter, and not route/runtime composition.

## Operations

Supported operations:

| Operation | Result |
|---|---|
| `savePrepared(attempt)` | Stores first `PREPARED` attempt or returns existing duplicate. |
| `findByIdempotencyKey(key)` | Returns stored attempt snapshot or `null`. |
| `markInProgress(key, now)` | Moves `PREPARED` attempt to `IN_PROGRESS`; duplicate otherwise. |
| `markSucceeded(key, createdSearchId, now)` | Moves `IN_PROGRESS` attempt to `SUCCEEDED` and stores passed success reference. |
| `markFailed(key, reason, now)` | Moves `IN_PROGRESS` attempt to `FAILED` and stores sanitized failure reason. |

Store results:

- `Stored(attempt)`;
- `Duplicate(existingAttempt)`;
- `Rejected(reason, existingAttempt?)`.

## Transition rules

Allowed:

| From | To |
|---|---|
| no attempt | `PREPARED` via `savePrepared`. |
| `PREPARED` | `IN_PROGRESS` via `markInProgress`. |
| `IN_PROGRESS` | `SUCCEEDED` via `markSucceeded`. |
| `IN_PROGRESS` | `FAILED` via `markFailed`. |

Duplicate/no-new-attempt behavior:

| Existing state | Duplicate behavior |
|---|---|
| `PREPARED` | `savePrepared` returns existing `PREPARED`. |
| `IN_PROGRESS` | `markInProgress` / `savePrepared` returns existing `IN_PROGRESS`. |
| `SUCCEEDED` | duplicate returns existing `SUCCEEDED` with same modeled search reference. |
| `FAILED` | duplicate returns existing `FAILED` with same failure reason. |

Rejected:

- transition for missing key -> `ATTEMPT_NOT_FOUND`;
- non-`PREPARED` input to `savePrepared` -> `PREPARED_ATTEMPT_REQUIRED`;
- `markSucceeded` / `markFailed` unless current state is `IN_PROGRESS` ->
  `ATTEMPT_NOT_IN_PROGRESS`.

Invalid transitions do not mutate the stored attempt.

## Duplicate handling

Duplicate handling is internal and state-based:

- duplicate in-progress does not start another execution;
- duplicate succeeded returns the same stored success reference;
- duplicate failed returns the same stored failed state;
- store never creates a new attempt when idempotency key already exists.

Retry policy for `FAILED` remains deferred. Stage 8.38 preserves failed state
rather than silently creating a second attempt.

## Success reference / search id boundary

`markSucceeded` accepts an existing `HotelSearchId` as a future-modeled success
reference and stores it in `ConfirmedSearchExecutionAttempt.createdSearchId`.

Stage 8.38 does not:

- generate `HotelSearchId`;
- call `CreateHotelSearchUseCase`;
- create actual runtime hotel search;
- expose the reference through public API.

The success reference exists only to model duplicate-after-success behavior for
future execution wiring.

## Process-local / no durable storage boundary

`InMemoryConfirmedSearchExecutionAttemptStore` is process-local and in-memory.

It does not add:

- database persistence;
- filesystem persistence;
- Redis/cache service;
- cross-instance synchronization;
- restart recovery;
- account-level storage.

TTL/expiry policy is not implemented in Stage 8.38. Pending confirmation TTL
still belongs to the existing pending-state guard path. Attempt TTL should be
reviewed before route execution wiring.

## No-route-wiring boundary

Attempt store is not connected to:

- assistant routes;
- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- `PlanConfirmedSearchCreationUseCase`;
- `BuildConfirmedSearchCreationCommandUseCase`;
- `PlanConfirmedSearchExecutionUseCase`;
- `PlanConfirmedSearchExecutionGuardUseCase`;
- `PlanConfirmedSearchExecutionAttemptUseCase`.

Backend runtime behavior is unchanged.

## No-search-execution boundary

Stage 8.38 does not:

- create search;
- create actual runtime `hotelSearchId`;
- return `show_hotel_results`;
- call `CreateHotelSearchUseCase`;
- call hotel provider;
- call `markConsumed`;
- write pending store;
- change existing hotel search flow.

## Raw/internal leakage boundary

Attempt store works only with typed internal application/domain objects:

- `ConfirmedSearchExecutionAttempt`;
- `ConfirmedSearchExecutionIdempotencyKey`;
- `ConfirmedSearchExecutionAttemptStatus`;
- `ConfirmedSearchExecutionAttemptFailureReason`;
- `HotelSearchId` as future-modeled success reference.

It does not accept, store or expose:

- raw `LlmCandidate`;
- `candidatePayload`;
- `modelResponse`;
- provider/model metadata;
- validation issue details;
- confidence or safety markers.

## Public API / OpenAPI / frontend / generated clients verdict

Unchanged.

Stage 8.38 does not change:

- public API request/response shape;
- OpenAPI contracts;
- frontend;
- generated clients;
- CI/build/package files.

## Stage 7 strict handoff compatibility

Compatible.

Stage 7 strict `hotel-search;` handoff remains the only current automatic
search creation path. The new attempt store is not route-wired and cannot start
search. Future confirmed-search execution can only be compatible as an
explicit-confirmation exception after separate route/execution readiness work.

## Provider / network / API keys verdict

Stage 8.38 does not add:

- real hotel provider;
- real LLM provider;
- network calls;
- provider-specific configuration;
- API keys, secrets or environment variables.

## Риски и ограничения

- Store is process-local; attempts are lost on restart.
- No cross-instance duplicate protection.
- No TTL/expiry for attempts yet.
- `FAILED` retry policy is intentionally deferred.
- Store is not route-wired and does not execute search.
- Success reference is modeled only; actual execution remains deferred.
- Durable storage remains out of scope.

## Рекомендуемый Stage 8.39

Safe Stage 8.39: review/design-only attempt store integration readiness gate.

Минимальная цель:

- проверить, достаточно ли store transitions для future execution use case;
- определить retry behavior for `FAILED`;
- определить TTL/expiry policy for attempts;
- определить ordering между store success, pending `markConsumed` и public
  response;
- подтвердить, что actual `CreateHotelSearchUseCase` call still needs a
  separate execution stage;
- не подключать route wiring или actual search execution.

## Verdict

Stage 8.38 выполнен как backend-only in-memory confirmed-search execution
attempt store skeleton.

Store хранит typed attempt state и поддерживает safe transitions/duplicates, но
не подключен к routes/runtime composition, не создает search, не создает actual
runtime `hotelSearchId`, не возвращает `show_hotel_results`, не вызывает
`CreateHotelSearchUseCase`, provider или `markConsumed`, не добавляет durable
storage и не меняет public contracts.
