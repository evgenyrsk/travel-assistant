# Stage 8.42 — Attempt Lifecycle and Response Mapping Policy

## 1. Scope

Stage 8.42 — review/design-only policy stage. Зафиксировать explicit policy
для future confirmed-search transition runtime wiring:

- attempt lifecycle policy;
- TTL/stale policy для `IN_PROGRESS`;
- retry policy для `FAILED`, stale `IN_PROGRESS`, `DUPLICATE_BLOCKED`;
- pending confirmation consume ordering;
- response mapping policy для каждого `ExecuteConfirmedSearchTransitionResult` variant;
- список implementation stages перед route wiring.

Stage 8.42 не меняет production code, tests, runtime behavior, routes, public
API, OpenAPI, frontend, generated clients или roadmap/root status files.

Stage 8.42 не закрывает B6 (Stage 7 handoff compatibility proof) — это
остаётся для отдельного stage.

## 2. Current inspected state

Attempt statuses (from `ConfirmedSearchExecutionAttemptStatus`):

| Status | Model field | Terminal? |
|---|---|---|
| `PREPARED` | `status` | Нет; ожидает execution. |
| `IN_PROGRESS` | `status` | Нет; ожидает completion. |
| `SUCCEEDED` | `status` + `createdSearchId` | Да. |
| `FAILED` | `status` + `failureReason` | Да. |
| `DUPLICATE_BLOCKED` | `status` | Да; snapshot state. |

Current transition rules (from `InMemoryConfirmedSearchExecutionAttemptStore`):

| Transition | Condition |
|---|---|
| no attempt → `PREPARED` | `savePrepared` with `PREPARED` attempt. |
| `PREPARED` → `IN_PROGRESS` | `markInProgress` from `PREPARED`. |
| `IN_PROGRESS` → `SUCCEEDED` | `markSucceeded` from `IN_PROGRESS` with `HotelSearchId`. |
| `IN_PROGRESS` → `FAILED` | `markFailed` from `IN_PROGRESS` with `FailureReason`. |

Failure reasons (from `ConfirmedSearchExecutionAttemptFailureReason`):

- `SEARCH_CREATION_FAILED`;
- `EXECUTION_STATE_UNKNOWN`.

Attempt model timestamps: `createdAt`, `updatedAt`. Нет `expiresAt`.

Pending confirmation: имеет `expiresAt`, `statusAt(now)` возвращает `EXPIRED`
если past TTL. Текущий runtime TTL: 15 минут.

Current runtime consume behavior: immediate `markConsumed` для
`PostConfirmationDecision.Confirmed`, `Declined`, `NeedsReplanning`.

Orchestration skeleton (`ExecuteConfirmedSearchTransitionUseCase`):
возвращает `PendingConsumptionDecision` как metadata. Не вызывает `markConsumed`.

Result variants (from `ExecuteConfirmedSearchTransitionResult`):

- `Transitioned(attempt, executionResult, pendingConsumptionDecision)`;
- `DuplicateDetected(existingAttempt, duplicateReason, pendingConsumptionDecision)`;
- `GuardRejected(attemptRejectionReason)`;
- `StoreRejected(reason)`.

## 3. Key policy questions

1. Какие attempt statuses terminal, а какие transient?
2. Должен ли `IN_PROGRESS` иметь TTL?
3. Когда допустим retry из `FAILED` и stale `IN_PROGRESS`?
4. Когда future runtime может вызвать `markConsumed`?
5. Как каждый result variant должен map'иться в public response?
6. Какие conditions необходимы перед route wiring?

## 4. Attempt lifecycle policy

### 4.1 Current statuses

Текущие пять статусов покрывают базовый lifecycle:

```
no attempt → PREPARED → IN_PROGRESS → SUCCEEDED
                                    → FAILED
          → DUPLICATE_BLOCKED (snapshot only)
```

Все пять статусов достаточны для policy definition. Новые статусы не нужны
для policy stage.

