# Stage 8.37 — Attempt store readiness gate

## Цель Stage 8.37

Оценить, нужен ли process-local `ConfirmedSearchExecutionAttemptStore`
skeleton перед любым actual confirmed-search execution, и какие state
transitions нужно зафиксировать для future confirmed-search execution flow.

Stage 8.37 — review/design-only gate. Он не меняет production code, tests,
runtime behavior, routes, public API, OpenAPI, frontend, generated clients или
roadmap/root status files.

## Текущая точка входа

После Stage 8.36 есть:

- `ConfirmedSearchExecutionAttempt`;
- `ConfirmedSearchExecutionAttemptStatus`;
- `ConfirmedSearchExecutionAttemptResult`;
- `ConfirmedSearchExecutionIdempotencyKey`;
- `PlanConfirmedSearchExecutionAttemptUseCase`;
- `ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard`;
- process-local `PendingConfirmationStore`;
- existing Stage 7 strict `hotel-search;` handoff.

Attempt use case может подготовить `PREPARED` attempt или распознать duplicate
на основе переданного existing attempt snapshot. Он не владеет store lookup,
state transitions, execution locking, created search mapping или replay after
success.

## Что уже есть после Stage 8.36

Internal confirmed-search chain может дойти до attempt boundary:

1. `PostConfirmationDecision.Confirmed(criteria)`.
2. `PlanConfirmedSearchCreationUseCase`.
3. `ConfirmedSearchCreationPlan.ReadyToCreateSearch`.
4. `BuildConfirmedSearchCreationCommandUseCase`.
5. `ConfirmedSearchCreationCommandPlan.CommandReady`.
6. `PlanConfirmedSearchExecutionUseCase`.
7. `ConfirmedSearchExecutionResult.PreparedButNotExecuted`.
8. `PlanConfirmedSearchExecutionGuardUseCase`.
9. `ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard`.
10. `PlanConfirmedSearchExecutionAttemptUseCase`.
11. `AttemptPreparedButExecutionBlocked` или `DuplicateDetected`.

Ни один слой не вызывает `CreateHotelSearchUseCase`, не создает actual
`hotelSearchId`, не возвращает `show_hotel_results`, не вызывает provider и не
вызывает `markConsumed`.

## Attempt store readiness assessment

Вердикт: process-local `ConfirmedSearchExecutionAttemptStore` skeleton нужен
перед любым actual execution.

Причины:

- Stage 8.36 моделирует attempts, но не хранит их между route calls;
- duplicate detection сейчас возможен только если caller передал existing
  attempt snapshot;
- future route path должен атомарно отличать first attempt от duplicate;
- `IN_PROGRESS` state нужен, чтобы repeated confirmation не создавала второй
  search;
- `SUCCEEDED` state нужен, чтобы duplicate after success мог вернуть тот же
  created search reference;
- `FAILED` state нужен, чтобы failure/retry policy не зависела от текста
  ответа;
- без store невозможно безопасно связать pending confirmation, attempt и
  created search reference.

Для Stage 8 scope store должен быть process-local/in-memory only. Durable
storage остается отдельной будущей темой и не требуется для skeleton.

Минимальные операции future store:

| Операция | Назначение |
|---|---|
| `findByIdempotencyKey` | Найти existing attempt для duplicate detection. |
| `createPrepared` | Создать first attempt из allowed guard result. |
| `markInProgress` | Заблокировать duplicate execution перед future search call. |
| `markSucceeded` | Зафиксировать created search reference после successful search creation. |
| `markFailed` | Зафиксировать failure без public/internal leakage. |
| `findActiveOrRecent` | Поддержать replay/duplicate handling до TTL expiry. |

Store не должен сам вызывать execution, provider, routes или pending
`markConsumed`.

## State transitions assessment

Вердикт: transitions нужно зафиксировать до execution route wiring.

Рекомендуемые transitions:

| From | To | Условие |
|---|---|---|
| no attempt | `PREPARED` | Guard allowed, idempotency key not found. |
| `PREPARED` | `IN_PROGRESS` | Future execution boundary готов начать search call. |
| `PREPARED` | `FAILED` | Command/guard/attempt preparation failed before execution. |
| `IN_PROGRESS` | `SUCCEEDED` | Search creation succeeded and created search reference recorded. |
| `IN_PROGRESS` | `FAILED` | Search creation failed before confirmed success. |
| `IN_PROGRESS` | duplicate response | Repeated confirmation while execution is running. |
| `SUCCEEDED` | duplicate same-search response | Repeated confirmation after successful creation. |
| `FAILED` | retry decision | Retry allowed only by explicit future policy. |
| any terminal or unresolved state | `DUPLICATE_BLOCKED` snapshot | Duplicate should not create a new search implicitly. |

`DUPLICATE_BLOCKED` should remain a result/snapshot state, not a replacement for
the original attempt record.

## Idempotency key assessment

Вердикт: current deterministic `ConfirmedSearchExecutionIdempotencyKey` is
достаточен для bounded Stage 8 process-local skeleton, но требует future
review before durable or multi-instance behavior.

Текущий fingerprint включает:

- internal scope prefix;
- `AssistantSessionId`;
- destination;
- check-in/check-out dates;
- adults/children;
- rooms.

Сильные стороны:

- deterministic for same session and criteria;
- internal only;
- raw session/destination не раскрываются в key value;
- не добавляет public confirmation id.

Риски и заметки:

- key не включает stable pending confirmation identity, потому что такой
  identity пока не существует;
