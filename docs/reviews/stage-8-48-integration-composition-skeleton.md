# Stage 8.48 — Integration Composition Skeleton

## 1. Scope

Stage 8.48 добавляет internal composition boundary, который связывает
orchestration, response mapping и safe message planning в единый use case:

- `ComposeConfirmedSearchTransitionResponseRequest` — input model;
- `ComposeConfirmedSearchTransitionResponseResult` — output model;
- `PendingConsumeInstruction` — explicit consume instruction enum;
- `ComposeConfirmedSearchTransitionResponseUseCase` — composition use case;
- narrow unit tests.

Stage 8.48 не меняет runtime behavior, routes, API, OpenAPI, frontend,
generated clients и не подключает composition к runtime.

## 2. Implemented changes

Production files добавлены:

- `ComposeConfirmedSearchTransitionResponseRequest.kt` — request data class.
- `ComposeConfirmedSearchTransitionResponseResult.kt` — result data class + `PendingConsumeInstruction` enum.
- `ComposeConfirmedSearchTransitionResponseUseCase.kt` — composition use case с safe message text planning.

Test files добавлены:

- `ComposeConfirmedSearchTransitionResponseUseCaseTest.kt` — 9 narrow unit tests.

Не менялись:

- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- assistant routes;
- API/OpenAPI/frontend/generated clients;
- existing Stage 8 classes (кроме new files).

## 3. Composition boundary

`ComposeConfirmedSearchTransitionResponseUseCase` — internal-only composition
use case. Он связывает:

1. `ExecuteConfirmedSearchTransitionUseCase` — orchestration;
2. `MapConfirmedSearchTransitionResultToResponseDirectiveUseCase` — response mapping;
3. Safe message text planning — typed message kind → internal placeholder text;
4. Explicit non-consume instruction — `DO_NOT_CONSUME_PENDING_CONFIRMATION`.

Use case принимает typed request и возвращает typed composed result. Он не
подключён к routes/runtime.

## 4. Input and output model

**Request** (`ComposeConfirmedSearchTransitionResponseRequest`):

| Field | Type | Purpose |
|---|---|---|
| `sessionId` | `AssistantSessionId` | Session identifier. |
| `decision` | `PostConfirmationDecision.Confirmed` | Confirmed decision input. |
| `pendingConfirmation` | `PendingProceedWithCandidateConfirmation?` | Read-only pending snapshot. |
| `now` | `Instant` | Deterministic clock. |

**Result** (`ComposeConfirmedSearchTransitionResponseResult`):

| Field | Type | Purpose |
|---|---|---|
| `transitionResult` | `ExecuteConfirmedSearchTransitionResult` | Orchestration outcome. |
| `responseDirective` | `ConfirmedSearchTransitionResponseDirective` | Response directive. |
| `messageText` | `String` | Safe internal placeholder text. |
| `pendingConsumeInstruction` | `PendingConsumeInstruction` | Explicit consume instruction. |

**`PendingConsumeInstruction`**:

| Value | Purpose |
|---|---|
| `DO_NOT_CONSUME_PENDING_CONFIRMATION` | Explicit instruction: do not consume pending confirmation. Single value for all current fake/no-op states. |

## 5. Composition behavior

Composition flow:

```
Request
  → ExecuteConfirmedSearchTransitionUseCase(request → transitionRequest)
  → ExecuteConfirmedSearchTransitionResult
  → MapConfirmedSearchTransitionResultToResponseDirectiveUseCase(result)
  → ConfirmedSearchTransitionResponseDirective
  → safeMessageText(directive.messageKind)
  → ComposeConfirmedSearchTransitionResponseResult
      (transitionResult, directive, messageText, DO_NOT_CONSUME)
```

Safe message text mapping:

| Message kind | Internal placeholder text |
|---|---|
| `PROCESSING` | "I am preparing that search, but results are not available yet." |
| `ALREADY_PROCESSING` | "That search is already being prepared." |
| `CONFIRMATION_REJECTED` | "I could not proceed with the current confirmation state." |
| `TEMPORARY_FAILURE` | "I could not record the search transition. Please try again." |

Все тексты — internal placeholders. Они не claim'ят actual hotel search
results, не expose `hotelSearchId`, не reference `show_hotel_results`.

## 6. Consume behavior

