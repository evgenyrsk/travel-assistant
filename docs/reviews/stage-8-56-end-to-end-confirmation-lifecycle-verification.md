# Stage 8.56 — End-to-End Confirmation Lifecycle Verification

## 1. Scope

Stage 8.56 — medium-small docs-only review/design stage. Проверяет
и документирует полный Stage 8 confirmation lifecycle после Stage 8.55:

1. pending confirmation creation from LLM proceed candidate;
2. confirmation prompt delivery;
3. user confirmation "да" routing;
4. local search creation;
5. successful attempt recording;
6. `show_hotel_results` response with `hotelSearchId`;
7. pending confirmation consumption;
8. duplicate/failure/non-success safety;
9. Stage 7 compatibility.

Stage 8.56 не меняет production code, tests, runtime, routes, API,
OpenAPI, frontend, generated clients или roadmap/status/product/
architecture docs.

## 2. Current end-to-end lifecycle

После Stage 8.55 confirmation lifecycle состоит из следующих шагов:

```
User message (hotel request)
  → LLM returns INTERPRETED candidate (complete constraints)
  → ProceedWithCandidate decision
  → PlanProceedWithCandidateConfirmationUseCase
  → ConfirmationRequired plan
  → savePendingConfirmation (PENDING, 15-min TTL)
  → Confirmation prompt via ask_clarification
  → User "да"
  → findActiveBySession (active pending found)
  → PlanPostConfirmationDecisionUseCase → Confirmed(criteria)
  → ComposeConfirmedSearchTransitionResponseUseCase
    → ExecuteConfirmedSearchTransitionUseCase
      → PlanConfirmedSearchCreationUseCase → ReadyToCreateSearch
      → BuildConfirmedSearchCreationCommandUseCase → CommandReady
      → PlanConfirmedSearchExecutionGuardUseCase → AllowedButBlocked
      → PlanConfirmedSearchExecutionAttemptUseCase → Prepared
      → savePrepared → Stored (PREPARED)
      → markInProgress → Stored (IN_PROGRESS)
      → hotelSearchBoundary.createSearch(command) → HotelSearch
      → markSucceeded(key, searchId, now) → Stored (SUCCEEDED)
      → Transitioned(SearchCreated(searchId), CONSUME_AFTER_SUCCESSFUL_RECORDING)
    → MapToDirective → SHOW_HOTEL_RESULTS + RESULTS_READY + hotelSearchId
    → consumeInstruction → CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS
  → consumePendingConfirmation(decidedAt) → markConsumed → CONSUMED
  → Response: show_hotel_results + hotelSearchId + "The search is ready."
  → Pending: consumed (findActiveBySession → null)
```

## 3. Happy path verification

### 3.1 Pending confirmation creation

**Механизм**: LLM returns `INTERPRETED` с complete constraints →
`PlanAssistantLlmDecisionUseCase` → `ProceedWithCandidate` →
`PlanProceedWithCandidateConfirmationUseCase` →
`ConfirmationRequired`.

**Store**: `savePendingConfirmation` in `AssistantLlmRouteWiringUseCase:183-197`
creates `PendingProceedWithCandidateConfirmation` with `PENDING` status,
`createdAt`, `updatedAt`, `expiresAt` (+15 min TTL).

**Response**: `ask_clarification` с `confirmationPromptMessage()`
(summary + confirmationQuestion). No `hotelSearchId`, no
`show_hotel_results`.

**Test proof**: `llmProceedCandidateReturnsConfirmationPromptWithoutCreatingHotelSearch`
(asserts pending criteria, PENDING status, TTL, proposal summary,
no hotel search created).

**Status**: Completed.

### 3.2 Confirmation routing

**Механизм**: User "да" → `acceptUserMessage` →
`explicitHotelSearchMessageParser.parse` returns `NotRequested` →
`findActiveBySession(sessionId, now)` → active pending found →
`PlanPostConfirmationDecisionUseCase` →
`ClassifyConfirmationReplyUseCase("да")` → `ExplicitPositive` →
`PostConfirmationDecision.Confirmed(criteria)`.

