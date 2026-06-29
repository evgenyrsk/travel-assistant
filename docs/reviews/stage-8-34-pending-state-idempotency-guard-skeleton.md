# Stage 8.34 — Pending-state/idempotency guard skeleton

## Цель Stage 8.34

Добавить backend-only internal guard layer для будущего confirmed-search execution flow.

Guard проверяет, можно ли безопасно дойти до execution boundary на основе session-bound pending confirmation state и command/session alignment. Даже успешная проверка на Stage 8.34 не разрешает actual search execution: результат остается заблокированным до отдельной idempotency/execution policy.

## Что было добавлено

Добавлены internal application-layer типы:

- `ConfirmedSearchExecutionGuardRequest`;
- `ConfirmedSearchExecutionGuardResult`;
- `PlanConfirmedSearchExecutionGuardUseCase`.

Use case принимает read-only snapshot pending confirmation, текущий `AssistantSessionId`, `ConfirmedSearchCreationCommandPlan.CommandReady` и deterministic `now`.

## Production files

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionGuardRequest.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionGuardResult.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanConfirmedSearchExecutionGuardUseCase.kt`.

Существующие production files не изменялись.

## Tests

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/PlanConfirmedSearchExecutionGuardUseCaseTest.kt`.

Тесты проверяют:

- active session-bound pending + matching command возвращает blocked result, а не execution;
- отсутствие active pending отклоняется;
- expired pending отклоняется;
- consumed pending отклоняется;
- pending/session mismatch отклоняется;
- command/session mismatch отклоняется;
- criteria mismatch отклоняется;
- idempotency guard required before execution сохраняется как policy;
- guard остается read-only и не consumes pending state;
- из guard result не утекают search execution markers, raw candidate data или public action markers;
- behavior остается deterministic.

## Guard input/output

Input:

- `AssistantSessionId`;
- `ConfirmedSearchCreationCommandPlan.CommandReady`;
- `PendingProceedWithCandidateConfirmation?`;
- `Instant now`.

Output:

- `ConfirmedSearchExecutionGuardResult`.

Supported outcomes:

| Outcome | Значение |
|---|---|
| `AllowedButBlockedUntilIdempotencyGuard` | Pending state, session и criteria совпали, но actual execution остается заблокированным до будущей idempotency/execution policy. |
| `Rejected(NO_ACTIVE_PENDING_CONFIRMATION)` | Pending confirmation snapshot отсутствует. |
| `Rejected(PENDING_CONFIRMATION_EXPIRED)` | Pending confirmation is expired at `now`. |
| `Rejected(PENDING_CONFIRMATION_CONSUMED)` | Pending confirmation was already consumed. |
| `Rejected(SESSION_MISMATCH)` | Current route session, pending session или command session не совпадают. |
| `Rejected(CRITERIA_MISMATCH)` | Command criteria do not match pending confirmed criteria after internal mapping. |

## Guard checks

`PlanConfirmedSearchExecutionGuardUseCase` checks:

- pending confirmation snapshot существует;
- pending confirmation is not expired;
- pending confirmation is not consumed;
- pending confirmation session matches current `AssistantSessionId`;
- `CommandReady.command.sessionId` matches current `AssistantSessionId`;
- command `HotelSearchCriteria` equals pending `ProceedWithCandidateCriteria` mapped through `ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper`;
- execution остается заблокированным idempotency policy даже когда все проверки прошли.

## Idempotency policy

Stage 8.34 сохраняет existing execution policy:

- `ConfirmedSearchExecutionPolicy.DuplicateHandling.REQUIRE_IDEMPOTENCY_GUARD_BEFORE_EXECUTION`;
- `ConfirmedSearchExecutionPolicy.PendingConsumption.CONSUME_AFTER_FUTURE_SEARCH_SUCCESS`;
- `ConfirmedSearchExecutionPolicy.FailureResponse.OMIT_SEARCH_ID_ON_FAILURE`;
- `ConfirmedSearchExecutionPolicy.RouteContext.REQUIRE_ACTIVE_PENDING_CONFIRMATION`.

Guard result намеренно не создает idempotency key, не хранит created search mapping и не заявляет route execution readiness.

## Read-only pending-state boundary

Guard принимает `PendingProceedWithCandidateConfirmation?` как input snapshot.

It does not:

- call `PendingConfirmationStore.save`;
- call `PendingConfirmationStore.markConsumed`;
- mutate `InMemoryPendingConfirmationStore`;
- update timestamps;
- consume pending state;
- store idempotency metadata.

Это сохраняет Stage 8.34 как internal validation/planning only, а не runtime state transition.

## No-route-wiring boundary

Guard is not connected to:

- assistant routes;
- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- `PlanConfirmedSearchCreationUseCase`;
- `BuildConfirmedSearchCreationCommandUseCase`;
- `PlanConfirmedSearchExecutionUseCase`;
- runtime composition.

Runtime behavior is unchanged.

## No-search-execution boundary

Guard does not:

- create hotel search;
- create actual `hotelSearchId`;
- return `show_hotel_results`;
- call `CreateHotelSearchUseCase`;
- call hotel provider;
- call `markConsumed`;
- persist search state.

Stage 7 strict `hotel-search;` handoff остается единственным current automatic search creation path.

## Raw/internal leakage boundary

Guard работает только с typed internal objects:

- `AssistantSessionId`;
- `ConfirmedSearchCreationCommandPlan.CommandReady`;
- `PendingProceedWithCandidateConfirmation`;
- `HotelSearchCriteria`;
- `ProceedWithCandidateCriteria`.

Guard does not accept, store or expose:

- raw `LlmCandidate`;
- `candidatePayload`;
- `modelResponse`;
- provider/model metadata;
- validation issue details;
- confidence or safety markers;
- free-form user text.

## Public API / OpenAPI / frontend / generated clients verdict

- Public API request/response shape не изменен.
- OpenAPI contracts не менялись.
- Frontend не менялся.
- Generated clients не создавались и не обновлялись.
- Новые public fields или `nextAction` values не добавлены.

## Stage 7 strict handoff compatibility

Совместимо.

Stage 8.34 добавляет только internal guard skeleton. Existing Stage 7 strict explicit `hotel-search;` handoff не менялся и остается единственным current automatic search creation path.

Future confirmed-search execution может быть допустим только как explicit-confirmation exception:

- active session-bound pending confirmation required;
- pending state должен быть non-expired и non-consumed;
- command/session must match current assistant session;
- command criteria must match pending criteria;
- duplicate confirmation/idempotency должны быть обработаны до actual execution;
- generic “yes” without active pending state must not create search.

## Durable storage / provider / network / API keys verdict

Stage 8.34 не добавляет:

- durable storage;
- database, filesystem persistence or Redis;
- real hotel provider;
- real LLM provider;
- network calls;
- API keys, secrets or environment variables;
- auth or booking flow.

## Риски и ограничения

- Guard не route-wired и не выполняет search.
- Guard получает pending snapshot; он не владеет store lookup или state mutation.
- Idempotency остается policy-only: idempotency key, execution attempt record и created search mapping отсутствуют.
- Process-local pending state limitation remains.
- Multi-instance/restart behavior remains unresolved.
- Actual execution по-прежнему требует future explicit gate перед любым `CreateHotelSearchUseCase` call.

## Рекомендуемый Stage 8.35

Safe Stage 8.35: review/design-only confirmed-search execution guard integration gate.

Минимальная цель:

- проверить, достаточно ли Stage 8.34 guard для future execution path;
- решить, нужен ли отдельный idempotency attempt record before execution;
- определить, где должны жить store lookup, guard call, execution call и `markConsumed`;
- зафиксировать failure/duplicate response mapping before route wiring;
- не подключать actual `CreateHotelSearchUseCase` к routes до explicit readiness verdict.

Если gate покажет, что idempotency storage still missing, следующий implementation step должен быть backend-only internal idempotency/attempt result skeleton, а не route execution.

## Verdict

Stage 8.34 выполнен как backend-only internal pending-state/idempotency guard skeleton.

`PlanConfirmedSearchExecutionGuardUseCase` проверяет pending/session/criteria alignment и возвращает typed internal guard result. Даже matching active pending state дает `AllowedButBlockedUntilIdempotencyGuard`, поэтому actual search execution остается deferred. Routes, runtime behavior, public API, OpenAPI, frontend, generated clients, durable storage, provider/network/API keys, actual `hotelSearchId`, `show_hotel_results`, `CreateHotelSearchUseCase` call и `markConsumed` call не добавлены.