- same session + same criteria can intentionally map to same key;
- if user reconfirms equivalent criteria after a previous attempt expired,
  TTL policy must decide whether this is duplicate or new attempt;
- key must remain non-public and must not be returned in API response.

Future attempt store skeleton должен сохранять key internal и не должен
создавать public ids.

## Pending lifecycle assessment

Вердикт: attempt store должен link pending confirmation context before future
execution can call search.

Needed linkage:

- attempt -> `AssistantSessionId`;
- attempt -> idempotency key;
- attempt -> command criteria;
- attempt -> pending confirmation snapshot or internal pending reference;
- attempt -> optional created search reference after success.

Pending consumption ordering:

| Event | Pending behavior |
|---|---|
| Guard rejected | Do not consume. |
| Attempt prepared | Do not consume. |
| Attempt in progress | Do not consume yet. |
| Search failed before success | Do not consume. |
| Search succeeded and attempt marked `SUCCEEDED` | Consume after success is recorded. |
| Duplicate after success | Return same search reference; do not create new search. |
| Lost response after success | Store-backed duplicate can recover same search reference. |

Текущий `PendingConfirmationStore.markConsumed` сам по себе недостаточен,
потому что он не хранит created search reference или attempt status.

## Failure / public response assessment

Вердикт: store state is needed before safe public failure/retry behavior.

Failure branches need internal state:

| Store state | Future public response direction |
|---|---|
| no attempt | Safe clarification/boundary, no search reference. |
| `PREPARED` | Execution not started; safe retry possible after policy. |
| `IN_PROGRESS` | Duplicate/in-progress message, no new search. |
| `SUCCEEDED` | Return same created search reference. |
| `FAILED` | Safe retry/clarification text, no search reference unless future policy says otherwise. |

Ограничения public mapping:

- no `hotelSearchId` on failure;
- no `show_hotel_results` unless success is known;
- same created search reference on duplicate after success;
- no raw provider/internal error leakage;
- no new public fields unless separate OpenAPI/frontend step explicitly allows it.

Existing public response shape выглядит достаточным для future minimal
backend-only step, если success использует существующие `show_hotel_results` +
`hotelSearchId`, а failure использует существующий safe
clarification/boundary response. Route tests должны доказать это до execution
wiring.

## Stage 7 strict handoff compatibility

Вердикт: compatible only as explicit-confirmation exception.

Attempt store must serve only the future confirmed-search execution path:

- active pending confirmation required;
- idempotency key derived from session-bound command criteria;
- generic natural-language request does not create attempt;
- generic “yes” without active pending state does not create attempt/search;
- existing `hotel-search;` path remains unchanged and priority-safe;
- duplicate confirmation does not create duplicate search.

Future tests should prove:

- Stage 7 explicit handoff still creates search as before;
- attempt store is not used by strict `hotel-search;`;
- confirmed-search execution cannot bypass guard/attempt checks;
- stale/expired/consumed pending state blocks attempt creation.

## Что не входит в Stage 8.37

- Production code changes.
- Tests.
- `ConfirmedSearchExecutionAttemptStore`.
- Route wiring.
- `Application.kt` changes.
- `AssistantLlmRouteWiringUseCase` changes.
- `PlanPostConfirmationDecisionUseCase` changes.
- `PlanConfirmedSearchCreationUseCase` changes.
- `BuildConfirmedSearchCreationCommandUseCase` changes.
- `PlanConfirmedSearchExecutionUseCase` changes.
- `PlanConfirmedSearchExecutionGuardUseCase` changes.
- `PlanConfirmedSearchExecutionAttemptUseCase` changes.
- Actual `CreateHotelSearchUseCase` call.
- Hotel provider call.
- Actual `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` call.
- Public API/OpenAPI/frontend/generated clients changes.
- Durable storage, auth or booking flow.
- Roadmap/root status changes.

## Риски преждевременного actual execution / route wiring

- Duplicate hotel search on repeated confirmation.
- Search succeeded but response lost, then retry creates another search.
- Pending consumed before attempt success is recorded.
- Created search reference exists but cannot be replayed to duplicate request.
- `IN_PROGRESS` duplicate creates parallel execution.
- Failed attempt retry policy is unclear.
- Expired pending state still maps to old idempotency key.
- Stage 7 strict `hotel-search;` handoff becomes ambiguous.

## Рекомендуемый Stage 8.38

Safe Stage 8.38: backend-only in-memory
`ConfirmedSearchExecutionAttemptStore` skeleton, no route wiring and no search
execution.

Минимальная цель:

- add internal store interface;
- add process-local `InMemoryConfirmedSearchExecutionAttemptStore`;
- support find/create prepared/mark in-progress/mark succeeded/mark failed;
- preserve idempotency key and session boundary;
- model TTL/expiry policy conservatively;
- no `CreateHotelSearchUseCase` call;
- no provider call;
- no actual `hotelSearchId` creation by execution;
- no `markConsumed`;
- no route/runtime composition.

Actual execution route wiring должен оставаться отложенным, пока store
transitions и failure/retry behavior не будут покрыты targeted tests.

## Verdict

Stage 8.37 confirms that an attempt store is needed before actual
confirmed-search execution. The safe next step is a backend-only process-local
store skeleton with transition tests, still without route wiring, search
execution, `hotelSearchId`, `show_hotel_results`, `CreateHotelSearchUseCase`,
provider calls, `markConsumed`, durable storage or public contract changes.