**Code**: `AssistantLlmRouteWiringUseCase:36-51` — pending check
и post-confirmation decision routing.

**Test proof**: `positiveConfirmationReplyConsumesPendingAfterSuccessfulSearchCreation`
(pending active before reply, triggers confirmed path).

**Status**: Completed.

### 3.3 Local search execution

**Механизм**: `PostConfirmationDecision.Confirmed` →
`withPostConfirmationDecision` → `composeTransitionResponse` →
`ComposeConfirmedSearchTransitionResponseUseCase` →
`ExecuteConfirmedSearchTransitionUseCase`:

1. `planSearchCreation(decision)` → `ReadyToCreateSearch`
2. `buildCommand(sessionId, plan)` → `CommandReady`
3. `guardUseCase(guardRequest)` → `AllowedButBlockedUntilIdempotencyGuard`
4. `findByIdempotencyKey(key, now)` → `null` (first attempt)
5. `planAttempt(guardResult, now, null)` → `AttemptPreparedButExecutionBlocked`
6. `savePrepared(attempt)` → `Stored(PREPARED)`
7. `markInProgress(key, now)` → `Stored(IN_PROGRESS)`
8. `hotelSearchBoundary.createSearch(command)` → `HotelSearch(id)`
9. `markSucceeded(key, createdSearch.id, now)` → `Stored(SUCCEEDED)`
10. Return `Transitioned(SearchCreated(searchId), CONSUME_AFTER_SUCCESSFUL_RECORDING)`

**Code**: `ExecuteConfirmedSearchTransitionUseCase:90-150` —
`executeSearchCreation` method.

**Test proof**: Route test asserts `show_hotel_results` and non-blank
`hotelSearchId`. Unit tests in `ExecuteConfirmedSearchTransitionUseCaseTest`
assert `createSearch` called once, attempt `SUCCEEDED`, `SearchCreated(searchId)` returned.

**Status**: Completed.

### 3.4 Successful attempt recording

**Механизм**: `markSucceeded` in `InMemoryConfirmedSearchExecutionAttemptStore:81-112`
transitions `IN_PROGRESS` → `SUCCEEDED` with `createdSearchId` and
`failureReason = null`.

**Idempotency**: attempt keyed by `ConfirmedSearchExecutionIdempotencyKey`,
derived from command plan (sessionId + criteria). Same key never
produces two searches.

**Test proof**: `successfulTransitionCreatesSearchAndRecordsSucceeded`
(unit test). Route test asserts offers accessible via `hotelSearchId`.

**Status**: Completed.

### 3.5 Results response

**Механизм**: `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase:29-45`:
- `Transitioned` with `SearchCreated` → `SHOW_HOTEL_RESULTS` +
  `RESULTS_READY` + `hotelSearchId` + `mayShowHotelResults=true` +
  `shouldConsumePendingConfirmation=true`.

`ComposeConfirmedSearchTransitionResponseUseCase:22-29`:
- `messageText` = `RESULTS_READY_MESSAGE` ("The search is ready. Hotel results are available.")
- `pendingConsumeInstruction` = `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS`
- `hotelSearchId` propagated from directive.

`AssistantLlmRouteWiringUseCase:135-144`:
- `SHOW_HOTEL_RESULTS` → `AssistantNextAction.SHOW_HOTEL_RESULTS` +
  `hotelSearchId` in response.

**Response shape**:

| Field | Value |
|---|---|
| `nextAction` | `show_hotel_results` |
| `hotelSearchId` | Real local search id |
| `assistantMessage.content` | "The search is ready. Hotel results are available." |

**Test proof**: `positiveConfirmationReplyConsumesPendingAfterSuccessfulSearchCreation`,
`stage8CompatibilityFullConfirmationCycleCreatesHotelSearchWithResults`.

**Status**: Completed.

### 3.6 Pending consumption

**Механизм**: `AssistantLlmRouteWiringUseCase:130-134`:

```
if (composedResult.pendingConsumeInstruction ==
    PendingConsumeInstruction.CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS) {
    consumePendingConfirmation(decidedAt)
}
```

