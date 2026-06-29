# Stage 8.39 — Attempt Store Integration Readiness Gate

## 1. Scope

Stage 8.39 — review/design-only gate. Проверить, достаточно ли текущего
`InMemoryConfirmedSearchExecutionAttemptStore` skeleton из Stage 8.38 для
будущего fake/no-op confirmed-search execution boundary, или нужен отдельный
application-level transition orchestration use case перед runtime wiring.

Stage 8.39 не меняет production code, tests, runtime behavior, routes, public
API, OpenAPI, frontend, generated clients или roadmap/root status files.

## 2. Current inspected state

После Stage 8.38 internal confirmed-search chain проходит через следующие
skeleton layers:

| Шаг | Компонент | Результат |
|---|---|---|
| 1 | `PlanPostConfirmationDecisionUseCase` | `PostConfirmationDecision.Confirmed(criteria)` |
| 2 | `PlanConfirmedSearchCreationUseCase` | `ConfirmedSearchCreationPlan.ReadyToCreateSearch` |
| 3 | `BuildConfirmedSearchCreationCommandUseCase` | `ConfirmedSearchCreationCommandPlan.CommandReady` |
| 4 | `PlanConfirmedSearchExecutionUseCase` | `ConfirmedSearchExecutionResult.PreparedButNotExecuted` |
| 5 | `PlanConfirmedSearchExecutionGuardUseCase` | `AllowedButBlockedUntilIdempotencyGuard` или `Rejected` |
| 6 | `PlanConfirmedSearchExecutionAttemptUseCase` | `AttemptPreparedButExecutionBlocked` или `DuplicateDetected` |
| 7 | `InMemoryConfirmedSearchExecutionAttemptStore` | process-local storage с typed transitions |

Текущий runtime (`AssistantLlmRouteWiringUseCase`) обрабатывает
`PostConfirmationDecision.Confirmed` через немедленный `markConsumed` и
возвращает text-only acknowledgement без search creation, без `hotelSearchId`,
без `show_hotel_results` и без вызова любого из шагов 2-7.

Шаги 2-7 существуют как isolated internal skeletons. Они не подключены к routes,
не вызываются из `AssistantLlmRouteWiringUseCase` и не имеют единой
orchestration точки.

## 3. Key question

Достаточен ли текущий `InMemoryConfirmedSearchExecutionAttemptStore` как
future dependency для no-op/fake execution boundary, или перед runtime wiring
нужен отдельный transition orchestration use case, который связывает:

- pending-state / idempotency guard result;
- attempt planning;
- attempt store;
- execution result skeleton;
- pending confirmation consume ordering.

## 4. Findings

### 4.1 Store role

`InMemoryConfirmedSearchExecutionAttemptStore` — это storage primitive с
typed transitions. Он предоставляет:

| Операция | Поведение |
|---|---|
| `savePrepared(attempt)` | Сохраняет first `PREPARED` attempt или возвращает existing duplicate. |
| `findByIdempotencyKey(key)` | Возвращает stored attempt snapshot или `null`. |
| `markInProgress(key, now)` | Переводит `PREPARED` -> `IN_PROGRESS`; duplicate otherwise. |
| `markSucceeded(key, searchId, now)` | Переводит `IN_PROGRESS` -> `SUCCEEDED`; rejected otherwise. |
| `markFailed(key, reason, now)` | Переводит `IN_PROGRESS` -> `FAILED`; rejected otherwise. |

Store не владеет:

- guard result evaluation;
- attempt creation/planning из guard result;
- idempotency key derivation;
- existing attempt lookup decision;
- execution call или fake/no-op result;
- pending confirmation consume ordering;
- response decision/action creation;
- failure mapping для public response;
- duplicate-after-success search id replay.

**Вывод**: store достаточен как storage primitive для future orchestration.
Он недостаточен как direct execution dependency из route handler, потому что
между store и route handler отсутствует слой, который владеет ordering и
composition.

### 4.2 Missing orchestration boundary

Ни один существующий компонент не связывает шаги 2-7 в единый atomic
transition flow:

- `PlanConfirmedSearchExecutionGuardUseCase` — read-only precondition check;
  не знает о store или attempts.
- `PlanConfirmedSearchExecutionAttemptUseCase` — моделирует attempt plan из
  guard result и optional existing attempt snapshot; не владеет store lookup,
  persistence или execution.
- `InMemoryConfirmedSearchExecutionAttemptStore` — хранит attempts; не знает
  о guard, pending state, execution или pending consumption.
- `PlanConfirmedSearchExecutionUseCase` — возвращает `PreparedButNotExecuted`;
  не владеет store transitions.
- `AssistantLlmRouteWiringUseCase` — текущий runtime entry point; обрабатывает
  `Confirmed` через immediate `markConsumed` без использования шагов 2-7.

