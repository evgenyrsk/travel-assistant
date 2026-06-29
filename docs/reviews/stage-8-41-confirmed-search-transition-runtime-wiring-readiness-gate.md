# Stage 8.41 — Confirmed Search Transition Runtime Wiring Readiness Gate

## 1. Scope

Stage 8.41 — review/design-only gate. Проверить, готов ли
`ExecuteConfirmedSearchTransitionUseCase` skeleton из Stage 8.40 к будущему
подключению в `AssistantLlmRouteWiringUseCase`, и определить, какие условия
должны быть выполнены перед route/runtime wiring.

Stage 8.41 не меняет production code, tests, runtime behavior, routes, public
API, OpenAPI, frontend, generated clients или roadmap/root status files.

## 2. Current inspected state

После Stage 8.40 internal confirmed-search chain включает:

| Компонент | Статус |
|---|---|
| `PlanPostConfirmationDecisionUseCase` | Подключён к runtime; возвращает `Confirmed(criteria)`. |
| `PlanConfirmedSearchCreationUseCase` | Internal skeleton; не подключён к runtime. |
| `BuildConfirmedSearchCreationCommandUseCase` | Internal skeleton; не подключён к runtime. |
| `PlanConfirmedSearchExecutionUseCase` | Internal skeleton; возвращает `PreparedButNotExecuted`. |
| `PlanConfirmedSearchExecutionGuardUseCase` | Internal skeleton; read-only guard. |
| `PlanConfirmedSearchExecutionAttemptUseCase` | Internal skeleton; attempt planning. |
| `InMemoryConfirmedSearchExecutionAttemptStore` | Process-local storage; не подключён к runtime. |
| `ExecuteConfirmedSearchTransitionUseCase` | Internal orchestration skeleton; не подключён к runtime. |

Текущий runtime (`AssistantLlmRouteWiringUseCase`):

- для `PostConfirmationDecision.Confirmed` немедленно вызывает
  `consumePendingConfirmation(decidedAt)`;
- возвращает text-only `CONFIRMATION_RECEIVED_MESSAGE` через
  `withClarification`;
- не использует `ExecuteConfirmedSearchTransitionUseCase`;
- не использует attempt store;
- не создаёт `hotelSearchId`;
- не возвращает `show_hotel_results`.

`ExecuteConfirmedSearchTransitionUseCase` не referenced из:

- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- assistant routes;
- любые другие runtime composition files.

## 3. Key question

Готов ли `ExecuteConfirmedSearchTransitionUseCase` skeleton к безопасному
подключению в `AssistantLlmRouteWiringUseCase` для обработки
`PostConfirmationDecision.Confirmed`, или перед wiring необходимы отдельные
policy/design steps?

## 4. Findings

### 4.1 Runtime wiring readiness

Runtime wiring **не безопасен** на текущем этапе.

Основные причины:

1. **Consume ordering conflict.** Текущий runtime вызывает `markConsumed`
   немедленно для `Confirmed`. Orchestration skeleton возвращает
   `PendingConsumptionDecision` как metadata, но не определяет, должен ли
   runtime заменить immediate consume на deferred consume. Без explicit
   policy wiring создаст ordering conflict: pending confirmation может быть
   consumed до того, как attempt recording завершится, или не consumed
   вообще, что приведёт к stale pending state.

2. **Integration point undefined.** Не определено, как
   `ExecuteConfirmedSearchTransitionUseCase` встраивается в
   `withPostConfirmationDecision`. Текущий `Confirmed` branch сразу
   consumes и возвращает text. Future wiring должен решить: заменить
   current branch, вызвать orchestration до consume, или вызвать
   orchestration вместо current branch.

3. **No response mapping.** `ExecuteConfirmedSearchTransitionResult`
   возвращает typed variants (`Transitioned`, `DuplicateDetected`,
   `GuardRejected`, `StoreRejected`), но нет mapping rules из этих
   variants в `AcceptedAssistantMessage` / public response shape.

4. **No attempt TTL.** `ConfirmedSearchExecutionAttempt` не имеет
   `expiresAt` или TTL policy. `IN_PROGRESS` attempt может оставаться
   indefinitely в store.

5. **No retry policy.** `FAILED` и stale `IN_PROGRESS` attempts не имеют
   recovery path. Store не поддерживает retry transitions.

### 4.2 `IN_PROGRESS` and stale attempt policy

`IN_PROGRESS` зафиксирован как internal fake/no-op transition marker в
Stage 8.40. Это безопасно как skeleton concept, но создаёт risks для
runtime:

| Risk | Описание |
|---|---|
| Stale `IN_PROGRESS` | Attempt остаётся `IN_PROGRESS` indefinitely. Повторный confirmation для тех же criteria блокируется как duplicate. |
| Process restart | In-memory store loses all attempts. После restart тот же confirmation может создать duplicate attempt (если pending ещё active). |
| No transition to terminal | Skeleton вызывает `markInProgress`, но не `markSucceeded` или `markFailed`. Attempt никогда не reaches terminal state без future execution stage. |

