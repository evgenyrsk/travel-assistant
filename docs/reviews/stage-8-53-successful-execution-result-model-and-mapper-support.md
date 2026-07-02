# Stage 8.53 — Successful Execution Result Model and Mapper Support

## 1. Scope

Stage 8.53 — medium-small backend skeleton stage. Подготовить internal
model/mapper/composition support для future successful confirmed-search
execution result с real `HotelSearchId`, без actual execution call и без
route/runtime behavior changes.

Stage 8.53 объединяет:

1. response directive support для successful results (`RESULTS_READY`);
2. mapper support для `Transitioned(SearchCreated)` и `DuplicateDetected(SUCCEEDED)`;
3. composition result support для successful directive + `hotelSearchId`;
4. consume-after-success instruction (explicit, not executed);
5. safe success message text;
6. unit tests.

## 2. Implemented changes

Production files изменены:

- `ConfirmedSearchTransitionResponseDirective.kt` — добавлен `RESULTS_READY` в `TransitionMessageKind`.
- `ComposeConfirmedSearchTransitionResponseResult.kt` — добавлен `hotelSearchId: HotelSearchId?` field; добавлен `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS` в `PendingConsumeInstruction`.
- `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase.kt` — добавлен `mapTransitioned` с check на `SearchCreated`; обновлён `mapDuplicate` для `SUCCEEDED` с `createdSearchId`.
- `ComposeConfirmedSearchTransitionResponseUseCase.kt` — добавлен `RESULTS_READY_MESSAGE`; `consumeInstruction` derived from directive; `hotelSearchId` propagated to result.

Test files обновлены:

- `MapConfirmedSearchTransitionResultToResponseDirectiveUseCaseTest.kt` — 3 new tests.
- `ComposeConfirmedSearchTransitionResponseUseCaseTest.kt` — 1 new test.

Не менялись:

- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- assistant routes;
- API/OpenAPI/frontend/generated clients;
- `ExecuteConfirmedSearchTransitionUseCase` (execution logic);
- `ExecuteConfirmedSearchTransitionResult` (model);
- `ConfirmedSearchExecutionResult` (model — `SearchCreated` already existed);
- retry/stale behavior;
- route tests.

## 3. Successful execution result model

`ConfirmedSearchExecutionResult.SearchCreated` уже существует с
`searchId: HotelSearchId`. Stage 8.53 не создаёт новых execution result
variants. Mapper теперь умеет распознавать `SearchCreated` и map'ить его
в successful directive.

## 4. Transition result support

`ExecuteConfirmedSearchTransitionResult.Transitioned` already carries
`executionResult: ConfirmedSearchExecutionResult`. Когда future Stage 8.54
produces `SearchCreated`, mapper распознаёт его и maps в successful
directive.

Current runtime path (Stage 8.50) still produces `PreparedButNotExecuted`
→ non-results PROCESSING directive. No behavior change.

## 5. Response directive mapping

Updated mapping:

| Transition result | Directive |
|---|---|
| `Transitioned(PreparedButNotExecuted)` | `ASK_CLARIFICATION` + `PROCESSING` (unchanged). |
| `Transitioned(SearchCreated(searchId))` | `SHOW_HOTEL_RESULTS` + `RESULTS_READY` + `hotelSearchId` + `mayShowHotelResults=true` + `shouldConsume=true`. |
| `DuplicateDetected(SUCCEEDED with searchId)` | `SHOW_HOTEL_RESULTS` + `RESULTS_READY` + `hotelSearchId` + `mayShowHotelResults=true` + `shouldConsume=true`. |
| `DuplicateDetected(SUCCEEDED without searchId)` | `ASK_CLARIFICATION` + `ALREADY_PROCESSING` (unchanged). |
| `DuplicateDetected(IN_PROGRESS)` | `ASK_CLARIFICATION` + `ALREADY_PROCESSING` (unchanged). |
| `GuardRejected` | `ASK_CLARIFICATION` + `CONFIRMATION_REJECTED` (unchanged). |
| `StoreRejected` | `ASK_CLARIFICATION` + `TEMPORARY_FAILURE` (unchanged). |

## 6. Composition support

Composition result updated:

| Field | Success path | Non-success path |
|---|---|---|
| `responseDirective` | `SHOW_HOTEL_RESULTS` + `RESULTS_READY` | `ASK_CLARIFICATION` + safe text |
| `messageText` | "The search is ready. Hotel results are available." | PROCESSING/ALREADY_PROCESSING/REJECTED/FAILURE text |
| `pendingConsumeInstruction` | `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS` | `DO_NOT_CONSUME_PENDING_CONFIRMATION` |
| `hotelSearchId` | Real `HotelSearchId` from directive | `null` |

Composition use case derives `consumeInstruction` from `directive.shouldConsumePendingConfirmation`
и propagates `directive.hotelSearchId` to result.

## 7. Consume-after-success instruction

`PendingConsumeInstruction` updated:

| Value | When produced |
|---|---|
| `DO_NOT_CONSUME_PENDING_CONFIRMATION` | All non-success states (PROCESSING, ALREADY_PROCESSING, CONFIRMATION_REJECTED, TEMPORARY_FAILURE). |
| `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS` | Successful states (RESULTS_READY with real `hotelSearchId`). |

`markConsumed` **не вызывается** в Stage 8.53. Instruction is metadata only.
Future Stage 8.55 will implement actual consume-after-success behavior.

## 8. Runtime/API safety

- `CreateHotelSearchUseCase` не вызывается.
- Real runtime `hotelSearchId` не создаётся.
- `show_hotel_results` не возвращается из runtime.
- `markConsumed` не wire'ится и не вызывается.
- Runtime routes не менялись.
- Current Stage 8.50 non-results behavior unchanged.
- Stage 7 strict handoff unchanged.
- API/OpenAPI/frontend/generated clients не менялись.

Successful mapping is internal model support only. Actual execution,
`markConsumed`, и runtime `show_hotel_results` response remain future work.

## 9. Tests

Mapper tests (3 new):

| Test | Что проверяет |
|---|---|
| `transitionedWithSearchCreatedMapsToShowHotelResultsDirective` | `Transitioned(SearchCreated)` → `SHOW_HOTEL_RESULTS` + `RESULTS_READY` + `hotelSearchId` + `mayShowHotelResults=true` + `shouldConsume=true`. |
| `duplicateSucceededWithSearchIdMapsToShowHotelResultsDirective` | `DuplicateDetected(SUCCEEDED with searchId)` → same successful directive. |
| `duplicateSucceededWithoutSearchIdMapsToAlreadyProcessing` | `DuplicateDetected(SUCCEEDED without searchId)` → `ASK_CLARIFICATION` + `ALREADY_PROCESSING`. |

Composition test (1 new):

| Test | Что проверяет |
|---|---|
| `duplicateSucceededWithSearchIdComposesShowHotelResultsDirective` | Full composition flow with pre-seeded `SUCCEEDED` attempt → `SHOW_HOTEL_RESULTS` + `RESULTS_READY` + `hotelSearchId` + `CONSUME_AFTER_SUCCESS` + success message text. |

Все tests pass (BUILD SUCCESSFUL).

## 10. Explicit non-goals

Stage 8.53 не создаёт и не меняет:

- Actual `CreateHotelSearchUseCase` call.
- Route/runtime wiring.
- `markConsumed` wiring или вызов.
- Real runtime `hotelSearchId` creation.
- `show_hotel_results` runtime response.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status docs.
- `ExecuteConfirmedSearchTransitionUseCase` execution logic.
- Retry/stale store behavior.
- Stage 7 strict handoff.
- Current Stage 8.50 non-results route behavior.

## 11. Validation

- `git status --short`: подтверждено.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: BUILD SUCCESSFUL.
- Inspection: `CreateHotelSearchUseCase` not called; `Application.kt` and `AssistantLlmRouteWiringUseCase` not in changed files; successful mapping only activated when `SearchCreated` or `SUCCEEDED with createdSearchId` produced — neither happens in current runtime path.

## 12. Verdict

**Passed with notes** — successful mapping supported, actual execution
remains blocked.

Stage 8.53 добавил internal model/mapper/composition support для future
successful execution result. `RESULTS_READY` message kind, `SHOW_HOTEL_RESULTS`
directive mapping для `Transitioned(SearchCreated)` и `DuplicateDetected(SUCCEEDED
with searchId)`, `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS` instruction,
и `hotelSearchId` propagation — всё internal only. Actual execution call,
`markConsumed`, и runtime `show_hotel_results` response remain blocked
до Stage 8.54–8.55. Current Stage 8.50 non-results route behavior
unchanged. Stage 7 strict handoff unchanged.