### 4.2 Terminal vs transient states

| Status | Classification | Reason |
|---|---|---|
| `PREPARED` | Transient | Attempt создан, но execution не начался. Не должен оставаться indefinitely. |
| `IN_PROGRESS` | Transient | Execution начался, но не завершился. Не должен оставаться indefinitely. |
| `SUCCEEDED` | Terminal | Search создан, `createdSearchId` записан. Финальный outcome. |
| `FAILED` | Terminal | Execution failed. Финальный outcome без retry policy. |
| `DUPLICATE_BLOCKED` | Terminal snapshot | Не attempt record, а snapshot для duplicate response. |

Transient states требуют TTL policy. Terminal states не требуют TTL, но
могут иметь data retention policy (out of scope для Stage 8).

### 4.3 Duplicate blocking rules

| Status | Blocks new attempt for same idempotency key? | Mechanism |
|---|---|---|
| `PREPARED` | Да | `savePrepared` returns `Duplicate`. |
| `IN_PROGRESS` | Да | `savePrepared` returns `Duplicate`. `markInProgress` returns `Duplicate`. |
| `SUCCEEDED` | Да | `savePrepared` returns `Duplicate`. Existing `createdSearchId` can be replayed. |
| `FAILED` | Да | `savePrepared` returns `Duplicate`. Retry blocked без explicit retry policy. |
| `DUPLICATE_BLOCKED` | Да (indirectly) | Snapshot state; store retains original attempt record. |

Policy decision: все statuses block duplicate для того же idempotency key.
Retry из `FAILED` и stale `IN_PROGRESS` требует explicit mechanism (см. 4.5).

### 4.4 TTL and stale `IN_PROGRESS`

**Policy decision**: `IN_PROGRESS` без TTL не должен быть runtime-facing.

Current gap: `ConfirmedSearchExecutionAttempt` не имеет `expiresAt`. Store
не имеет stale detection.

Future policy (design-only; не создавать code в Stage 8.42):

| Parameter | Policy |
|---|---|
| `IN_PROGRESS` TTL | Должен быть определён. Рекомендация: aligned с pending confirmation TTL (15 минут) или короче. |
| Stale `IN_PROGRESS` behavior | Treat as `FAILED` с new failure reason `STALE_EXECUTION`. |
| `PREPARED` TTL | Должен быть определён. Может быть aligned с pending confirmation TTL. |
| Stale `PREPARED` behavior | Treat as expired; allow new attempt для тех же criteria после pending re-confirmation. |
| Implementation approach | Добавить optional `expiresAt` field в `ConfirmedSearchExecutionAttempt`. Store `findByIdempotencyKey` должен проверять stale и return annotated result. |

**Новый status не нужен.** Stale `IN_PROGRESS` классифицируется как
`FAILED(STALE_EXECUTION)` через existing status + new failure reason.
Это сохраняет existing model simplicity.

**Process restart behavior**: in-memory store loses all attempts. После
restart pending confirmation может быть ещё active (если не expired).
Future confirmation attempt creates new attempt для того же idempotency
key. Это acceptable для process-local scope. Durable storage — separate
future concern.

### 4.5 Retry policy

**Policy decision**: retry разрешён только из `FAILED` и stale `IN_PROGRESS`.

| From state | Retry allowed? | Mechanism |
|---|---|---|
| `PREPARED` | Нет (retry не нужен) | Normal first attempt. |
| `IN_PROGRESS` (active) | Нет | Execution в процессе; duplicate blocked. |
| `IN_PROGRESS` (stale) | Да, через stale → `FAILED(STALE_EXECUTION)` → retry. | Stale detection переводит в `FAILED`, затем retry mechanism создаёт новый attempt. |
| `SUCCEEDED` | Нет | Search уже создан. Duplicate возвращает existing `createdSearchId`. |
| `FAILED` | Да | Retry создаёт новый attempt. |
| `DUPLICATE_BLOCKED` | Нет | Snapshot state; original attempt record governs behavior. |

