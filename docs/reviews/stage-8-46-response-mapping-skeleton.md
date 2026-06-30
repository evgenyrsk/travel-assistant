# Stage 8.46 — Response Mapping Skeleton

## 1. Scope

Stage 8.46 добавляет internal typed mapper, который превращает
`ExecuteConfirmedSearchTransitionResult` variants в internal response
directives для future runtime wiring:

- `ConfirmedSearchTransitionResponseDirective` — internal typed directive model;
- `InternalTransitionNextAction` — internal next action enum;
- `TransitionMessageKind` — internal message direction enum;
- `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase` — mapper use case;
- narrow unit tests.

Stage 8.46 не меняет runtime behavior, routes, API, OpenAPI, frontend,
generated clients и не подключает mapper к runtime.

## 2. Implemented changes

Production files добавлены:

- `ConfirmedSearchTransitionResponseDirective.kt` — directive data class + `InternalTransitionNextAction` + `TransitionMessageKind` enums.
- `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase.kt` — mapper from transition result to directive.

Test files добавлены:

- `MapConfirmedSearchTransitionResultToResponseDirectiveUseCaseTest.kt` — 8 narrow unit tests.

Не менялись:

- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- assistant routes;
- API/OpenAPI/frontend/generated clients;
- existing Stage 8 classes.

## 3. Mapper boundary

`MapConfirmedSearchTransitionResultToResponseDirectiveUseCase` — это
internal-only mapper. Он принимает `ExecuteConfirmedSearchTransitionResult`
и возвращает `ConfirmedSearchTransitionResponseDirective`.

Mapper не владеет:

- route handler logic;
- HTTP response shape;
- `CreateHotelSearchUseCase` call;
- actual `hotelSearchId` creation;
- `show_hotel_results` response;
- `markConsumed` wiring;
- public API contract.

Mapper не подключён к runtime.

## 4. Directive model

`ConfirmedSearchTransitionResponseDirective` содержит:

| Field | Type | Purpose |
|---|---|---|
| `nextAction` | `InternalTransitionNextAction` | Internal next action directive. |
| `messageKind` | `TransitionMessageKind` | User-facing message direction. |
| `hotelSearchId` | `HotelSearchId?` | Nullable. Always null in Stage 8.46. |
| `mayShowHotelResults` | `Boolean` | Always false in Stage 8.46. |
| `shouldConsumePendingConfirmation` | `Boolean` | Always false in Stage 8.46. |

`InternalTransitionNextAction`:

| Value | Purpose |
|---|---|
| `ASK_CLARIFICATION` | Safe non-results action for all current skeleton states. |
| `SHOW_HOTEL_RESULTS` | Future-only; not produced by current mapper. |

`TransitionMessageKind`:

| Value | Purpose |
|---|---|
| `PROCESSING` | Transition recorded; internal processing state. |
| `ALREADY_PROCESSING` | Duplicate detected; existing attempt active. |
| `CONFIRMATION_REJECTED` | Guard rejected; cannot proceed. |
| `TEMPORARY_FAILURE` | Store rejected; safe retry. |

## 5. Mapping behavior

### 5.1 `Transitioned`

Current `Transitioned` с `PreparedButNotExecuted` / no real `createdSearchId`:

| Directive field | Value |
|---|---|
| `nextAction` | `ASK_CLARIFICATION` |
| `messageKind` | `PROCESSING` |
| `hotelSearchId` | `null` |
| `mayShowHotelResults` | `false` |
| `shouldConsumePendingConfirmation` | `false` |

Future `Transitioned(SUCCEEDED)` с real search id remains blocked until
actual execution stage provides real `HotelSearchId`.

### 5.2 `DuplicateDetected`

| Existing attempt status | Directive |
|---|---|
| `IN_PROGRESS` | `ASK_CLARIFICATION` + `ALREADY_PROCESSING`; no results; no consume. |
| `SUCCEEDED` | `ASK_CLARIFICATION` + `ALREADY_PROCESSING`; no results; no consume. Future: may produce `SHOW_HOTEL_RESULTS` with real search id. |
| `PREPARED` | `ASK_CLARIFICATION` + `ALREADY_PROCESSING`; no results; no consume. |
| `FAILED` | `ASK_CLARIFICATION` + `ALREADY_PROCESSING`; no results; no consume. |
| `DUPLICATE_BLOCKED` | `ASK_CLARIFICATION` + `ALREADY_PROCESSING`; no results; no consume. |

