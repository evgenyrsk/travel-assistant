# Stage 8.52 — Post-Wiring Verification and Actual Execution Readiness Plan

## 1. Scope

Stage 8.52 — review/design-only stage. Проверить текущее runtime состояние
после Stage 8.50 non-results wiring и сформировать безопасный readiness plan
до actual execution wiring.

Stage 8.52 не меняет production code, tests, runtime behavior, routes,
API, OpenAPI, frontend, generated clients или roadmap/status/product/
architecture docs.

## 2. Current runtime snapshot

После Stage 8.50:

| Event | Behavior |
|---|---|
| User sends hotel request → LLM proceed candidate | Confirmation prompt shown; pending saved. |
| User replies "да" | Composition called; attempt recorded (IN_PROGRESS); PROCESSING text; pending active; no search. |
| User replies "да" again | Composition detects duplicate; ALREADY_PROCESSING text; pending active; no search. |
| User replies "ок" / ambiguous | NeedsClarification text; pending active. |
| User replies "нет" | Declined text; pending consumed. |
| User replies "лучше Париж" | NeedsReplanning text; pending consumed. |
| User sends `hotel-search; ...` | Strict handoff creates search; `show_hotel_results` + `hotelSearchId`. |
| Pending TTL expires (15 min) | Pending auto-expires; no attempt store cleanup. |

## 3. Post-wiring verification

### 3.1 Confirmed branch behavior

Текущий `Confirmed` branch:

```
PostConfirmationDecision.Confirmed
  → ComposeConfirmedSearchTransitionResponseUseCase(request)
    → ExecuteConfirmedSearchTransitionUseCase
      → PlanConfirmedSearchCreationUseCase (ReadyToCreateSearch)
      → BuildConfirmedSearchCreationCommandUseCase (CommandReady)
      → PlanConfirmedSearchExecutionUseCase (PreparedButNotExecuted)
      → PlanConfirmedSearchExecutionGuardUseCase (AllowedButBlocked)
      → PlanConfirmedSearchExecutionAttemptUseCase (Prepared)
      → savePrepared (Stored)
      → markInProgress (IN_PROGRESS)
      → Transitioned(IN_PROGRESS, PreparedButNotExecuted)
    → MapToDirective (ASK_CLARIFICATION + PROCESSING)
    → safeMessageText ("I am preparing that search...")
  → withClarification(composedResult.messageText)
```

- No `CreateHotelSearchUseCase` call.
- No `hotelSearchId` in response.
- No `show_hotel_results` in response.
- No `markConsumed` call.
- `nextAction` = `ask_clarification`.

**Verdict**: safe non-results behavior confirmed.

### 3.2 Pending confirmation behavior

| Decision | Pending consumed? | Reason |
|---|---|---|
| `Confirmed` | Нет | Composition returns `DO_NOT_CONSUME`. |
| `NeedsClarification` | Нет | Unchanged. |
| `Declined` | Да | Unchanged. |
| `NeedsReplanning` | Да | Unchanged. |
| `Unknown` | Нет | Unchanged. |
| `NoActivePendingConfirmation` | Нет | No pending to consume. |

Pending TTL (15 min) handles cleanup. No attempt store TTL interaction yet.

**Verdict**: pending behavior safe и consistent со Stage 8.42 policy.

### 3.3 Duplicate confirmation behavior

Повторный "да" после первого:

- `findByIdempotencyKey` находит existing IN_PROGRESS attempt.
- `DuplicateDetected(IN_PROGRESS)` returned.
- Mapper → `ASK_CLARIFICATION` + `ALREADY_PROCESSING`.
- "That search is already being prepared."
- Pending remains active.
- No search created.

**Verdict**: duplicate behavior safe.

### 3.4 Stage 7 strict handoff compatibility

Stage 7 strict `hotel-search;` handoff:

- `explicitHotelSearchMessageParser.parse` runs BEFORE pending confirmation check.
- Strict handoff creates search via `CreateHotelSearchUseCase`.
- Returns `show_hotel_results` + `hotelSearchId`.
- Unaffected by Stage 8.50 wiring.

Tests proving compatibility:

- `stage8CompatibilityStrictHandoffRemainsOnlySearchCreationPath`;
- `stage8WiringStrictHandoffAfterConfirmedReplyStillCreatesSearch`;
- `completeExplicitAssistantMessageCreatesSearchAndExposesRankedOffers`;
- `explicitHotelSearchHandoffStillCreatesSearchWhenLlmWouldProceed`.

**Verdict**: Stage 7 strict handoff remains единственный current automatic
search creation path.

## 4. Actual execution blockers

| # | Blocker | Description |
|---|---|---|
| B1 | **No execution call.** `PlanConfirmedSearchExecutionUseCase` returns `PreparedButNotExecuted`. No `CreateHotelSearchUseCase` invocation. |
| B2 | **No SUCCEEDED recording.** Attempt stays IN_PROGRESS. No `markSucceeded` with real `HotelSearchId`. |
| B3 | **No successful-results mapper.** Mapper maps ALL results to non-results. No `SHOW_HOTEL_RESULTS` mapping. |
| B4 | **No conditional consume.** Composition always returns `DO_NOT_CONSUME`. No consume-after-success. |
| B5 | **Transitioned lacks HotelSearchId.** `Transitioned` result carries attempt but not direct `HotelSearchId` for response. |

Blockers B1-B5 form a dependency chain:

```
B1 (execution call) → B2 (SUCCEEDED recording) → B5 (HotelSearchId in Transitioned) → B3 (mapper support) → B4 (consume-after-success)
```

## 5. Consume ordering readiness