Retry mechanism (design-only):

| Decision | Policy |
|---|---|
| Retry создаёт новый attempt или reuses key? | **Новый attempt** с тем же idempotency key. Store должен поддержать `FAILED` → allow new `savePrepared` для того же key. |
| Как избежать повторного actual search creation? | Retry attempt проходит через тот же orchestration flow. Store duplicate detection предотвращает parallel execution. |
| Различать failure reasons для retry? | Да. `SEARCH_CREATION_FAILED` — retry allowed. `EXECUTION_STATE_UNKNOWN` — retry blocked (может означать, что search был создан, но state неизвестен). `STALE_EXECUTION` — retry allowed. |
| Retry limit | Минимум 1 retry для `SEARCH_CREATION_FAILED` и `STALE_EXECUTION`. Retry limit policy — separate future concern. |

Store interface changes (design-only; future stage):

- `savePrepared` должен accept new `PREPARED` attempt для existing `FAILED` key (сейчас returns `Duplicate`).
- Добавить `findBySessionAndCriteria` для lookup без exact idempotency key (future concern).

## 5. Pending confirmation consume policy

**Policy decision**: `markConsumed` не должен вызываться после fake/no-op
transition. Consume допустим только после durable success condition.

| Result variant | Consume allowed? | Condition |
|---|---|---|
| `Transitioned` (fake/no-op, skeleton) | **Нет** | Skeleton не создаёт actual search. Pending остаётся active. |
| `Transitioned` (future actual execution, `SUCCEEDED`) | **Да** | Только после `SUCCEEDED` attempt recording с real `HotelSearchId`. |
| `DuplicateDetected` — `SUCCEEDED` | **Да** (если pending ещё не consumed) | Existing search id can be replayed. Pending served. |
| `DuplicateDetected` — `IN_PROGRESS` | **Нет** | Execution не завершился. |
| `DuplicateDetected` — `FAILED` | **Нет** | Execution failed. Pending может быть нужен для retry. |
| `DuplicateDetected` — `PREPARED` | **Нет** | Execution не начался. |
| `DuplicateDetected` — `DUPLICATE_BLOCKED` | **Нет** | Original attempt governs. |
| `GuardRejected` | **Нет** | Guard failed. Pending остаётся active для correction или retry. |
| `StoreRejected` | **Нет** | Store failed. Pending остаётся active. |

Durable success condition (для future actual execution):

1. `CreateHotelSearchUseCase` returned `HotelSearchId`;
2. Attempt store recorded `SUCCEEDED` с `createdSearchId`;
3. Pending confirmation can be safely consumed после step 2.

Consume ordering для future wiring:

```
1. Guard evaluation (read-only).
2. Existing attempt lookup (read-only).
3. Attempt planning.
4. Attempt persistence (savePrepared).
5. Execution: CreateHotelSearchUseCase call.
6. markSucceeded с HotelSearchId (или markFailed).
7. markConsumed — только если step 6 returned SUCCEEDED.
8. Response mapping.
```

Integration point: `AssistantLlmRouteWiringUseCase.withPostConfirmationDecision`
для `PostConfirmationDecision.Confirmed` должен:

1. Не вызывать `consumePendingConfirmation` немедленно.
2. Вызвать `ExecuteConfirmedSearchTransitionUseCase`.
3. Проверить result variant и `PendingConsumptionDecision`.
4. Вызвать `consumePendingConfirmation` только если result is `Transitioned`
   с actual `SUCCEEDED` attempt recording.
5. Map result в response.

## 6. Future response mapping policy

### 6.1 `Transitioned`

