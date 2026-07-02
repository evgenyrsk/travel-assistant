# Stage 8.54 — Actual Execution Call and Succeeded Recording

## 1. Scope

Stage 8.54 — narrow backend/runtime stage. Добавляет actual local
`CreateHotelSearchUseCase` call из confirmed-search transition flow и
records successful attempt as `SUCCEEDED` с real `HotelSearchId`.

Stage 8.54 объединяет:

1. actual `CreateHotelSearchUseCase` call внутри `ExecuteConfirmedSearchTransitionUseCase`;
2. successful attempt recording (`markSucceeded` с real `HotelSearchId`);
3. failure handling (`markFailed(SEARCH_CREATION_FAILED)`);
4. route response mapping для successful result (`show_hotel_results` + `hotelSearchId`);
5. unit tests для execution flow;
6. route tests для confirmation success behavior;
7. review doc.

Stage 8.54 НЕ добавляет `markConsumed`. Pending confirmation остаётся
active после success. Consume-after-success — Stage 8.55.

## 2. Implemented changes

Production files изменены:

- `ExecuteConfirmedSearchTransitionUseCase.kt`:
  - добавлен `hotelSearchBoundary: HotelSearchBoundary` constructor parameter;
  - удалён `planExecution: PlanConfirmedSearchExecutionUseCase` (no longer needed);
  - `transitionExecution` теперь calls `hotelSearchBoundary.createSearch(command)`, then `markSucceeded(searchId)`, returns `Transitioned(SearchCreated(searchId))`;
  - on exception: `markFailed(SEARCH_CREATION_FAILED)`, returns `StoreRejected`.

- `AssistantLlmRouteWiringUseCase.kt`:
  - `composeTransitionResponse` теперь required parameter (без default);
  - `Confirmed` branch maps `SHOW_HOTEL_RESULTS` directive to `AssistantNextAction.SHOW_HOTEL_RESULTS` + `hotelSearchId`; `ASK_CLARIFICATION` directive remains `withClarification`.

- `Application.kt`:
  - `hotelSearchBoundary` passed to `ExecuteConfirmedSearchTransitionUseCase`.

Test files обновлены:

- `ExecuteConfirmedSearchTransitionUseCaseTest.kt` — rewritten for actual execution: `StubHotelSearchBoundary`, `FailingHotelSearchBoundary`, `SequenceHotelSearchBoundary`; 8 tests covering success, failure, duplicate, retry.
- `ComposeConfirmedSearchTransitionResponseUseCaseTest.kt` — updated for successful composition: `TestHotelSearchBoundary`; 6 updated tests + 1 existing success test.
- `AssistantSessionRoutesTest.kt` — 3 route tests updated for confirmation success behavior.

Не менялись:

- API/OpenAPI contracts;
- frontend;
- generated clients;
- Stage 7 strict handoff;
- declined/replanning behavior;
- retry/stale store behavior;
- `markConsumed` not called.

## 3. Actual execution flow

Новый execution flow внутри `ExecuteConfirmedSearchTransitionUseCase`:

```
1. planSearchCreation(decision) → ReadyToCreateSearch
2. buildCommand(sessionId, plan) → CommandReady
3. guardUseCase(guardRequest) → AllowedButBlocked / Rejected
4. findByIdempotencyKey(key, now) → existing attempt / null
5. planAttempt(guardResult, now, planningInput) → Prepared / Duplicate / Rejected
6. For Prepared:
   a. savePrepared(attempt) → Stored
   b. markInProgress(key, now) → Stored (IN_PROGRESS)
   c. hotelSearchBoundary.createSearch(command) → HotelSearch
   d. markSucceeded(key, searchId, now) → Stored (SUCCEEDED)
   e. Return Transitioned(SearchCreated(searchId))
   f. On exception: markFailed(SEARCH_CREATION_FAILED) → StoreRejected
```

## 4. Successful attempt recording

Successful execution records:

- Attempt status: `SUCCEEDED`;
- `createdSearchId`: real `HotelSearchId` from `CreateHotelSearchUseCase`;
- `failureReason`: `null`;
- `executionResult`: `SearchCreated(searchId)`.

Duplicate detection for existing `SUCCEEDED` attempt returns same `hotelSearchId`
without calling `CreateHotelSearchUseCase` again.

## 5. Failure handling

If `CreateHotelSearchUseCase` throws:

- Attempt: `markFailed(SEARCH_CREATION_FAILED)`;
- Result: `StoreRejected(ATTEMPT_NOT_IN_PROGRESS)`;
- No `hotelSearchId`;
- No `show_hotel_results`;
- Pending confirmation remains active.

Retry allowed for `FAILED(SEARCH_CREATION_FAILED)` per existing retry policy.

## 6. Duplicate/idempotency behavior

| Existing attempt | Behavior |
|---|---|
| `SUCCEEDED` with `createdSearchId` | `DuplicateDetected(SUCCEEDED)` → `SHOW_HOTEL_RESULTS` + same `hotelSearchId`. No second `createSearch` call. |
| `IN_PROGRESS` (non-stale) | `DuplicateDetected(IN_PROGRESS)` → `ASK_CLARIFICATION` + `ALREADY_PROCESSING`. No `createSearch` call. |
| `FAILED(SEARCH_CREATION_FAILED)` | Retry allowed → new `PREPARED` → execution → `SUCCEEDED`. |
| `FAILED(EXECUTION_STATE_UNKNOWN)` | Retry blocked → `DuplicateDetected(FAILED)`. No `createSearch` call. |
| `FAILED(STALE_EXECUTION)` | Retry allowed → new `PREPARED` → execution → `SUCCEEDED`. |

Idempotency preserved: same confirmation/idempotency key never produces
multiple local hotel searches.