`consumePendingConfirmation` calls `pendingConfirmationStore.markConsumed(sessionId, consumedAt)`.
`InMemoryPendingConfirmationStore:25-37` sets status to `CONSUMED`.

After consumption: `findActiveBySession` returns `null`.

**Trigger**: explicit `PendingConsumeInstruction`, not message text,
nextAction string, or hotelSearchId presence.

**Test proof**: `positiveConfirmationReplyConsumesPendingAfterSuccessfulSearchCreation`
(asserts `activePendingAfterReply == null`).

**Status**: Completed.

## 4. Failure and non-success behavior

| Path | Pending consumed? | Response | Retry? |
|---|---|---|---|
| `GuardRejected` | No | `ASK_CLARIFICATION` + `CONFIRMATION_REJECTED` | No |
| `StoreRejected` | No | `ASK_CLARIFICATION` + `TEMPORARY_FAILURE` | No |
| `DuplicateDetected(IN_PROGRESS)` | No | `ASK_CLARIFICATION` + `ALREADY_PROCESSING` | No |
| `FAILED(SEARCH_CREATION_FAILED)` | No | `ASK_CLARIFICATION` + `TEMPORARY_FAILURE` | Yes |
| `FAILED(STALE_EXECUTION)` | No | `ASK_CLARIFICATION` + `ALREADY_PROCESSING` | Yes |
| `FAILED(EXECUTION_STATE_UNKNOWN)` | No | `ASK_CLARIFICATION` + `ALREADY_PROCESSING` | No |
| `NeedsClarification` | No | `CONFIRMATION_NEEDS_CLARIFICATION_MESSAGE` | N/A |
| `Declined` | Yes | `CONFIRMATION_DECLINED_MESSAGE` | N/A |
| `NeedsReplanning` | Yes | `CONFIRMATION_REPLANNING_MESSAGE` | N/A |
| `Unknown` | No | `CONFIRMATION_UNKNOWN_REPLY_MESSAGE` | N/A |
| `NoActivePendingConfirmation` | No | `NO_ACTIVE_CONFIRMATION_MESSAGE` | N/A |

**`markConsumed` scope**: called for:
1. `Confirmed` with `CONSUME_PENDING_CONFIRMATION_AFTER_SUCCESS` (success only).
2. `Declined` (explicit user rejection).
3. `NeedsReplanning` (explicit user correction).

**`markConsumed` NOT called** for: `GuardRejected`, `StoreRejected`,
`DuplicateDetected(IN_PROGRESS)`, `NeedsClarification`, `Unknown`,
`FAILED` (all reasons), or any non-success transition result.

**Test proof**:
- `ambiguousConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch` — "ок" → pending active.
- `negativeConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` — "нет" → consumed.
- `correctionConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` — "лучше Париж" → consumed.
- `unknownConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch` — unknown → pending active.

**Status**: Completed.

## 5. Idempotency and duplicate behavior

| Scenario | Second search created? | Behavior |
|---|---|---|
| Duplicate "да" after consumed success | No | Pending consumed → `findActiveBySession` = `null` → LLM path → `show_boundary_message`. |
| Duplicate "да" while IN_PROGRESS | No | `DuplicateDetected(IN_PROGRESS)` → `ALREADY_PROCESSING`. |
| Duplicate "да" after SUCCEEDED (not consumed) | No | `DuplicateDetected(SUCCEEDED)` → `SHOW_HOTEL_RESULTS` + same `hotelSearchId`. |
| Retry after FAILED(SEARCH_CREATION_FAILED) | Yes (new attempt) | Retry allowed → new PREPARED → execution → SUCCEEDED. |
| Retry after FAILED(STALE_EXECUTION) | Yes (new attempt) | Retry allowed → new PREPARED → execution → SUCCEEDED. |
| Retry after FAILED(EXECUTION_STATE_UNKNOWN) | No | Retry blocked → `DuplicateDetected(FAILED)`. |

**Key property**: consumed pending means no active confirmation exists.
Repeated "да" does not create duplicate search and does not reuse
consumed pending.