`markConsumed` can be safely introduced только после:

1. Actual `CreateHotelSearchUseCase` call succeeds (B1 resolved).
2. Attempt `markSucceeded` records `HotelSearchId` (B2 resolved).
3. Composition checks `SUCCEEDED` status or `CONSUME_AFTER_SUCCESSFUL_RECORDING` decision.

Edge cases:

| Scenario | Consume behavior |
|---|---|
| `CreateHotelSearchUseCase` succeeds | Consume after `markSucceeded`. |
| `CreateHotelSearchUseCase` fails | No consume. Attempt → `FAILED(SEARCH_CREATION_FAILED)`. Retry allowed. |
| `CreateHotelSearchUseCase` unknown state | No consume. Attempt → `FAILED(EXECUTION_STATE_UNKNOWN)`. Retry blocked. |
| Duplicate SUCCEEDED with real search id | Consume if pending still active. Replay `hotelSearchId`. |
| Duplicate IN_PROGRESS | No consume. ALREADY_PROCESSING text. |

**Verdict**: consume ordering policy is clear, но implementation blocked
до B1-B2 resolution.

## 6. Response contract readiness

`show_hotel_results` can be returned после:

1. Actual search creation with real `HotelSearchId` (B1 resolved).
2. `Transitioned(SUCCEEDED)` or `DuplicateDetected(SUCCEEDED)` with real search id (B2, B5 resolved).
3. Mapper produces `SHOW_HOTEL_RESULTS` + `hotelSearchId` (B3 resolved).

**OpenAPI**: already supports `show_hotel_results` и `hotelSearchId` from
Stage 7 strict handoff. No OpenAPI change needed.

**Frontend**: already handles `show_hotel_results` from Stage 7.51. No
frontend change needed.

**Verdict**: response contract ready for future successful-results wiring.
No OpenAPI/frontend changes required.

## 7. Recommended next-stage sequence

| Stage | Scope | Risk profile |
|---|---|---|
| **8.53** | Execution result model + successful-results mapper support. Update `ExecuteConfirmedSearchTransitionUseCase` to accept optional `CreateHotelSearchUseCase` dependency. Produce `SearchCreated` result. Update `Transitioned` to carry `HotelSearchId`. Update mapper to produce `SHOW_HOTEL_RESULTS` for `SUCCEEDED`. No actual execution call. No route wiring. No consume. | Low — model/mapper only. |
| **8.54** | Actual execution call + SUCCEEDED recording. Wire `CreateHotelSearchUseCase` into `ExecuteConfirmedSearchTransitionUseCase`. Produce `SearchCreated` → `markSucceeded` → `SUCCEEDED` with real `HotelSearchId`. Update route tests. No `markConsumed`. | Medium — adds real execution. |
| **8.55** | Consume-after-success policy + route tests. Update `ComposeConfirmedSearchTransitionResponseUseCase` to conditionally consume when `SUCCEEDED`. Route tests prove `markConsumed` only after success. Route tests prove pending active for failed/duplicate. | Medium — adds consume behavior. |

**Sizing assessment**:

- Stage 8.53 — medium-small: model + mapper changes within one internal boundary.
- Stage 8.54 — narrow: actual execution call is a separate risk profile. Must not combine with 8.53.
- Stage 8.55 — narrow: consume-after-success is a separate risk profile. Must not combine with 8.54.

**What must remain separate** (per AGENTS.md Stage Sizing Policy):

- Runtime wiring (already done in 8.50) + actual execution (8.54);
- Actual execution (8.54) + `markConsumed` (8.55);
- `show_hotel_results` response (emerges in 8.54) + consume (8.55).

## 8. Medium-small stage sizing assessment

Stage 8.53 can be medium-small: model changes + mapper changes + tests within
one internal boundary. No runtime touching.

Stage 8.54 must be narrow: first actual `CreateHotelSearchUseCase` call
from confirmation flow is a significant risk boundary.

Stage 8.55 must be narrow: first `markConsumed` from new execution path
is a significant risk boundary.

## 9. Explicit non-goals

Stage 8.52 не создаёт и не меняет:

- Production code.
- Tests.
- Runtime/routes/API/OpenAPI/frontend/generated clients.
- Roadmap/status/product/architecture docs.
- Actual execution implementation.
- Consume ordering implementation.
- Successful-results mapping implementation.

## 10. Validation

Review-only inspection:

- `AssistantLlmRouteWiringUseCase` — `Confirmed` branch uses composition; no `markConsumed`; no `CreateHotelSearchUseCase`.
- `ComposeConfirmedSearchTransitionResponseUseCase` — always returns `DO_NOT_CONSUME`; safe text only.
- `ExecuteConfirmedSearchTransitionUseCase` — `transitionExecution` calls `markInProgress` only; no `markSucceeded`; no execution call.
- Stage 7 strict handoff tests pass.
- `git status --short` — working tree clean.
- Tests не запускались: stage is docs-only review/design; production code и tests не менялись.

## 11. Verdict

**Passed with notes** — current wiring is safe, actual execution still blocked.

Stage 8.52 подтверждает, что post-wiring runtime state после Stage 8.50
safe и consistent. `Confirmed` branch returns non-results PROCESSING text,
pending remains active, duplicate returns ALREADY_PROCESSING, Stage 7 strict
handoff unchanged. Actual execution blocked из-за dependency chain
(B1→B2→B5→B3→B4). Recommended sequence: 8.53 (model/mapper), 8.54
(actual execution), 8.55 (consume-after-success). Каждый stage separate
per AGENTS.md Stage Sizing Policy. OpenAPI и frontend already support
future successful-results response; no contract changes needed.
