# Stage 8.40 — Execute Confirmed Search Transition Use Case Skeleton

## 1. Scope

Stage 8.40 добавляет backend-only internal application-level orchestration
skeleton `ExecuteConfirmedSearchTransitionUseCase`. Этот use case связывает
existing confirmed-search chain в единый orchestration boundary:

- guard result evaluation;
- attempt planning;
- attempt store persistence;
- fake/no-op execution transition;
- pending confirmation consumption decision.

Stage 8.40 не подключает use case к routes/runtime composition, не вызывает
`CreateHotelSearchUseCase`, не создает actual `hotelSearchId`, не возвращает
`show_hotel_results` и не wire'ит `markConsumed` в execution path.

## 2. Implemented changes

Добавлены production files:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ExecuteConfirmedSearchTransitionRequest.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ExecuteConfirmedSearchTransitionResult.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ExecuteConfirmedSearchTransitionUseCase.kt`.

Добавлен test file:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/ExecuteConfirmedSearchTransitionUseCaseTest.kt`.

Не менялись:

- `Application.kt`;
- assistant routes;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- `PlanConfirmedSearchCreationUseCase`;
- `BuildConfirmedSearchCreationCommandUseCase`;
- `PlanConfirmedSearchExecutionUseCase`;
- `PlanConfirmedSearchExecutionGuardUseCase`;
- `PlanConfirmedSearchExecutionAttemptUseCase`;
- `InMemoryConfirmedSearchExecutionAttemptStore`;
- любые другие existing Stage 8 classes.

## 3. Skeleton boundary

`ExecuteConfirmedSearchTransitionUseCase` — это internal application-layer
orchestration skeleton. Он принимает typed request и возвращает typed result.

Request:

- `ExecuteConfirmedSearchTransitionRequest(sessionId, decision, pendingConfirmation, now)`.

Result variants:

| Variant | Условие |
|---|---|
| `Transitioned` | New attempt planned, persisted и transitioned через fake/no-op execution. |
| `DuplicateDetected` | Existing attempt найден в store; duplicate не создаёт второй attempt. |
| `GuardRejected` | Guard или attempt planning вернул rejection. |
| `StoreRejected` | Store отклонил persist операцию. |

Use case использует existing chain internally:

1. `PlanConfirmedSearchCreationUseCase` — `Confirmed(criteria)` -> `ReadyToCreateSearch`.
2. `BuildConfirmedSearchCreationCommandUseCase` — `ReadyToCreateSearch` -> `CommandReady`.
3. `PlanConfirmedSearchExecutionUseCase` — `CommandReady` -> `PreparedButNotExecuted`.
4. `PlanConfirmedSearchExecutionGuardUseCase` — guard evaluation с pending snapshot.
5. `ConfirmedSearchExecutionAttemptStore.findByIdempotencyKey` — existing attempt lookup.
6. `PlanConfirmedSearchExecutionAttemptUseCase` — attempt planning из guard result.
7. `ConfirmedSearchExecutionAttemptStore.savePrepared` — attempt persistence.
8. `ConfirmedSearchExecutionAttemptStore.markInProgress` — fake/no-op execution transition.

Use case не владеет:

- route handler logic;
- HTTP response shape;
- `CreateHotelSearchUseCase` call;
- actual `hotelSearchId` creation;
- `show_hotel_results` response;
- provider calls;
- `markConsumed` runtime wiring;
- durable storage.

## 4. Ordering model

Orchestration ordering зафиксирован в use case code и tests:

| Шаг | Действие | State change |
|---|---|---|
| 1 | Guard evaluation | Read-only; no mutation. |
| 2 | Existing attempt lookup | Read-only; no mutation. |
| 3 | Attempt planning | No store mutation; produces plan. |
| 4 | Attempt persistence (`savePrepared`) | Store: no attempt -> `PREPARED`. |
| 5 | Fake/no-op execution (`markInProgress`) | Store: `PREPARED` -> `IN_PROGRESS`. |
| 6 | Transition result | Typed result с `PendingConsumptionDecision`. |

`PendingConsumptionDecision` — это metadata concept, не actual mutation:

| Decision | Условие |
|---|---|
| `CONSUME_AFTER_SUCCESSFUL_RECORDING` | Attempt успешно transitioned или existing attempt `SUCCEEDED`. |
| `DO_NOT_CONSUME` | Attempt `PREPARED`, `IN_PROGRESS`, `FAILED` или `DUPLICATE_BLOCKED`. |

Actual `markConsumed` call остаётся future runtime concern.

## 5. Idempotency behavior

Use case наследует idempotency behavior от existing store и attempt planning:

| Сценарий | Результат |
|---|---|
| First call с valid guard | `Transitioned` с new attempt в `IN_PROGRESS`. |
| Повторный call с тем же decision | `DuplicateDetected` с existing `IN_PROGRESS` attempt. |
| Guard rejected | `GuardRejected`; attempt не создан, store не мутирован. |
| Store rejected persist | `StoreRejected`; attempt не transitioned. |

