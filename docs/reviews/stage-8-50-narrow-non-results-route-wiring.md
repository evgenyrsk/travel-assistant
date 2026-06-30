# Stage 8.50 — Narrow Non-Results Route Wiring

## 1. Scope

Stage 8.50 подключает `ComposeConfirmedSearchTransitionResponseUseCase` к
`PostConfirmationDecision.Confirmed` branch внутри
`AssistantLlmRouteWiringUseCase`. Wiring ограничен non-results behavior:

- pending confirmation остаётся active (не consumed);
- response text берётся из composition;
- `nextAction` остаётся `ask_clarification`;
- `hotelSearchId` остаётся `null`;
- `show_hotel_results` не возвращается;
- `CreateHotelSearchUseCase` не вызывается;
- actual execution не добавляется.

## 2. Implemented changes

Production files изменены:

- `AssistantLlmRouteWiringUseCase.kt`:
  - добавлен `composeTransitionResponse` constructor parameter с default;
  - `withPostConfirmationDecision` принимает `activePendingConfirmation` parameter;
  - `Confirmed` branch: убран `consumePendingConfirmation`; добавлен composition call; response text = `composedResult.messageText`;
  - удалён `CONFIRMATION_RECEIVED_MESSAGE` constant (более не используется).

- `Application.kt`:
  - добавлен `InMemoryConfirmedSearchExecutionAttemptStore` и `ComposeConfirmedSearchTransitionResponseUseCase` в composition chain;
  - передан в `AssistantLlmRouteWiringUseCase` как `composeTransitionResponse`.

Test files изменены/добавлены:

- `AssistantSessionRoutesTest.kt`:
  - `positiveConfirmationReplyConsumesPendingStateWithoutCreatingHotelSearch` → renamed to `positiveConfirmationReplyKeepsPendingActiveAndReturnsSafeProcessingText`; pending remains active; text = PROCESSING message.
  - `stage8CompatibilityFullConfirmationCycleDoesNotCreateHotelSearch` — updated: pending remains active; text = PROCESSING message.
  - Added: `repeatedConfirmationReplyReturnsAlreadyProcessingMessage` — second "да" returns ALREADY_PROCESSING.
  - Added: `stage8WiringStrictHandoffAfterConfirmedReplyStillCreatesSearch` — strict handoff creates search even after Stage 8.50 wiring.

Не менялись:

- API/OpenAPI contracts;
- frontend;
- generated clients;
- assistant routes;
- retry/stale behavior;
- response mapper semantics.

## 3. Runtime wiring behavior

`Confirmed` branch после Stage 8.50:

```
PostConfirmationDecision.Confirmed
  → ComposeConfirmedSearchTransitionResponseUseCase(request)
  → composedResult.messageText
  → withClarification(composedResult.messageText)
```

No `consumePendingConfirmation`. No `hotelSearchId`. No `show_hotel_results`.

## 4. Pending confirmation behavior

| Event | Pending behavior |
|---|---|
| `Confirmed` reply (first) | Pending remains `PENDING` / active. Not consumed. |
| `Confirmed` reply (repeated) | Pending remains active. Composition detects duplicate attempt → `ALREADY_PROCESSING`. |
| `NeedsClarification` reply | Pending remains active (unchanged). |
| `Declined` reply | Pending consumed (unchanged from previous stage). |
| `NeedsReplanning` reply | Pending consumed (unchanged from previous stage). |
| `Unknown` reply | Pending remains active (unchanged). |
| Pending TTL expiry | Existing 15-min TTL handles cleanup. |

## 5. Response contract

| Field | Value |
|---|---|
| `nextAction` | `ask_clarification` (unchanged). |
| `assistantMessage.content` | Composition message text: PROCESSING / ALREADY_PROCESSING / CONFIRMATION_REJECTED / TEMPORARY_FAILURE. |
| `hotelSearchId` | `null` / absent. |
| `show_hotel_results` | Forbidden / not returned. |
| OpenAPI | Unchanged. |

## 6. Stage 7 compatibility

Stage 7 strict `hotel-search;` handoff remains unchanged:

- `explicitHotelSearchMessageParser.parse` runs before pending confirmation check;
- strict handoff creates search and returns `show_hotel_results` + `hotelSearchId`;
- test `stage8WiringStrictHandoffAfterConfirmedReplyStillCreatesSearch` proves strict handoff works after Stage 8.50 wiring with different criteria (Paris instead of Rome).

## 7. Runtime/API safety

- `CreateHotelSearchUseCase` не вызывается из confirmation flow.
- Real `hotelSearchId` не создаётся после confirmation.
- `show_hotel_results` не возвращается после confirmation.
- `markConsumed` не вызывается в `Confirmed` branch.
- Provider/network calls не добавлены.
- Stage 7 strict handoff unchanged.

## 8. Tests

Updated tests (2):

| Test | Change |
|---|---|
| `positiveConfirmationReplyKeepsPendingActiveAndReturnsSafeProcessingText` | Pending remains active; text = PROCESSING; renamed from consumed variant. |
| `stage8CompatibilityFullConfirmationCycleDoesNotCreateHotelSearch` | Pending remains active after "да"; text = PROCESSING. |

New tests (2):

| Test | Что проверяет |
|---|---|
| `repeatedConfirmationReplyReturnsAlreadyProcessingMessage` | Second "да" → ALREADY_PROCESSING text; no `hotelSearchId`; offers 404; pending still active. |
| `stage8WiringStrictHandoffAfterConfirmedReplyStillCreatesSearch` | Strict handoff after Stage 8.50 confirmed reply still creates search + `show_hotel_results`. |

Все tests pass (BUILD SUCCESSFUL).

## 9. Explicit non-goals

Stage 8.50 не создаёт и не меняет:

- `CreateHotelSearchUseCase` call из confirmation flow.
- Actual `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` wiring в `Confirmed` branch.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.
- Successful-results mapping.
- Actual hotel search execution.
- Retry/stale behavior changes.

## 10. Validation

- `git status --short`: подтверждено.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: BUILD SUCCESSFUL.
- Inspection: `ComposeConfirmedSearchTransitionResponseUseCase` connected only to `Confirmed` branch; `CreateHotelSearchUseCase` not called from confirmation flow; `markConsumed` not called in new path.

## 11. Verdict

**Passed with notes** — non-results wiring added, consume/results remain blocked.

Stage 8.50 подключил `ComposeConfirmedSearchTransitionResponseUseCase` к
`Confirmed` branch. Pending confirmation intentionally remains active.
Response text теперь берётся из composition (PROCESSING для first
confirmation, ALREADY_PROCESSING для duplicate). `nextAction` остаётся
`ask_clarification`. `hotelSearchId` и `show_hotel_results` не
возвращаются. `CreateHotelSearchUseCase` не вызывается. `markConsumed`
не wire'ится в new path. Stage 7 strict handoff unchanged.
Successful-results behavior remains future work.