Отсутствует application-level use case, который:

1. принимает `PostConfirmationDecision.Confirmed` или эквивалентный input;
2. прогоняет через confirmed-search creation/execution chain;
3. выполняет guard check;
4. ищет existing attempt через store;
5. планирует и персистирует new attempt;
6. выполняет fake/no-op execution transition;
7. управляет `markConsumed` ordering;
8. возвращает typed transition result для future route mapping.

Без такого orchestration layer будущий runtime wiring должен будет держать
всю ordering логику внутри route handler, что создаёт coupling и риски.

### 4.3 Ordering risks

Текущий `AssistantLlmRouteWiringUseCase` вызывает `markConsumed` немедленно
для `PostConfirmationDecision.Confirmed`. Это безопасно для текущего
text-only acknowledgement, но создаёт ordering conflict для future execution:

| Ordering | Риск |
|---|---|
| `markConsumed` before attempt creation | Если attempt creation fails, pending уже consumed и retry невозможен через тот же pending state. |
| `markConsumed` before execution | Если execution fails, pending consumed, search не создан, user теряет confirmation context. |
| `markConsumed` after execution success | Правильный ordering, но требует, чтобы success был записан в attempt store до consume. |
| `markConsumed` after duplicate detection | Duplicate после success: pending уже consumed, но existing search id может быть replayed из store. |
| Нет `markConsumed` для failed execution | Stale pending может trigger повторный confirmation prompt или unexpected behavior. |

Минимальный mandatory ordering для future orchestration:

1. Guard result evaluation.
2. Existing attempt lookup через store.
3. Attempt planning (new или duplicate detection).
4. Attempt persistence (`savePrepared` для new attempt).
5. Fake/no-op execution result (без actual search creation).
6. Attempt state transition (`markInProgress` / `markSucceeded` / `markFailed` для future real execution).
7. Pending confirmation consume — только после successful attempt state recording.
8. Response decision/action creation из typed transition result.

### 4.4 Idempotency risks

Если подключить attempt store напрямую из route handler без orchestration
layer:

| Риск | Описание |
|---|---|
| Bypass attempt planning | Route handler может вызвать store напрямую, минуя `PlanConfirmedSearchExecutionAttemptUseCase` и его key/session/command mismatch validation. |
| Non-atomic guard-to-store | Guard result может устареть между evaluation и store operation, особенно если pending state меняется concurrently. |
| Missing `IN_PROGRESS` lock | Без `markInProgress` перед execution, parallel repeated confirmation может создать второй attempt для тех же criteria. |
| Lost response после success | Если attempt `SUCCEEDED`, но response lost, retry должен вернуть existing `createdSearchId`; без orchestration layer route handler не знает, как replay. |
| `markConsumed` без attempt recording | Если pending consumed, но attempt не записан в store, retry создаёт orphan state. |
| Duplicate после `FAILED` | Текущий store не поддерживает retry из `FAILED` state; route handler без orchestration не знает, разрешён ли retry. |

### 4.5 Runtime/API safety

Текущий runtime безопасен:

- `Confirmed` -> immediate `markConsumed` -> text-only acknowledgement;
- нет `hotelSearchId`;
- нет `show_hotel_results`;
- нет вызова `CreateHotelSearchUseCase`;
- нет provider calls.

Future runtime wiring должен доказать:

- attempt store не bypass guard;
- `markConsumed` не вызывается до successful attempt recording;
- duplicate confirmation не создаёт duplicate search;
- `SUCCEEDED` attempt replays same search reference;
- `FAILED` attempt не leaks internal failure details;
- Stage 7 strict `hotel-search;` handoff остаётся единственным current automatic search creation path.

## 5. Recommendation

Перед любым runtime wiring рекомендован отдельный application-level
transition orchestration use case, например future concept:

- `ExecuteConfirmedSearchTransitionUseCase`.

Этот use case должен:

- владеть ordering между guard, attempt planning, store persistence, execution
  result и pending confirmation consume;
- принимать typed input (pending confirmation snapshot, session id, command
  plan или equivalent);
- возвращать typed transition result для future route response mapping;
- не вызывать `CreateHotelSearchUseCase` на Stage 8 skeleton phase;
- не создавать actual `hotelSearchId` на Stage 8 skeleton phase;
- не возвращать `show_hotel_results` на Stage 8 skeleton phase;
- использовать fake/no-op execution result для skeleton validation.

Store не требует изменений для поддержки такого orchestration use case.
Текущий interface и transitions достаточны.

## 6. Proposed future boundary

`ExecuteConfirmedSearchTransitionUseCase` (future concept, не создаётся в
Stage 8.39):

Владеет:

| Responsibility | Описание |
|---|---|
| Guard evaluation | Вызывает `PlanConfirmedSearchExecutionGuardUseCase` с pending snapshot. |
| Existing attempt lookup | Вызывает `store.findByIdempotencyKey` для duplicate detection. |
| Attempt planning | Вызывает `PlanConfirmedSearchExecutionAttemptUseCase` с guard result и existing attempt. |
| Attempt persistence | Вызывает `store.savePrepared` для new `PREPARED` attempt. |
| Execution transition | Для future real execution: `markInProgress` -> execution -> `markSucceeded`/`markFailed`. Для skeleton: fake/no-op result. |
| Pending consume ordering | Вызывает `PendingConfirmationStore.markConsumed` только после successful attempt state recording. |
| Transition result | Возвращает typed internal result для future route response mapping. |

Не владеет:

| Out of scope | Почему |
|---|---|
| Route handler logic | Route handler должен быть thin delegate. |
| HTTP response shape | Response mapping — separate route-layer concern. |
| `CreateHotelSearchUseCase` call | Execution boundary — separate future step после skeleton validation. |
| Provider calls | Provider integration — separate future layer. |
| LLM orchestration | LLM pipeline — separate existing layer. |
| Public API contract changes | OpenAPI/frontend — separate future step. |
| Durable storage | Persistence — separate future concern. |

## 7. Explicit non-goals

Stage 8.39 не создаёт и не меняет:

- Production code.
- Tests.
- `ExecuteConfirmedSearchTransitionUseCase` или любой другой orchestration use case.
- Route wiring или runtime composition.
- `Application.kt`.
- `AssistantLlmRouteWiringUseCase`.
- `CreateHotelSearchUseCase` call из confirmation flow.
- Actual `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` wiring в execution path.
- Provider, network, API keys, auth, durable storage, booking flow.
- OpenAPI contracts, frontend, generated clients.
- Roadmap/root status files.
- Переименование существующих Stage 8 classes.
- Runtime semantics текущего confirmation flow.

## 8. Suggested next stage

Safe Stage 8.40: backend-only internal `ExecuteConfirmedSearchTransitionUseCase`
skeleton, no route wiring and no actual search execution.

Минимальная цель:

- добавить internal orchestration use case, который связывает guard, attempt
  planning, store persistence, fake/no-op execution result и pending consume
  ordering;
- использовать `PreparedButNotExecuted` или equivalent no-op execution result;
- не вызывать `CreateHotelSearchUseCase`;
- не создавать actual `hotelSearchId`;
- не возвращать `show_hotel_results`;
- не подключать к routes;
- не менять `AssistantLlmRouteWiringUseCase`;
- покрыть ordering и idempotency behavior targeted tests.

Out of scope для Stage 8.40:

- Route wiring.
- Actual `CreateHotelSearchUseCase` call.
- `hotelSearchId` / `show_hotel_results` creation.
- `Application.kt` changes.
- Public API/OpenAPI/frontend changes.
- Durable storage.
- Provider calls.
- `markConsumed` wiring в runtime execution path.

## 9. Validation

Review-only inspection:

- `rg -n "ConfirmedSearchExecutionAttempt|ConfirmedSearchExecutionAttemptStore|InMemoryConfirmedSearchExecutionAttemptStore"` — найдены skeleton files и tests, не подключены к runtime.
- `rg -n "PostConfirmationDecision|markConsumed|show_hotel_results|hotelSearchId|CreateHotelSearchUseCase"` — `markConsumed` вызывается только для text-only acknowledgement; `show_hotel_results`/`hotelSearchId` существуют только в Stage 7 strict handoff path; `CreateHotelSearchUseCase` не вызывается из confirmation flow.
- `AssistantLlmRouteWiringUseCase` — `PostConfirmationDecision.Confirmed` обрабатывается через immediate `consumePendingConfirmation` и text-only response без вызова confirmed-search chain.
- `InMemoryConfirmedSearchExecutionAttemptStore` — process-local, `@Synchronized` для mutations, typed transitions, не подключен к routes.
- `PlanConfirmedSearchExecutionAttemptUseCase` — моделирует attempt plan, но не владеет store lookup или persistence.
- `PlanConfirmedSearchExecutionGuardUseCase` — deterministic read-only guard без store dependency.

## 10. Verdict

**Passed with notes** — orchestration recommended, but no store changes
required yet.

Текущий `InMemoryConfirmedSearchExecutionAttemptStore` достаточен как storage
primitive для future orchestration. Store interface и transitions не требуют
изменений. Перед любым runtime wiring необходим отдельный application-level
transition orchestration use case, который владеет ordering между guard,
attempt planning, store persistence, execution result и pending confirmation
consume. Без такого слоя direct route-to-store wiring создаёт idempotency,
ordering и coupling риски.