Future `DuplicateDetected(SUCCEEDED)` с actual execution may map to
`SHOW_HOTEL_RESULTS` + real `hotelSearchId` + `mayShowHotelResults = true`
+ `shouldConsumePendingConfirmation = true`.

### 5.3 `GuardRejected`

| Directive field | Value |
|---|---|
| `nextAction` | `ASK_CLARIFICATION` |
| `messageKind` | `CONFIRMATION_REJECTED` |
| `hotelSearchId` | `null` |
| `mayShowHotelResults` | `false` |
| `shouldConsumePendingConfirmation` | `false` |

### 5.4 `StoreRejected`

| Directive field | Value |
|---|---|
| `nextAction` | `ASK_CLARIFICATION` |
| `messageKind` | `TEMPORARY_FAILURE` |
| `hotelSearchId` | `null` |
| `mayShowHotelResults` | `false` |
| `shouldConsumePendingConfirmation` | `false` |

## 6. Runtime/API safety

Mapper не подключён к routes/runtime composition.

- `Application.kt` не изменён.
- `AssistantLlmRouteWiringUseCase` не изменён.
- `CreateHotelSearchUseCase` не вызывается.
- Real `hotelSearchId` не создаётся.
- `show_hotel_results` не возвращается из runtime.
- `markConsumed` не wire'ится.
- Stage 7 strict `hotel-search;` handoff unchanged.
- Public API/OpenAPI не менялись.

## 7. Tests

Добавлен `MapConfirmedSearchTransitionResultToResponseDirectiveUseCaseTest` с 8 tests:

| Test | Что проверяет |
|---|---|
| `transitionedMapsToProcessingWithoutHotelResultsOrSearchId` | `Transitioned` → `ASK_CLARIFICATION` + `PROCESSING`; no `hotelSearchId`; no results. |
| `transitionedDoesNotRequestPendingConsume` | `Transitioned` → `shouldConsumePendingConfirmation = false`. |
| `duplicateInProgressMapsToAlreadyProcessingWithoutHotelResults` | `DuplicateDetected(IN_PROGRESS)` → `ASK_CLARIFICATION` + `ALREADY_PROCESSING`; no results. |
| `guardRejectedMapsToConfirmationRejectedWithoutConsumeOrResults` | `GuardRejected` → `ASK_CLARIFICATION` + `CONFIRMATION_REJECTED`; no consume; no results. |
| `storeRejectedMapsToTemporaryFailureWithoutConsumeOrResults` | `StoreRejected` → `ASK_CLARIFICATION` + `TEMPORARY_FAILURE`; no consume; no results. |
| `noMapperOutputContainsRawShowHotelResults` | All 4 result variants produce directives without `show_hotel_results` or `SHOW_HOTEL_RESULTS` in string representation. |
| `mapperDoesNotCreateOrFakeRealHotelSearchId` | `Transitioned` directive has `null` hotelSearchId and `mayShowHotelResults = false`. |
| `mapperDoesNotRequireCreateHotelSearchUseCase` | Mapper instantiates and works without `CreateHotelSearchUseCase` dependency. |

Все tests pass (BUILD SUCCESSFUL).

## 8. Explicit non-goals

Stage 8.46 не создаёт и не меняет:

- Route wiring или runtime composition.
- `Application.kt`, `AssistantLlmRouteWiringUseCase`, assistant routes.
- `CreateHotelSearchUseCase` call.
- Real `hotelSearchId` creation.
- `show_hotel_results` response from runtime.
- `markConsumed` wiring.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.
- Actual hotel search execution.
- Response mapping for future `SUCCEEDED` with real search id.
- Stage 7 strict handoff changes.

## 9. Validation

- `git status --short`: подтверждено.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: BUILD SUCCESSFUL.
- Inspection: mapper files not referenced from `Application.kt` or `AssistantLlmRouteWiringUseCase`; no `CreateHotelSearchUseCase` call; no `show_hotel_results`/`hotelSearchId` leakage in mapper output.

## 10. Verdict

**Passed with notes** — mapper added, successful-results mapping remains
future work.

Stage 8.46 добавил internal typed response mapping skeleton:
`ConfirmedSearchTransitionResponseDirective` с `InternalTransitionNextAction`,
`TransitionMessageKind` и mapper use case. Все current fake/no-op/rejected
results map'ятся в safe non-results directives. `hotelSearchId` всегда null;
`mayShowHotelResults` всегда false; `shouldConsumePendingConfirmation`
всегда false. Future `SUCCEEDED` mapping с real search id remains blocked
until actual execution stage. Mapper не подключён к runtime. Stage 7
strict handoff unchanged.
