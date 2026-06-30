# Stage 8.49 — Non-Results Route Wiring Readiness Gate

## 1. Scope

Stage 8.49 — review/design-only gate. Проверить, можно ли в Stage 8.50
безопасно сделать narrow non-results route wiring для
`ComposeConfirmedSearchTransitionResponseUseCase`, и зафиксировать
explicit wiring checklist.

Stage 8.49 не меняет production code, tests, runtime behavior, routes,
public API, OpenAPI, frontend, generated clients или roadmap/root status
files.

## 2. Current inspected state

После Stage 8.48 internal composition chain включает:

| Компонент | Stage | Runtime status |
|---|---|---|
| `ExecuteConfirmedSearchTransitionUseCase` | 8.40 | Internal; не подключён. |
| `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase` | 8.46 | Internal; не подключён. |
| `ComposeConfirmedSearchTransitionResponseUseCase` | 8.48 | Internal; не подключён. |
| `ComposeConfirmedSearchTransitionResponseResult` | 8.48 | `pendingConsumeInstruction = DO_NOT_CONSUME`. |
| `ConfirmedSearchTransitionResponseDirective` | 8.46 | Все current states: `ASK_CLARIFICATION`, `mayShowHotelResults = false`, `hotelSearchId = null`. |

Текущий runtime (`AssistantLlmRouteWiringUseCase`):

```kotlin
is PostConfirmationDecision.Confirmed -> {
    consumePendingConfirmation(decidedAt)
    withClarification(CONFIRMATION_RECEIVED_MESSAGE)
}
```

- `consumePendingConfirmation` → `pendingConfirmationStore.markConsumed`;
- response: `ask_clarification` + "Confirmation received. I will not start a hotel search automatically yet.";
- no `hotelSearchId`, no `show_hotel_results`.

Existing route test `positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch`:

- Asserts: pending `null` after "да" reply;
- Asserts: `ask_clarification` nextAction;
- Asserts: text = "Confirmation received...";
- Asserts: no `hotelSearchId`;
- Asserts: offers endpoint returns 404.

## 3. Key question

Можно ли в Stage 8.50 безопасно заменить current `Confirmed` branch на
composition use case call, сохранив safe non-results behavior?

## 4. Readiness assessment

### 4.1 Integration point

Integration point: `AssistantLlmRouteWiringUseCase.withPostConfirmationDecision`,
`PostConfirmationDecision.Confirmed` branch.

Current:

```kotlin
is PostConfirmationDecision.Confirmed -> {
    consumePendingConfirmation(decidedAt)
    withClarification(CONFIRMATION_RECEIVED_MESSAGE)
}
```

Future (Stage 8.50):

```kotlin
is PostConfirmationDecision.Confirmed -> {
    val composedResult = composeTransitionResponse(
        ComposeConfirmedSearchTransitionResponseRequest(
            sessionId = sessionId,
            decision = decision,
            pendingConfirmation = activePendingConfirmation,
            now = decidedAt,
        ),
    )
    withClarification(composedResult.messageText)
}
```

**Key changes**:

- Remove `consumePendingConfirmation(decidedAt)` — composition returns `DO_NOT_CONSUME`.
- Replace `CONFIRMATION_RECEIVED_MESSAGE` with `composedResult.messageText`.
- `nextAction` remains `ask_clarification`.
- `hotelSearchId` remains `null`.

**Wiring approach**: composition use case подключается через constructor
injection в `AssistantLlmRouteWiringUseCase`. Route code знает о composition
use case; composition не знает о route.

**Verdict**: integration point identified и well-defined. Wiring approach
is straightforward.

### 4.2 Consume ordering

Current behavior: `markConsumed` called immediately on `Confirmed`.

Composition behavior: `DO_NOT_CONSUME_PENDING_CONFIRMATION` — no consume.

**Implications of non-consume**:

| Concern | Analysis |
|---|---|
| Pending remains active after "да" | Yes. Pending stays `PENDING` status with existing TTL (15 min). |
| Subsequent "да" reply | Routes to `withPostConfirmationDecision` again → composition → `DuplicateDetected(IN_PROGRESS)` → `ALREADY_PROCESSING` message. |
| Pending expiry | Existing TTL (15 min) handles cleanup. No permanent stale state. |
| Retry after pending expiry | New LLM proceed candidate creates new pending. Existing attempt store state may block via duplicate detection (retry allowed for `STALE_EXECUTION`/`SEARCH_CREATION_FAILED`). |
| Duplicate/already-processing responses | Acceptable skeleton behavior. User sees "That search is already being prepared." |