**Idempotency key**: `ConfirmedSearchExecutionIdempotencyKey.from(commandPlan)`
derived from session + criteria. Same criteria for same session produces
same key.

**Test proof**:
- `repeatedConfirmationAfterConsumedSuccessGoesThroughLlmPath` — second "да" → LLM path → `show_boundary_message` → no duplicate search → pending `null`.
- Unit: `duplicateAfterSuccessDoesNotCreateSecondSearch`, `duplicateAfterSuccessReusesExistingHotelSearchId`.

**Status**: Completed.

## 6. Stage 7 compatibility

**Механизм**: `explicitHotelSearchMessageParser.parse(command.message)`
runs BEFORE pending confirmation check (`AssistantLlmRouteWiringUseCase:30-33`).
If message matches strict `hotel-search;` format, `acceptUserMessage`
returns immediately via `AssistantHotelSearchHandoffUseCase`, which
calls `CreateHotelSearchUseCase` directly.

**Independence**: Stage 7 strict handoff:
- Not affected by pending confirmation state.
- Not affected by Stage 8 confirmation lifecycle.
- Creates search via `CreateHotelSearchUseCase`.
- Returns `show_hotel_results` + `hotelSearchId`.
- Runs before any Stage 8 code path.

**Test proof**:
- `completeExplicitAssistantMessageCreatesSearchAndExposesRankedOffers` — strict handoff creates search.
- `explicitHotelSearchHandoffStillCreatesSearchWhenLlmWouldProceed` — strict handoff even when LLM would proceed.
- `stage8CompatibilityStrictHandoffAfterConfirmationStillCreatesSearch` — strict handoff after confirmation creates different search.
- `stage8WiringStrictHandoffAfterConfirmedReplyStillCreatesSearch` — strict handoff after confirmed "да" still works.

**Status**: Completed.

## 7. Contract/API/frontend assessment

**Backend response shape**: uses existing `show_hotel_results` +
`hotelSearchId` contract established in Stage 7.50 (strict handoff).
No new response fields introduced by Stage 8 confirmation flow.

**OpenAPI**: already supports `show_hotel_results` and `hotelSearchId`
from Stage 7. No OpenAPI change needed for Stage 8 confirmation results.

**Frontend**: Stage 7.51 frontend already handles `show_hotel_results`
response shape. No frontend change needed for basic contract support.

**Future UX work** (not blocking Stage 8 closure):
- Confirmation prompt UX (rich card instead of text-only);
- Inline retry UX for failed search creation;
- Progress/loading states for confirmation execution;
- Failure/retry user messaging polish;
- Correction flow UX (inline criteria editing).

**Status**: Completed (backend contract sufficient). Frontend UX polish is future work.

## 8. Stage 8 completion assessment

### Completed now

| Goal | Evidence |
|---|---|
| Pending confirmation for proceed candidate | Stage 8.15-8.22 |
| Confirmation reply classification | Stage 8.18-8.20 |
| Post-confirmation decision composition | Stage 8.20-8.24 |
| Confirmed-to-search planning/command/guard | Stage 8.25-8.37 |
| Attempt store with lifecycle transitions | Stage 8.38-8.44 |
| Execute transition orchestration | Stage 8.40-8.48 |
| Response mapping skeleton | Stage 8.46-8.48 |
| Non-results route wiring | Stage 8.50 |
| Successful result model/mapper | Stage 8.53 |
| Actual execution + SUCCEEDED recording | Stage 8.54 |
| Consume-after-success policy | Stage 8.55 |
| End-to-end happy path | This review |
| Failure/non-success safety | This review |
| Idempotency/duplicate safety | This review |
| Stage 7 compatibility | This review + Stage 8.45 |

### Still incomplete but acceptable for Stage 8

| Item | Reason acceptable |
|---|---|
| Process-local stores (InMemory) | Durable persistence is future infrastructure work. |
| FakeLlmClient (deterministic) | Real LLM provider integration is future work. |
| FakeHotelOfferProvider | Real hotel provider integration is future work. |
| Static message text | Production-grade error/UX copy is future work. |