## 7. Pending confirmation behavior

Stage 8.54: pending confirmation **remains active** after success.

- `markConsumed` **не вызывается**.
- Pending stays `PENDING` status.
- Existing 15-min TTL handles cleanup.
- This is intentional temporary behavior.
- Stage 8.55 must introduce consume-after-success.

## 8. Runtime/API safety

- `CreateHotelSearchUseCase` вызывается из confirmation flow через `HotelSearchBoundary`.
- No external provider/network calls (uses existing `FakeHotelOfferProvider`).
- Real local `hotelSearchId` created after confirmation.
- `show_hotel_results` returned after successful confirmation.
- `markConsumed` **не** вызывается.
- Pending confirmation **не** consumed.
- OpenAPI/frontend/generated clients unchanged.
- Stage 7 strict `hotel-search;` handoff unchanged.

Route response for successful confirmation:

| Field | Value |
|---|---|
| `nextAction` | `show_hotel_results` |
| `hotelSearchId` | Real local search id |
| `assistantMessage.content` | "The search is ready. Hotel results are available." |
| Pending status | `PENDING` (not consumed) |

## 9. Tests

Unit tests (8 in `ExecuteConfirmedSearchTransitionUseCaseTest`):

| Test | Что проверяет |
|---|---|
| `successfulTransitionCreatesSearchAndRecordsSucceeded` | `createSearch` called once; attempt `SUCCEEDED`; `SearchCreated(searchId)` returned. |
| `duplicateAfterSuccessDoesNotCreateSecondSearch` | Second call → `DuplicateDetected(SUCCEEDED)`; `createSearch` not called again. |
| `duplicateAfterSuccessReusesExistingHotelSearchId` | Same `hotelSearchId` returned for duplicate. |
| `failedSearchCreationRecordsFailedWithSearchCreationFailedReason` | Exception → `markFailed(SEARCH_CREATION_FAILED)`; attempt `FAILED`. |
| `failedSearchCreationDoesNotConsumePendingConfirmation` | Pending remains active after failure. |
| `guardRejectedReturnsGuardRejectedWithoutCallingSearchCreation` | Guard rejected → `createSearch` not called; `createSearchCallCount = 0`. |
| `retryAfterStaleInProgressCreatesNewSearch` | Stale attempt → retry → new `SUCCEEDED` with new `searchId`. |
| `retryBlockedAfterExecutionStateUnknown` | `EXECUTION_STATE_UNKNOWN` → `DuplicateDetected(FAILED)`; `createSearch` not called. |

Composition tests (6 updated in `ComposeConfirmedSearchTransitionResponseUseCaseTest`):

| Test | Что проверяет |
|---|---|
| `successfulTransitionComposesShowHotelResultsDirective` | `SHOW_HOTEL_RESULTS` + `RESULTS_READY` + `CONSUME_AFTER_SUCCESS`. |
| `successfulTransitionIncludesHotelSearchId` | `hotelSearchId` present in directive and result. |
| `successfulTransitionRequestsHotelResults` | `mayShowHotelResults = true`. |
| `successfulTransitionRequestsPendingConsume` | `shouldConsumePendingConfirmation = true`. |
| `duplicateSucceededComposesShowHotelResultsWithExistingSearchId` | Duplicate → same `hotelSearchId` + `SHOW_HOTEL_RESULTS`. |
| `compositionDoesNotMutatePendingConfirmationStore` | Pending store unchanged after composition. |

Route tests (3 updated in `AssistantSessionRoutesTest`):

| Test | Что проверяет |
|---|---|
| `positiveConfirmationReplyKeepsPendingActiveAndCreatesHotelSearch` | "да" → `show_hotel_results` + `hotelSearchId` + pending active + offers 200. |
| `stage8CompatibilityFullConfirmationCycleCreatesHotelSearchWithResults` | Full cycle → search created + results available + pending active. |
| `repeatedConfirmationReplyReturnsSameHotelSearchResults` | Second "да" → same `hotelSearchId` + `show_hotel_results`. |
| `stage8CompatibilityStrictHandoffAfterConfirmationStillCreatesSearch` | Strict handoff after "да" → different `hotelSearchId` + `show_hotel_results`. |

Все 249 tests pass (BUILD SUCCESSFUL).

## 10. Explicit non-goals

Stage 8.54 не создаёт и не меняет:

- `markConsumed` wiring или вызов.
- Pending confirmation consumption.
- External provider/network calls.
- API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Stage 7 strict handoff behavior.
- Declined/replanning behavior.
- Retry/stale store policy.
- Actual `SUCCEEDED` execution handling beyond local `CreateHotelSearchUseCase`.

## 11. Validation

- `git status --short`: подтверждено.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: BUILD SUCCESSFUL (249 tests).
- Inspection: `CreateHotelSearchUseCase` called only from `ExecuteConfirmedSearchTransitionUseCase`; `markConsumed` not called in new path; `Application.kt` passes `hotelSearchBoundary`; Stage 7 strict handoff unchanged.

## 12. Verdict

**Passed with notes** — execution works, consume-after-success remains future work.

Stage 8.54 добавил actual local `CreateHotelSearchUseCase` call из
confirmed-search transition flow. Successful creation records `SUCCEEDED`
с real `HotelSearchId`. Duplicate success reuses existing `hotelSearchId`.
Failed creation records `FAILED(SEARCH_CREATION_FAILED)`. Route response
returns `show_hotel_results` + `hotelSearchId` for successful confirmation.
`markConsumed` **не** вызывается. Pending confirmation остаётся active
intentionally — consume-after-success остаётся future Stage 8.55.
OpenAPI/frontend/generated clients unchanged. Stage 7 strict handoff
unchanged.