Перед wiring необходимо определить:

- attempt TTL: сколько `IN_PROGRESS` attempt может оставаться active;
- stale attempt recovery: что делать, если `IN_PROGRESS` attempt exceeds TTL;
- process restart behavior: как handle lost in-memory state.

### 4.3 Retry policy gaps

Current store не поддерживает retry transitions:

| From state | Retry possible? | Current behavior |
|---|---|---|
| `PREPARED` | No retry needed | Normal first attempt. |
| `IN_PROGRESS` | No | `markInProgress` returns `Duplicate`. `markSucceeded`/`markFailed` accepted only from `IN_PROGRESS`. |
| `SUCCEEDED` | No | Duplicate returns existing. |
| `FAILED` | No | `markInProgress` returns `Rejected(ATTEMPT_NOT_IN_PROGRESS)`. No `FAILED` -> `PREPARED` transition. |
| `DUPLICATE_BLOCKED` | No | Terminal snapshot state; no transitions accepted. |

Retry policy needed для:

- `FAILED` -> allow new attempt для тех же criteria?
- Stale `IN_PROGRESS` -> treat as `FAILED` и allow retry?
- Expired pending + existing attempt -> allow new pending и new attempt?

### 4.4 Pending confirmation consume ordering

Текущий consume ordering:

```
PostConfirmationDecision.Confirmed -> immediate markConsumed -> text-only response
```

Future consume ordering должен быть:

```
PostConfirmationDecision.Confirmed
  -> ExecuteConfirmedSearchTransitionUseCase
  -> Transitioned / DuplicateDetected / GuardRejected / StoreRejected
  -> consume decision based on result
  -> response mapping
```

Ключевой вопрос: когда безопасно consume pending confirmation?

| Result variant | Safe to consume? | Reason |
|---|---|---|
| `Transitioned` (fake/no-op) | **Нет** для skeleton; **да** для future actual execution после `SUCCEEDED` recording. | Skeleton не создаёт actual search. Consuming pending до real search creation лишает user retry через тот же pending. |
| `DuplicateDetected(SUCCEEDED)` | Да, если actual search уже создан и search id can be replayed. | Pending уже served; existing search id известен. |
| `DuplicateDetected(IN_PROGRESS)` | Нет. | Execution еще не завершился; premature consume. |
| `DuplicateDetected(FAILED)` | Нет. | Execution failed; pending может быть нужен для retry. |
| `GuardRejected` | Нет. | Guard failed; pending остаётся active для future retry или correction. |
| `StoreRejected` | Нет. | Store failed; pending remains active. |

Для skeleton stage `Transitioned` с fake/no-op execution **не должен**
trigger `markConsumed`. Pending consumption должен быть разрешён только
после actual `SUCCEEDED` attempt recording с real `HotelSearchId`.

### 4.5 Future response mapping

Future response mapping из `ExecuteConfirmedSearchTransitionResult` в
public response shape:

| Result variant | Current safe response | Future with actual execution |
|---|---|---|
| `Transitioned` (skeleton) | `ask_clarification` + acknowledgement text; no `hotelSearchId`; no `show_hotel_results`. | — |
| `Transitioned` (future actual) | — | `show_hotel_results` + `hotelSearchId` + success message. |
| `DuplicateDetected(SUCCEEDED)` | — | Replay `show_hotel_results` + existing `hotelSearchId`. |
| `DuplicateDetected(IN_PROGRESS)` | `ask_clarification` + in-progress text. | Same. |
| `DuplicateDetected(FAILED)` | `ask_clarification` + failure boundary text. | Same or retry prompt. |
| `DuplicateDetected(PREPARED)` | `ask_clarification` + acknowledgement. | Same. |
| `DuplicateDetected(DUPLICATE_BLOCKED)` | `ask_clarification` + boundary text. | Same. |
| `GuardRejected` | `ask_clarification` + guard failure text; no `hotelSearchId`. | Same. |
| `StoreRejected` | `ask_clarification` + store failure text; no `hotelSearchId`. | Same. |

Result variants, которые **никогда** не должны производить
`show_hotel_results`:

- `GuardRejected`;
- `StoreRejected`;
- `DuplicateDetected` с любым status кроме `SUCCEEDED`;
- `Transitioned` в skeleton phase (без actual search creation).

Result variants, которые могут производить user-facing assistant message
без hotel results:

- все current skeleton variants через `ask_clarification` + safe text.

### 4.6 Runtime/API safety

Текущий runtime безопасен:

- `ExecuteConfirmedSearchTransitionUseCase` не подключён к routes;
- `Application.kt` не изменён;
- `AssistantLlmRouteWiringUseCase` не изменён;
- `CreateHotelSearchUseCase` не вызывается из confirmation flow;
- `hotelSearchId` не создаётся;
- `show_hotel_results` не возвращается;
- `markConsumed` вызывается только для existing text-only confirmation
  acknowledgement.

Stage 7 strict `hotel-search;` handoff остаётся единственным current
automatic search creation path.

## 5. Blockers before wiring

Перед подключением `ExecuteConfirmedSearchTransitionUseCase` к runtime
необходимоresolve следующие blockers:

| # | Blocker | Тип |
|---|---|---|
| B1 | **Consume ordering policy.** Explicit rules: когда runtime может вызвать `markConsumed` после orchestration. Для skeleton: never. Для future actual execution: только после `SUCCEEDED` attempt recording. | Design/policy. |
| B2 | **Integration point design.** Explicit design: как `ExecuteConfirmedSearchTransitionUseCase` заменяет или дополняет current `Confirmed` branch в `withPostConfirmationDecision`. | Design. |
| B3 | **Response mapping rules.** Typed mapping из каждого `ExecuteConfirmedSearchTransitionResult` variant в `AcceptedAssistantMessage` / public response shape. | Design. |
| B4 | **Attempt TTL policy.** TTL или expiry для `IN_PROGRESS` attempts. Stale attempt recovery rules. | Design/policy. |
| B5 | **Retry policy.** Rules для `FAILED` retry, stale `IN_PROGRESS` recovery, `DUPLICATE_BLOCKED` handling. | Design/policy. |
| B6 | **Stage 7 handoff compatibility proof.** Future tests должны доказать, что Stage 7 strict `hotel-search;` handoff remains unchanged и priority-safe после wiring. | Testing. |

## 6. Recommendation

Runtime wiring is not safe yet. `ExecuteConfirmedSearchTransitionUseCase`
skeleton is useful как orchestration boundary, но остаётся blocked от route
use до explicit policy和设计 steps.

Рекомендуемый следующий step — review/design-only stage, который фиксирует
explicit policies для blockers B1-B5 и определяет integration point design
(B2), без production code changes.

После policy stage может следовать narrow implementation stage для attempt
TTL, retry transitions и response mapping skeletons.

Runtime wiring должен быть отдельным bounded stage после того, как все
blockers resolved и compatibility tests доказаны.

## 7. Explicit non-goals

Stage 8.41 не создаёт и не меняет:

- Production code.
- Tests.
- Route wiring или runtime composition.
- `Application.kt`.
- `AssistantLlmRouteWiringUseCase`.
- Assistant routes.
- `CreateHotelSearchUseCase` call.
- Actual `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` wiring в execution path.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.
- Attempt TTL implementation.
- Retry policy implementation.
- Response mapping implementation.

## 8. Suggested next stage

Safe Stage 8.42: review/design-only attempt lifecycle and response mapping
policy.

Минимальная цель:

- зафиксировать attempt TTL policy для `IN_PROGRESS` и stale attempts;
- зафиксировать retry policy для `FAILED` и stale `IN_PROGRESS`;
- зафиксировать consume ordering rules для каждого
  `ExecuteConfirmedSearchTransitionResult` variant;
- зафиксировать future response mapping из result variants в public response
  shape;
- определить integration point для `ExecuteConfirmedSearchTransitionUseCase`
  в `AssistantLlmRouteWiringUseCase`;
- не менять production code;
- не подключать runtime wiring.

Out of scope для Stage 8.42:

- Production code changes.
- Tests.
- Route wiring.
- Actual execution.
- `CreateHotelSearchUseCase` call.
- `hotelSearchId` / `show_hotel_results`.
- `markConsumed` runtime wiring.

## 9. Validation

Review-only inspection:

- `grep "ExecuteConfirmedSearchTransition" services/backend` — references найдены только в 3 skeleton files и 1 test file. Не referenced из `Application.kt`, `AssistantLlmRouteWiringUseCase`, assistant routes.
- `grep "markConsumed" services/backend` — `markConsumed` вызывается только в `AssistantLlmRouteWiringUseCase.consumePendingConfirmation` для existing text-only confirmation flow. Не referenced из `ExecuteConfirmedSearchTransitionUseCase`.
- `grep "CreateHotelSearchUseCase" services/backend` — referenced только в `Application.kt` для Stage 7 strict handoff composition. Не referenced из orchestration skeleton.
- `git status --short` — working tree clean.

## 10. Verdict

**Passed with blockers documented** — skeleton is useful but wiring remains
blocked.

`ExecuteConfirmedSearchTransitionUseCase` skeleton из Stage 8.40
предоставляет корректный orchestration boundary с typed result и ordering
model. Runtime wiring не безопасен до explicit policy/design steps вокруг:
consume ordering, attempt TTL, retry policy, response mapping и integration
point design. Stage 7 strict hotel-search handoff остаётся единственным
текущим automatic search creation path.