Composition use case **всегда** возвращает
`PendingConsumeInstruction.DO_NOT_CONSUME_PENDING_CONFIRMATION`.

Это explicit non-consume behavior:

- use case не вызывает `markConsumed`;
- use case не модифицирует pending confirmation store;
- use case не предоставляет механизм consume для caller;
- pending consume остаётся blocked до отдельного actual execution stage.

Для всех current fake/no-op/rejected outcomes:

- `shouldConsumePendingConfirmation` в directive = `false`;
- `pendingConsumeInstruction` = `DO_NOT_CONSUME_PENDING_CONFIRMATION`;
- pending confirmation store не мутирован.

## 7. Runtime/API safety

Composition use case не подключён к routes/runtime composition.

- `Application.kt` не изменён.
- `AssistantLlmRouteWiringUseCase` не изменён.
- `CreateHotelSearchUseCase` не вызывается.
- Real `hotelSearchId` не создаётся.
- `show_hotel_results` не возвращается из runtime.
- `markConsumed` не wire'ится и не вызывается.
- Stage 7 strict `hotel-search;` handoff unchanged.
- Public API/OpenAPI не менялись.

## 8. Tests

Добавлен `ComposeConfirmedSearchTransitionResponseUseCaseTest` с 9 tests:

| Test | Что проверяет |
|---|---|
| `fakeNoOpTransitionComposesProcessingMessageWithoutConsume` | Transitioned → PROCESSING message; DO_NOT_CONSUME. |
| `composedResultDoesNotIncludeRealHotelSearchId` | No `hotelSearchId` in directive or transition result. |
| `composedResultDoesNotRequestHotelResults` | `mayShowHotelResults = false`; `ASK_CLARIFICATION`. |
| `composedResultDoesNotRequestPendingConsume` | `DO_NOT_CONSUME`; `shouldConsumePendingConfirmation = false`. |
| `duplicateInProgressComposesAlreadyProcessingMessage` | Duplicate IN_PROGRESS → ALREADY_PROCESSING. |
| `guardRejectedComposesConfirmationRejectedMessageWithoutConsume` | GuardRejected → CONFIRMATION_REJECTED; DO_NOT_CONSUME. |
| `compositionDoesNotRequireCreateHotelSearchUseCase` | Composition works without `CreateHotelSearchUseCase`. |
| `compositionDoesNotMutatePendingConfirmationStore` | Pending store unchanged after composition. |
| `composedResultDoesNotExposeForbiddenTokens` | No `show_hotel_results`, `SHOW_HOTEL_RESULTS`, `CreateHotelSearchUseCase`, `markConsumed`, `provider` in result text. |

Все tests pass (BUILD SUCCESSFUL).

## 9. Explicit non-goals

Stage 8.48 не создаёт и не меняет:

- Route wiring или runtime composition.
- `Application.kt`, `AssistantLlmRouteWiringUseCase`, assistant routes.
- `CreateHotelSearchUseCase` call.
- Real `hotelSearchId` creation.
- `show_hotel_results` response from runtime.
- `markConsumed` wiring или вызов.
- Pending confirmation store mutation.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.
- Actual hotel search execution.
- Successful-results mapping.
- Stage 7 strict handoff changes.

## 10. Validation

- `git status --short`: подтверждено.
- `git diff --check`: no errors.
- `./gradlew test --no-daemon`: BUILD SUCCESSFUL.
- Inspection: composition files not referenced from `Application.kt` или `AssistantLlmRouteWiringUseCase`; no `CreateHotelSearchUseCase` dependency; no `show_hotel_results`/`hotelSearchId`/`markConsumed` leakage in composed result.

## 11. Verdict

**Passed with notes** — composition added, consume and successful-results
remain blocked.

Stage 8.48 добавил internal integration composition skeleton:
`ComposeConfirmedSearchTransitionResponseUseCase` связывает orchestration,
response mapping и safe message planning в единый composition boundary.
Все current fake/no-op/rejected outcomes возвращают explicit
`DO_NOT_CONSUME_PENDING_CONFIRMATION` instruction, safe placeholder text,
`null` hotelSearchId, и `mayShowHotelResults = false`. Composition не
подключён к runtime, не вызывает `CreateHotelSearchUseCase`, не создаёт
real search и не wire'ит `markConsumed`. Route wiring, consume ordering
и successful-results mapping остаются future work. Stage 7 strict handoff
unchanged.