| Phase | Response policy |
|---|---|
| Skeleton (fake/no-op) | `ask_clarification` + acknowledgement text (e.g. "Confirmation received. I will not start a hotel search automatically yet."). No `hotelSearchId`. No `show_hotel_results`. Pending remains active. |
| Future actual execution (`SUCCEEDED`) | `show_hotel_results` + `hotelSearchId` + success assistant message. Pending consumed. |

`Transitioned` в skeleton phase **не должен** производить `show_hotel_results`
или `hotelSearchId`.

### 6.2 `DuplicateDetected`

| Duplicate reason | Response policy |
|---|---|
| `PREPARED` | `ask_clarification` + "confirmation already received" text. No `hotelSearchId`. No `show_hotel_results`. Pending not consumed. |
| `IN_PROGRESS` | `ask_clarification` + "search is being processed" text. No `hotelSearchId`. No `show_hotel_results`. Pending not consumed. |
| `SUCCEEDED` | `show_hotel_results` + existing `hotelSearchId` + replay message. Pending consumed (if not already). |
| `FAILED` | `ask_clarification` + failure boundary text. No `hotelSearchId`. No `show_hotel_results`. Pending not consumed. Retry may be offered. |
| `DUPLICATE_BLOCKED` | `ask_clarification` + boundary text. No `hotelSearchId`. No `show_hotel_results`. Pending not consumed. |

`DuplicateDetected` с `SUCCEEDED` — единственный current-safe путь вернуть
`show_hotel_results` и `hotelSearchId`. Все остальные duplicate reasons
**не должны** производить hotel results.

### 6.3 `GuardRejected`

| Policy | Detail |
|---|---|
| User-facing message | `ask_clarification` + guard-specific text (expired, consumed, mismatch, no active pending). |
| `hotelSearchId` | Нет. |
| `show_hotel_results` | Нет. |
| Pending consumed | Нет. Pending остаётся active (или already consumed/expired per guard reason). |
| User retry | Пользователь может отправить новый confirmation reply или новые criteria. |

### 6.4 `StoreRejected`

| Policy | Detail |
|---|---|
| User-facing message | `ask_clarification` + safe boundary text ("Something went wrong. Please try again."). |
| `hotelSearchId` | Нет. |
| `show_hotel_results` | Нет. |
| Pending consumed | Нет. Pending остаётся active. |
| User retry | Пользователь может повторить confirmation. |

### Summary: response mapping invariants

Result variants, которые **никогда** не должны производить `show_hotel_results`:

- `GuardRejected`;
- `StoreRejected`;
- `Transitioned` в skeleton phase;
- `DuplicateDetected` с `PREPARED`, `IN_PROGRESS`, `FAILED`, `DUPLICATE_BLOCKED`.

Result variants, которые **могут** производить `show_hotel_results` (только
после actual execution stage):

- `Transitioned` с actual `SUCCEEDED` recording;
- `DuplicateDetected` с `SUCCEEDED` existing attempt.

Result variants, которые **никогда** не должны consume pending:

- `GuardRejected`;
- `StoreRejected`;
- `Transitioned` в skeleton phase;
- `DuplicateDetected` с любым status кроме `SUCCEEDED`.

## 7. Wiring readiness conditions

Перед route wiring должны быть выполнены:

| # | Condition | Type | Blocking? |
|---|---|---|---|
| C1 | Attempt TTL: `expiresAt` field, stale detection в store. | Implementation. | Да. |
| C2 | Retry policy: `FAILED` → allow new `savePrepared`; `STALE_EXECUTION` failure reason. | Implementation. | Да. |
| C3 | Response mapping: typed mapper из result variants в response directives. | Implementation. | Да. |
| C4 | Integration point: `withPostConfirmationDecision` modified для deferred consume. | Implementation. | Да. |
| C5 | Route tests: доказано, что skeleton `Transitioned` не производит `hotelSearchId` или `show_hotel_results`. | Tests. | Да. |
| C6 | Stage 7 compatibility tests: доказано, что strict `hotel-search;` handoff unchanged. | Tests (B6). | Да; separate stage. |
| C7 | Actual execution design: `CreateHotelSearchUseCase` integration в orchestration. | Design. | Да; separate stage. |

