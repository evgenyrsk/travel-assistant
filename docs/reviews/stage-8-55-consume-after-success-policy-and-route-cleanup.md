# Stage 8.55 — Consume-After-Success Policy and Route Cleanup

## 1. Scope

Stage 8.55 — medium-small backend/runtime stage. Добавляет safe
consume-after-success behavior для confirmed-search flow:

1. conditional `markConsumed` after successful local search creation;
2. pending confirmation consumed after success;
3. pending remains active for non-success/failure paths;
4. duplicate-after-consume behavior defined and tested;
5. route tests updated.

## 2. Implemented changes

Production files изменены:

- `AssistantLlmRouteWiringUseCase.kt`:
  - `Confirmed` branch: добавлен conditional `consumePendingConfirmation(decidedAt)` when `composedResult.pendingConsumeInstruction == CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS`.
  - Consume triggered by explicit instruction, not by message text, nextAction, or hotelSearchId presence.

Test files обновлены:

- `AssistantSessionRoutesTest.kt`:
  - `positiveConfirmationReplyKeepsPendingActiveAndCreatesHotelSearch` → renamed to `positiveConfirmationReplyConsumesPendingAfterSuccessfulSearchCreation`; pending assertion changed from active to consumed (`null`).
  - `stage8CompatibilityFullConfirmationCycleCreatesHotelSearchWithResults` — pending assertion changed to consumed (`null`).
  - `repeatedConfirmationReplyReturnsSameHotelSearchResults` → renamed to `repeatedConfirmationAfterConsumedSuccessGoesThroughLlmPath`; second "да" after consumed success goes through LLM path (no active pending), returns `show_boundary_message`.

Не менялись:

- API/OpenAPI contracts;
- frontend;
- generated clients;
- `ExecuteConfirmedSearchTransitionUseCase`;
- `ComposeConfirmedSearchTransitionResponseUseCase`;
- `ComposeConfirmedSearchTransitionResponseResult`;
- `PendingConsumeInstruction` enum;
- Stage 7 strict handoff;
- declined/replanning behavior;
- retry/stale store behavior.

## 3. Consume-after-success trigger

Consume trigger mechanism:

```
composedResult = composeTransitionResponse(request)
if (composedResult.pendingConsumeInstruction ==
    PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS) {
    consumePendingConfirmation(decidedAt)
}
```

Consume is determined by:

- **Explicit `PendingConsumeInstruction`** — not by message text, nextAction string, or hotelSearchId presence.
- `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS` — only set by composition when `directive.shouldConsumePendingConfirmation == true`, which only happens for `SHOW_HOTEL_RESULTS` directive with real `hotelSearchId`.
- `DO_NOT_CONSUME_PENDING_CONFIRMATION` — all other cases.

## 4. Successful confirmation behavior

After successful "да" reply:

| Step | Behavior |
|---|---|
| 1 | `findActiveBySession` → active pending found. |
| 2 | `PlanPostConfirmationDecisionUseCase` → `Confirmed`. |
| 3 | `composeTransitionResponse` → `Transitioned(SearchCreated(searchId))`. |
| 4 | Mapper → `SHOW_HOTEL_RESULTS` + `RESULTS_READY` + `hotelSearchId` + `shouldConsume=true`. |
| 5 | Composition → `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS`. |
| 6 | `consumePendingConfirmation(decidedAt)` called. |
| 7 | Response: `show_hotel_results` + `hotelSearchId` + "The search is ready." |
| 8 | Pending: consumed (`null`). |

## 5. Duplicate-after-success behavior

After successful consume, repeated "да":

| Step | Behavior |
|---|---|
| 1 | `findActiveBySession` → `null` (consumed). |
| 2 | Skips `withPostConfirmationDecision`. |
| 3 | Goes through `planAssistantLlmDecisionUseCase` (LLM path). |
| 4 | LLM returns response per existing behavior. |
| 5 | No second hotel search created. |
| 6 | No reuse of consumed pending confirmation. |

Key property: consumed pending means no active confirmation. Repeated
"да" does not create duplicate search and does not reuse consumed pending.

## 6. Non-success behavior

`markConsumed` **not called** for:

| Path | Pending behavior |
|---|---|
| `GuardRejected` | Remains active. |
| `StoreRejected` | Remains active. |
| `DuplicateDetected(IN_PROGRESS)` | Remains active. |
| `FAILED(SEARCH_CREATION_FAILED)` | Remains active. |
| `ASK_CLARIFICATION` (any) | Remains active. |
| `NeedsClarification` | Remains active (unchanged). |
| `Declined` | Consumed (unchanged). |
| `NeedsReplanning` | Consumed (unchanged). |
| `Unknown` | Remains active (unchanged). |
| `NoActivePendingConfirmation` | No pending to consume. |

## 7. Failure/retry behavior

If `CreateHotelSearchUseCase` fails:

- Attempt: `FAILED(SEARCH_CREATION_FAILED)`.
- Result: `StoreRejected`.
- Consume instruction: `DO_NOT_CONSUME_PENDING_CONFIRMATION`.
- Pending: remains active.
- User can retry per existing retry policy.
- No `hotelSearchId`. No `show_hotel_results`.

Retry-eligible failure reasons (`SEARCH_CREATION_FAILED`, `STALE_EXECUTION`)
allow new attempt creation on retry. Retry-blocked (`EXECUTION_STATE_UNKNOWN`)
blocks retry.

## 8. Stage 7 compatibility

Stage 7 strict `hotel-search;` handoff:

- Still creates search via `CreateHotelSearchUseCase`.
- Still returns `show_hotel_results` + `hotelSearchId`.
- Not affected by consume-after-success branch (strict handoff runs before pending confirmation check).
- Tests: `completeExplicitAssistantMessageCreatesSearchAndExposesRankedOffers`, `explicitHotelSearchHandoffStillCreatesSearchWhenLlmWouldProceed`, `stage8CompatibilityStrictHandoffAfterConfirmationStillCreatesSearch`, `stage8WiringStrictHandoffAfterConfirmedReplyStillCreatesSearch`.

## 9. Runtime/API safety

- `markConsumed` called only after explicit `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS` instruction.
- No external provider/network calls added.
- No API keys, auth, durable storage, booking flow added.
- OpenAPI/frontend/generated clients unchanged.
- Stage 7 strict handoff unchanged.
- Existing response contract supports `show_hotel_results` + `hotelSearchId`.

## 10. Tests

Route tests updated (3):

| Test | Что проверяет |
|---|---|
| `positiveConfirmationReplyConsumesPendingAfterSuccessfulSearchCreation` | "да" → search created → `show_hotel_results` + `hotelSearchId` → pending consumed (`null`). |
| `stage8CompatibilityFullConfirmationCycleCreatesHotelSearchWithResults` | Full cycle → search created → pending consumed. |
| `repeatedConfirmationAfterConsumedSuccessGoesThroughLlmPath` | Second "да" after consumed success → LLM path → `show_boundary_message` → no duplicate search. |

Existing tests remain green:

- `ambiguousConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch` — "ок" → pending active.
- `negativeConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` — "нет" → consumed (unchanged).
- `correctionConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` — "лучше Париж" → consumed (unchanged).
- `unknownConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch` — unknown → pending active.
- All Stage 7 strict handoff tests.

Все 249 tests pass (BUILD SUCCESSFUL).

## 11. Explicit non-goals

Stage 8.55 не создаёт и не меняет:

- External provider/network calls.
- API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Stage 7 strict handoff behavior.
- Declined/replanning behavior.
- Retry/stale store policy.
- Hotel provider implementation.
- Search result contract.
- `ExecuteConfirmedSearchTransitionUseCase` execution logic.
- `ComposeConfirmedSearchTransitionResponseUseCase` composition logic.
- `PendingConsumeInstruction` enum.

## 12. Validation

- `git status --short`: подтверждено.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: BUILD SUCCESSFUL (249 tests).
- Inspection: `markConsumed` called only in `Confirmed` branch with explicit `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS` check; non-success paths unchanged; Stage 7 strict handoff tests pass.

## 13. Verdict

**Passed** — consume-after-success policy implemented safely.

Stage 8.55 добавил conditional `markConsumed` после successful local search
creation. Consume triggered by explicit `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS`
instruction, не по message text или nextAction. Pending confirmation
consumed after success. Pending remains active для non-success/failure
paths. Repeated "да" после consumed success идёт через LLM path и не
создаёт duplicate search. Stage 7 strict handoff unchanged.
OpenAPI/frontend/generated clients unchanged.