**Stage 8.42 policy**: `markConsumed` допустим только после actual `SUCCEEDED`
recording с real `HotelSearchId`. Current skeleton не имеет actual `SUCCEEDED`.

**Verdict**: non-consume is correct per Stage 8.42 policy. Pending lifecycle
handled by existing TTL. Duplicate "да" replies produce safe
`ALREADY_PROCESSING` message.

**Required test change**: `positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch`
asserts `null` pending after reply. This test must be updated to assert
pending **remains active** after reply.

### 4.3 Response contract

| Field | Current | Stage 8.50 | Change? |
|---|---|---|---|
| `nextAction` | `ask_clarification` | `ask_clarification` | No. |
| `assistantMessage.content` | "Confirmation received..." | "I am preparing that search, but results are not available yet." | Yes — new safe text. |
| `hotelSearchId` | absent | absent | No. |
| `show_hotel_results` | forbidden | forbidden | No. |
| OpenAPI | unchanged | unchanged | No. |

**Text change analysis**: new message text does not claim hotel search
results. It says "preparing" which is consistent with internal `PROCESSING`
message kind. This is safe placeholder text.

**Verdict**: response contract remains safe non-results. Text changes from
"Confirmation received" to "I am preparing that search" — both safe,
neither claims results. No OpenAPI change needed.

### 4.4 Test requirements

**Existing tests that remain green** (no changes needed):

- `stage8CompatibilityFullConfirmationCycleDoesNotCreateHotelSearch` —
  asserts no `hotelSearchId`, no `show_hotel_results`, offers 404. Pending
  consumed assertion exists (`null` after reply) — **must be updated**.
- `stage8CompatibilityStrictHandoffRemainsOnlySearchCreationPath` —
  asserts strict handoff creates search after Stage 8 flow. No pending
  assertion affected.
- `ambiguousConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch` —
  not affected (different reply).
- `negativeConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` —
  not affected (different decision branch).
- `correctionConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` —
  not affected (different decision branch).
- `unknownConfirmationReplyKeepsPendingStateActiveWithoutCreatingHotelSearch` —
  not affected (different reply).

**Existing tests that must be updated**:

| Test | Current assertion | Required change |
|---|---|---|
| `positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` | `pendingAfterReply == null` | Assert pending **remains active**; update test name. |
| `stage8CompatibilityFullConfirmationCycleDoesNotCreateHotelSearch` | `pendingAfterReply == null` | Assert pending **remains active** after "да". |

**New tests needed for Stage 8.50**:

1. Non-results wiring: positive reply produces `PROCESSING` message text.
2. Non-results wiring: no `hotelSearchId` in response.
3. Non-results wiring: no `show_hotel_results` in response.
4. Non-results wiring: `markConsumed` not called (pending remains active).
5. Duplicate reply: second "да" produces `ALREADY_PROCESSING` message.
6. Stage 7 strict handoff: still creates search and returns `show_hotel_results` after Stage 8.50 wiring.
7. No `CreateHotelSearchUseCase` invocation from `Confirmed` branch (assert no search created, offers 404).

**Verdict**: test requirements are clear и bounded. 2 existing tests need
updates; 7 new tests needed.

### 4.5 Safety risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Non-consume causes duplicate processing | Medium | Low — produces safe `ALREADY_PROCESSING` message. | Acceptable skeleton limitation. |
| Non-consume causes pending to never expire | Low | None — existing 15-min TTL handles expiry. | TTL already implemented. |
| Text change breaks frontend expectations | Low | Low — `nextAction` unchanged; text is informational only. | No frontend contract change. |
| Composition use case has unexpected side effect | Low | Medium — composition may modify attempt store. | Attempt store is in-memory; side effects isolated to skeleton state. |
| Stage 7 strict handoff broken | Very Low | High — would break core search creation. | Strict handoff parser runs before pending check; unaffected. |

**Most significant risk**: behavioral change for `Confirmed` branch.
Currently: consume + text. After wiring: no consume + different text.
This is intentional per Stage 8.42 policy, but must be documented as
explicit behavior change.

## 5. Wiring checklist for Stage 8.50