Policies, которые можно реализовать skeleton-first (без runtime wiring):

- C1: attempt TTL model + store stale detection;
- C2: retry transitions в store;
- C3: typed response mapping skeleton;
- C5: skeleton route tests.

Policies, которые требуют actual execution stage:

- C4: integration с deferred consume;
- C7: `CreateHotelSearchUseCase` call;
- C6: Stage 7 compatibility proof.

## 8. Explicit non-goals

Stage 8.42 не создаёт и не меняет:

- Production code.
- Tests.
- Route wiring или runtime composition.
- `Application.kt`, `AssistantLlmRouteWiringUseCase`, assistant routes.
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
- Stage 7 handoff compatibility proof (B6).
- Новые attempt statuses.

## 9. Suggested next stages

Рекомендуемая последовательность implementation stages перед route wiring:

### Stage 8.43 — attempt TTL and stale detection model

Добавить `expiresAt` в `ConfirmedSearchExecutionAttempt`. Добавить
`STALE_EXECUTION` в `ConfirmedSearchExecutionAttemptFailureReason`.
Обновить `InMemoryConfirmedSearchExecutionAttemptStore` для stale detection
в `findByIdempotencyKey`. Без route wiring.

### Stage 8.44 — retry transition support

Обновить `InMemoryConfirmedSearchExecutionAttemptStore.savePrepared` для
accept new `PREPARED` attempt для existing `FAILED` key. Добавить narrow
tests для retry scenarios. Без route wiring.

### Stage 8.45 — response mapping skeleton

Добавить internal typed response mapper из
`ExecuteConfirmedSearchTransitionResult` variants в response directives
(nextAction, hotelSearchId presence, assistant message direction).
Без route wiring.

### Stage 8.46 — integration readiness gate (review/design-only)

Проверить, готовы ли Stage 8.43-8.45 skeletons к route wiring. Определить
integration point design для `withPostConfirmationDecision`. Зафиксировать
consume ordering rules как pre-wiring checklist.

### Stage 8.47+ — route wiring и actual execution

Route wiring, actual `CreateHotelSearchUseCase` call, `markConsumed` после
`SUCCEEDED`, Stage 7 compatibility proof (B6). Каждый как отдельный bounded
stage.

Out of scope для Stage 8.43-8.45:

- Route wiring.
- Actual `CreateHotelSearchUseCase` call.
- `hotelSearchId` / `show_hotel_results`.
- `markConsumed` runtime wiring.
- Stage 7 compatibility proof (B6).

## 10. Validation

Review-only inspection:

- `ConfirmedSearchExecutionAttemptStatus` — 5 statuses; все covered в policy.
- `ConfirmedSearchExecutionAttemptFailureReason` — 2 reasons; `STALE_EXECUTION` предложен как new future reason.
- `InMemoryConfirmedSearchExecutionAttemptStore` — transition rules consistent с policy decisions.
- `ExecuteConfirmedSearchTransitionResult` — 4 variants; все covered в response mapping.
- `PendingConsumptionDecision` — 2 values; consistent с consume ordering policy.
- `PendingProceedWithCandidateConfirmation` — has `expiresAt`; pending TTL уже существует (15 min).
- Stage 8.41 blockers B1-B5 — addressed в policy. B6 deferred.
- `git status --short` — working tree clean.

## 11. Verdict

**Passed** — policy documented, runtime wiring remains blocked.

Stage 8.42 зафиксировал explicit policy для attempt lifecycle, TTL/stale
handling, retry, consume ordering и response mapping. Policy decisions
согласованы с existing skeleton architecture и не требуют новых статусов.
Runtime wiring остаётся blocked до implementation stages (C1-C5) и
отдельного Stage 7 compatibility proof (B6).