### Blocking before Stage 8 closure

None identified. Core backend confirmation flow is complete and safe.

### Future/post-MVP

| Item | Category |
|---|---|
| Real external hotel provider integration | Provider |
| Durable persistence (PostgreSQL, Redis) | Infrastructure |
| Real LLM/provider behavior | Provider |
| Frontend UX polish (rich cards, inline retry) | UX |
| Booking flow | Product |
| Auth/API keys | Security |
| Production observability | Operations |
| Production-grade error copy | UX |
| Full UX around retry/failure | UX |
| Attempt store cleanup/TTL enforcement | Infrastructure |
| Generated client conformance | Tooling |

## 9. Remaining gaps

No correctness gaps identified in the core backend confirmation lifecycle.

Open items are all future work (providers, persistence, UX, security,
observability) and do not block Stage 8 backend closure.

## 10. Recommended next-stage sequence

| Stage | Scope | Type | Sizing |
|---|---|---|---|
| **8.57** | Stage 8 closure/readiness gate: formally assess Stage 8 closure, document carryover to Stage 9+, update roadmap status. | Review/design-only | Medium-small |
| **8.58** (if needed) | Polish/coverage: narrow implementation for any remaining test coverage, edge-case hardening, or message text improvements discovered during 8.57. | Implementation | Narrow |
| **8.59** (if needed) | Final Stage 8 closure commit after polish. | Review/design-only | Narrow |

**Assessment**: Stage 8.57 can be a closure/readiness gate because:
- Core backend confirmation lifecycle is verified complete.
- No blocking correctness gaps exist.
- All remaining work is future/post-MVP categories.
- Stage 7 compatibility is proven.

If Stage 8.57 identifies minor polish items, a Stage 8.58 narrow
implementation step can address them before final closure.

## 11. Explicit non-goals

Stage 8.56 не создаёт и не меняет:

- Production code.
- Tests.
- Runtime/routes/API/OpenAPI/frontend/generated clients.
- Roadmap/status/product/architecture docs.
- Provider/network/auth/booking behavior.
- Durable storage.
- Real LLM integration.
- Frontend UX.

## 12. Validation

Review-only inspection:

- `AssistantLlmRouteWiringUseCase` — pending save, post-confirmation decision, conditional consume, Stage 7 strict handoff priority.
- `ComposeConfirmedSearchTransitionResponseUseCase` — composition of execution + mapping + consume instruction.
- `ExecuteConfirmedSearchTransitionUseCase` — full execution flow: guard → attempt → store → markInProgress → createSearch → markSucceeded.
- `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase` — all result variants mapped correctly.
- `InMemoryPendingConfirmationStore` — save, findActive, markConsumed.
- `InMemoryConfirmedSearchExecutionAttemptStore` — savePrepared, markInProgress, markSucceeded, markFailed, stale detection.
- `PlanPostConfirmationDecisionUseCase` — reply classification → decision.
- Route tests — 20+ tests covering happy path, all failure paths, duplicate behavior, Stage 7 compatibility.
- `git status --short` — working tree clean.
- Tests не запускались: stage is docs-only review/design; production code и tests не менялись.

## 13. Verdict

**Passed with notes** — backend lifecycle is complete, production
readiness remains future work.

Stage 8.56 подтверждает, что полный end-to-end confirmation lifecycle
после Stage 8.55 safe и complete: pending confirmation создаётся для
proceed candidate; "да" triggers confirmed-search execution через
local `CreateHotelSearchUseCase`; successful creation records `SUCCEEDED`
с real `hotelSearchId`; route returns `show_hotel_results` + `hotelSearchId`;
pending confirmation consumed after success; failed/non-success paths
keep pending active; duplicate after consumed success goes through
LLM path without duplicate search; Stage 7 strict `hotel-search;`
handoff remains independent и unchanged. Backend core confirmation
flow closeable. Production readiness (real providers, persistence,
LLM, UX, auth, observability) remains future work и не blocks
Stage 8 closure.
