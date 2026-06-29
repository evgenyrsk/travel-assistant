# Stage 8.32 — Confirmed-search execution result skeleton

## Цель Stage 8.32

Добавить backend-only internal execution planning/result layer для будущего confirmed-search execution flow.

Stage 8.32 моделирует typed outcomes и policy для будущих execution success/failure сценариев, но не вызывает actual `CreateHotelSearchUseCase`, не запускает hotel search и не подключается к routes.

## Что было добавлено

Добавлены internal application-layer типы:

- `ConfirmedSearchExecutionPolicy`;
- `ConfirmedSearchExecutionResult`;
- `PlanConfirmedSearchExecutionUseCase`.

`PlanConfirmedSearchExecutionUseCase` принимает `ConfirmedSearchCreationCommandPlan.CommandReady` и возвращает `ConfirmedSearchExecutionResult.PreparedButNotExecuted`.

Это no-op/preparation-only skeleton. Он явно фиксирует, что actual execution не выполняется, пока не появится idempotency guard и отдельный execution boundary.

## Production files

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionPolicy.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionResult.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanConfirmedSearchExecutionUseCase.kt`.

Существующие production files не изменялись.

## Tests

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/PlanConfirmedSearchExecutionUseCaseTest.kt`.

Тесты проверяют:

- `CommandReady` input возвращает internal prepared/not-executed result;
- lifecycle policy сохраняется из command plan;
- result явно требует idempotency guard before actual execution;
- failure policy не допускает search id в failure response;
- consume policy остается after future successful search creation only;
- future route context должен иметь active pending confirmation;
- use case не создает search result и не возвращает public action marker;
- deterministic behavior.

## Result / use case input-output

Input:

- `ConfirmedSearchCreationCommandPlan.CommandReady`.

Output:

- `ConfirmedSearchExecutionResult`.

Текущий Stage 8.32 output:

- `ConfirmedSearchExecutionResult.PreparedButNotExecuted`.

`PreparedButNotExecuted` содержит:

- original `CommandReady`;
- `NotExecutedReason.IDEMPOTENCY_GUARD_REQUIRED`;
- `ConfirmedSearchCreationLifecyclePolicy`;
- `ConfirmedSearchExecutionPolicy`.

## Execution outcome model

`ConfirmedSearchExecutionResult` моделирует future outcomes:

| Outcome | Назначение |
|---|---|
| `PreparedButNotExecuted` | Текущий Stage 8.32 result: execution подготовлен как typed internal state, но не выполнен. |
| `SearchCreated(searchId, lifecyclePolicy, executionPolicy)` | Future success model для отдельного execution stage, который позже безопасно создаст search. |
| `SearchCreationFailed(reason, lifecyclePolicy, executionPolicy)` | Future typed failure model для execution failure без public/internal leakage. |
| `IdempotencyRequired(commandPlan, reason, lifecyclePolicy, executionPolicy)` | Future explicit idempotency branch перед route execution. |

Stage 8.32 use case намеренно возвращает только `PreparedButNotExecuted`.

## Lifecycle / failure / idempotency policy

`ConfirmedSearchExecutionPolicy` фиксирует execution guardrails:

| Policy field | Value | Meaning |
|---|---|---|
| `pendingConsumption` | `CONSUME_AFTER_FUTURE_SEARCH_SUCCESS` | Pending confirmation можно consume только после будущего successful search creation. |
| `failureResponse` | `OMIT_SEARCH_ID_ON_FAILURE` | Failure response не должен включать search id. |
| `duplicateHandling` | `REQUIRE_IDEMPOTENCY_GUARD_BEFORE_EXECUTION` | Duplicate confirmation требует idempotency guard до actual execution. |
| `routeContext` | `REQUIRE_ACTIVE_PENDING_CONFIRMATION` | Future route execution требует active session-bound pending confirmation. |

`ConfirmedSearchCreationLifecyclePolicy` также сохраняется без изменения:

- `CONSUME_AFTER_SEARCH_SUCCESS`;
- `DO_NOT_CONSUME_ON_SEARCH_FAILURE`;
- `REQUIRES_IDEMPOTENCY_GUARD`.

Stage 8.32 не меняет current `markConsumed` behavior.

## No-route-wiring boundary

Execution skeleton не подключен к:

- assistant routes;
- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- `PlanConfirmedSearchCreationUseCase`;
- `BuildConfirmedSearchCreationCommandUseCase`;
- pending confirmation store;
- runtime composition.

Runtime behavior не изменен.

## No-search-execution boundary

Execution skeleton не:

- создает hotel search;
- создает actual `hotelSearchId`;
- возвращает `show_hotel_results`;
- вызывает `CreateHotelSearchUseCase`;
- вызывает hotel provider;
- читает или пишет pending store;
- вызывает `markConsumed`;
- сохраняет search state.

Stage 7 strict `hotel-search;` handoff остается единственным current automatic search creation path.

## Raw / internal leakage boundary

Use case работает только с typed `ConfirmedSearchCreationCommandPlan.CommandReady`.

Use case не принимает, не хранит и не раскрывает:

- raw `LlmCandidate`;
- `candidatePayload`;
- `modelResponse`;
- provider/model metadata;
- validation issue details;
- confidence или safety markers;
- free-form user text.

Failure/success policy остается internal-only и не маппится в public response на Stage 8.32.

## Public API / OpenAPI / frontend / generated clients verdict

- Public API request/response shape не изменен.
- OpenAPI contracts не менялись.
- Frontend не менялся.
- Generated clients не создавались и не обновлялись.
- Новые public fields или `nextAction` values не добавлены.

## Stage 7 strict handoff compatibility

Совместимо.

Stage 8.32 добавляет только internal execution result skeleton. Existing Stage 7 strict `hotel-search;` handoff не менялся и остается единственным current automatic search creation path.

Future confirmed-search route execution должно оставаться explicit-confirmation exception:

- execution только из `ConfirmedSearchCreationCommandPlan.CommandReady`;
- active session-bound pending confirmation required;
- execution не запускается из ambiguous, negative, correction, unknown или missing pending state;
- generic “yes” без pending state не запускает execution;
- silent natural-language handoff не добавляется.

## Durable storage / provider / network / API keys verdict

Stage 8.32 не добавляет:

- durable storage;
- real hotel provider;
- real LLM provider;
- external calls;
- API keys или environment variables;
- auth или booking flow.

## Риски и ограничения

- Use case пока намеренно возвращает только `PreparedButNotExecuted`.
- Actual execution остается deferred.
- Idempotency guard не реализован, только зафиксирован policy.
- Failure response mapping не подключен к routes.
- Pending confirmation lifecycle не меняется.
- Process-local pending confirmation limitation остается.

## Рекомендуемый Stage 8.33

Stage 8.33 не должен сразу подключать route execution, если idempotency и failure execution boundary остаются unresolved.

Безопасный следующий шаг: review/design-only idempotency/failure execution boundary gate или backend-only internal execution boundary skeleton с injected fake/no-op executor, без route wiring.

Минимальная цель Stage 8.33:

- определить, кто владеет idempotency guard;
- определить typed failure outcomes для actual execution boundary;
- определить consume ordering around execution success/failure;
- не подключать `CreateHotelSearchUseCase` к routes до explicit guardrails и tests.

## Verdict

Stage 8.32 выполнен как internal execution result/use case skeleton. `PlanConfirmedSearchExecutionUseCase` принимает `ConfirmedSearchCreationCommandPlan.CommandReady` и возвращает `PreparedButNotExecuted` с lifecycle/execution policy. Routes, runtime behavior, public API, OpenAPI, frontend, generated clients, durable storage, real provider, actual hotel search execution, actual `hotelSearchId`, `show_hotel_results`, `CreateHotelSearchUseCase` call и `markConsumed` call не добавлены.