Idempotency key derivation использует existing deterministic
`ConfirmedSearchExecutionIdempotencyKey.from(commandPlan)`.

Duplicate detection делегирован existing store: store не создаёт второй
attempt для того же idempotency key.

Retry из `IN_PROGRESS` или `FAILED` state остаётся deferred. Это carryover
из Stage 8.38, а не новый risk Stage 8.40. Future orchestration wiring stage
должен определить retry policy до runtime подключения.

## 6. Runtime/API safety

Use case не подключён к routes/runtime composition.

- `Application.kt` не изменён.
- `AssistantLlmRouteWiringUseCase` не изменён.
- Assistant routes не изменены.
- `CreateHotelSearchUseCase` не вызывается.
- `hotelSearchId` не создаётся.
- `show_hotel_results` не возвращается.
- `markConsumed` не wire'ится в execution path.
- Provider/network calls отсутствуют.
- Stage 7 strict `hotel-search;` handoff остаётся единственным current
  automatic search creation path.

Execution result: `ConfirmedSearchExecutionResult.PreparedButNotExecuted`.
Attempt status после transition: `IN_PROGRESS` (без `createdSearchId`).

Note: `IN_PROGRESS` remains an internal fake/no-op transition marker in this
skeleton stage. It does not mean that actual hotel search execution has started,
and it must not be interpreted as permission to expose `hotelSearchId`, return
`show_hotel_results`, or consume pending confirmation from runtime before a
separate wiring readiness gate.

Note: `PendingConsumptionDecision` is metadata only in Stage 8.40. No pending
confirmation is consumed by this use case, and any future `markConsumed` call
remains blocked until an explicit runtime wiring stage defines safe consume
ordering.

## 7. Tests

Добавлен `ExecuteConfirmedSearchTransitionUseCaseTest` с narrow unit tests:

| Test | Что проверяет |
|---|---|
| `newAttemptIsStoredAndTransitionedToInProgressWithNoOpExecution` | Use case stores attempt, transitions to `IN_PROGRESS`, returns `Transitioned` с `PreparedButNotExecuted` execution result и `CONSUME_AFTER_SUCCESSFUL_RECORDING`. |
| `duplicateCallReturnsExistingInProgressAttemptWithoutCreatingSecondAttempt` | Повторный call возвращает `DuplicateDetected` с `IN_PROGRESS` reason и `DO_NOT_CONSUME`. |
| `guardRejectedReturnsGuardRejectedWithoutStoringAttempt` | Guard rejection возвращает `GuardRejected`; store не мутирован. |
| `transitionedResultDoesNotContainRealHotelSearchId` | `Transitioned` result не содержит `createdSearchId`, `show_hotel_results`, `CreateHotelSearchUseCase` или provider references. |
| `useCaseDoesNotRequireCreateHotelSearchUseCase` | Use case работает без `CreateHotelSearchUseCase` dependency. |
| `orderingIsGuardThenLookupThenPlanThenPersistThenTransition` | Ordering зафиксирован: attempt в store совпадает с result attempt, status `IN_PROGRESS`. |
| `transitionResultDoesNotLeakSearchExecutionOrInternalCandidateData` | Result string representation не содержит forbidden tokens. |
| `doesNotMutatePendingConfirmationStore` | Pending confirmation store остаётся unchanged после use case call. |

Все 212 tests pass (8 новых + 204 existing).

## 8. Explicit non-goals

Stage 8.40 не создаёт и не меняет:

- Route wiring или runtime composition.
- `Application.kt`.
- `AssistantLlmRouteWiringUseCase`.
- `CreateHotelSearchUseCase` call из confirmation flow.
- Actual `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` wiring в execution path.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.
- Existing Stage 8 classes.
- Runtime semantics текущего confirmation flow.

## 9. Validation

- `git status --short`: подтверждено.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: 212 tests, all passed.
- `rg` inspection: `CreateHotelSearchUseCase` не вызывается из `ExecuteConfirmedSearchTransitionUseCase`; `hotelSearchId`/`show_hotel_results`/`markConsumed` не leak в transition result.

## 10. Verdict

**Passed with notes** — skeleton added, runtime wiring still blocked.

`ExecuteConfirmedSearchTransitionUseCase` добавлен как internal application-level
orchestration skeleton. Он связывает existing confirmed-search chain в единый
ordering boundary с typed result, pending consumption decision и idempotency
behavior. Use case не подключён к runtime, не вызывает `CreateHotelSearchUseCase`,
не создаёт actual `hotelSearchId`, не возвращает `show_hotel_results` и не
wire'ит `markConsumed`. Stage 7 strict hotel-search handoff остаётся
единственным текущим automatic search creation path.