| # | Item | Status |
|---|---|---|
| W1 | Replace `consumePendingConfirmation` + `CONFIRMATION_RECEIVED_MESSAGE` with composition call + `composedResult.messageText`. | Required. |
| W2 | Add `ComposeConfirmedSearchTransitionResponseUseCase` as constructor parameter in `AssistantLlmRouteWiringUseCase`. | Required. |
| W3 | Add `ConfirmedSearchExecutionAttemptStore` as constructor parameter (needed by composition chain). | Required. |
| W4 | Keep `nextAction = ask_clarification`. | Required. |
| W5 | Keep `hotelSearchId = null`. | Required. |
| W6 | Do NOT call `markConsumed` for `Confirmed` branch. | Required. |
| W7 | Update `positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch`: assert pending remains active; rename test. | Required. |
| W8 | Update `stage8CompatibilityFullConfirmationCycleDoesNotCreateHotelSearch`: assert pending remains active. | Required. |
| W9 | Add new test: non-results wiring produces PROCESSING message. | Required. |
| W10 | Add new test: duplicate "да" produces ALREADY_PROCESSING. | Required. |
| W11 | Add new test: no `CreateHotelSearchUseCase` invocation from Confirmed branch. | Required. |
| W12 | Add new test: Stage 7 strict handoff unchanged after wiring. | Required. |
| W13 | No OpenAPI changes. | Required. |
| W14 | No frontend changes. | Required. |

## 6. Blockers

| # | Blocker | Status |
|---|---|---|
| B1 | Test updates (W7, W8) required before wiring. | Resolvable in Stage 8.50. |
| B2 | Behavioral change documentation. | Must be explicit in Stage 8.50 review. |
| B3 | Constructor wiring for composition use case + attempt store. | Straightforward; no architectural blocker. |

**No architectural blockers remain.** All pieces are in place for narrow
non-results wiring.

## 7. Recommendation

Stage 8.50 may safely wire `ComposeConfirmedSearchTransitionResponseUseCase`
to `AssistantLlmRouteWiringUseCase` `Confirmed` branch **if**:

1. All wiring checklist items (W1-W14) are followed.
2. Existing test assertions about pending consumption are updated.
3. New wiring-specific tests are added.
4. Behavioral change is documented explicitly.

Wiring must remain **non-results only**:

- no `hotelSearchId`;
- no `show_hotel_results`;
- no `CreateHotelSearchUseCase` call;
- no `markConsumed`;
- no actual execution;
- `nextAction = ask_clarification`.

Successful-results wiring (with real search id and `show_hotel_results`)
remains blocked until actual execution stage.

## 8. Explicit non-goals

Stage 8.49 не создаёт и не меняет:

- Production code.
- Tests.
- Route wiring.
- `Application.kt`, `AssistantLlmRouteWiringUseCase`, assistant routes.
- `CreateHotelSearchUseCase` call.
- Actual `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` wiring.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.

## 9. Suggested next stage

Stage 8.50: **narrow non-results route wiring** (backend implementation).

Подключить `ComposeConfirmedSearchTransitionResponseUseCase` к
`AssistantLlmRouteWiringUseCase` `Confirmed` branch:

- replace immediate `markConsumed` with composition call;
- use `composedResult.messageText` as response text;
- keep `ask_clarification` nextAction;
- keep `hotelSearchId = null`;
- update 2 existing tests;
- add 5-7 new wiring-specific tests;
- document behavioral change.

Out of scope:

- `markConsumed` wiring.
- `CreateHotelSearchUseCase` call.
- `hotelSearchId` / `show_hotel_results`.
- Actual execution.
- OpenAPI/frontend changes.
- Successful-results mapping.

## 10. Validation

Review-only inspection:

- `ComposeConfirmedSearchTransitionResponseUseCase` — not referenced from `Application.kt` или `AssistantLlmRouteWiringUseCase`.
- `ExecuteConfirmedSearchTransitionUseCase` — not referenced from runtime.
- `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase` — not referenced from runtime.
- `AssistantLlmRouteWiringUseCase.withPostConfirmationDecision` — `Confirmed` branch uses immediate `consumePendingConfirmation`.
- Existing route test asserts pending becomes `null` after "да" — requires update in Stage 8.50.
- Stage 8.45 compatibility tests prove Stage 7 strict handoff unchanged.
- `git status --short` — working tree clean.

## 11. Verdict

**Passed with guarded path** — Stage 8.50 may wire safe non-results
composition.

Stage 8.49 подтверждает, что narrow non-results route wiring возможен в
Stage 8.50. Integration point identified (`withPostConfirmationDecision`
`Confirmed` branch). Consume ordering decision explicit (non-consume per
Stage 8.42 policy). Response contract safe (ask_clarification + safe text
+ no hotelSearchId + no show_hotel_results). Test requirements bounded
(2 updates + 5-7 new tests). No architectural blockers. Stage 7 strict
handoff remains unaffected. Successful-results wiring и `markConsumed`
remain blocked until actual execution stage.
