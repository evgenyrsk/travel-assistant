# Stage 8.30 — Confirmed-search command builder skeleton

## Цель Stage 8.30

Добавить backend-only internal command builder, который преобразует session-bound confirmed-search plan в command-ready result для будущего hotel search creation.

Stage 8.30 связывает:

- `AssistantSessionId`;
- `ConfirmedSearchCreationPlan.ReadyToCreateSearch`;
- existing `CreateHotelSearchCommand`;
- lifecycle policy из confirmed-search plan.

Builder не подключается к routes и не запускает actual search execution.

## Что было добавлено

Добавлены internal application-layer типы:

- `ConfirmedSearchCreationCommandPlan`;
- `BuildConfirmedSearchCreationCommandUseCase`.

`BuildConfirmedSearchCreationCommandUseCase` принимает explicit `AssistantSessionId` и `ConfirmedSearchCreationPlan.ReadyToCreateSearch`, затем возвращает `ConfirmedSearchCreationCommandPlan.CommandReady`.

## Production files

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchCreationCommandPlan.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/BuildConfirmedSearchCreationCommandUseCase.kt`.

Существующие production files не изменялись.

## Tests

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/BuildConfirmedSearchCreationCommandUseCaseTest.kt`.

Тесты проверяют:

- session id берется только из explicit builder input;
- `HotelSearchCriteria` копируется из `ReadyToCreateSearch`;
- lifecycle policy остается доступной рядом с command;
- session не infer из free-form criteria text;
- command plan не содержит search execution markers;
- deterministic behavior.

## Builder input/output

Input:

- `AssistantSessionId`;
- `ConfirmedSearchCreationPlan.ReadyToCreateSearch`.

Output:

- `ConfirmedSearchCreationCommandPlan.CommandReady`.

`CommandReady` содержит:

- existing `CreateHotelSearchCommand`;
- `ConfirmedSearchCreationLifecyclePolicy`.

## Command mapping rules

| Target | Source |
|---|---|
| `CreateHotelSearchCommand.sessionId` | explicit `AssistantSessionId` input |
| `CreateHotelSearchCommand.criteria` | `ReadyToCreateSearch.criteria` |
| `CommandReady.lifecyclePolicy` | `ReadyToCreateSearch.lifecyclePolicy` |

Rules:

- session id не читается из public request body;
- session id не читается из free-form text;
- criteria не строится заново и не нормализуется повторно;
- idempotency key не создается;
- source marker или correlation id не добавляются;
- public fields не создаются.

## Session boundary

`AssistantSessionId` добавляется только на command builder boundary.

Builder не проверяет active pending confirmation state сам. Это остается responsibility будущей route/runtime composition, которая должна вызвать builder только после:

- active session-bound pending confirmation;
- `PostConfirmationDecision.Confirmed`;
- non-expired и non-consumed pending state;
- accepted typed criteria.

Такой split сохраняет builder deterministic и не смешивает command construction с pending state lifecycle.

## Lifecycle / failure / idempotency boundary

Builder переносит `ConfirmedSearchCreationLifecyclePolicy` из plan в command-ready result.

Policy остается metadata:

- consume pending state только после будущего successful search creation;
- не consume при search failure;
- duplicate confirmation требует future idempotency guard.

Stage 8.30 не решает:

- retry behavior;
- lost response after successful search creation;
- idempotency storage;
- mapping pending confirmation to created hotel search;
- failure response mapping;
- когда route должен вызывать `markConsumed` после execution.

Command construction не является execution readiness.

## No-route-wiring boundary

Builder не подключен к:

- assistant routes;
- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- `PlanConfirmedSearchCreationUseCase`;
- pending confirmation store;
- runtime composition.

Runtime behavior не изменен.

## No-search-execution boundary

Builder не:

- создает hotel search;
- создает `hotelSearchId`;
- возвращает `show_hotel_results`;
- вызывает `CreateHotelSearchUseCase`;
- вызывает hotel provider;
- читает или пишет pending store;
- вызывает `markConsumed`;
- сохраняет search state.

Stage 7 strict `hotel-search;` handoff остается единственным current automatic search creation path.

## Raw/internal leakage boundary

Builder работает только с typed `ConfirmedSearchCreationPlan.ReadyToCreateSearch` и explicit `AssistantSessionId`.

Builder не принимает, не хранит и не раскрывает:

- raw `LlmCandidate`;
- `candidatePayload`;
- `modelResponse`;
- provider/model metadata;
- validation issue details;
- confidence или safety markers;
- free-form user text.

## Public API / OpenAPI / frontend / generated clients verdict

- Public API request/response shape не изменен.
- OpenAPI contracts не менялись.
- Frontend не менялся.
- Generated clients не создавались и не обновлялись.
- Новые public fields или `nextAction` values не добавлены.

## Stage 7 strict handoff compatibility

Совместимо.

Stage 8.30 добавляет только internal command construction. Existing Stage 7 strict `hotel-search;` handoff не менялся и остается единственным current path, который создает hotel search.

Future confirmed-search route execution должно оставаться explicit-confirmation exception:

- command строится only after `Confirmed`;
- command строится only with active session-bound pending confirmation в future composition layer;
- command не строится из ambiguous, negative, correction, unknown или missing pending state;
- silent natural-language handoff не добавляется.

## Durable storage / provider / network / access-key verdict

Stage 8.30 не добавляет:

- durable storage;
- database или filesystem persistence;
- real hotel provider;
- real LLM provider;
- external calls;
- access keys или environment variables;
- auth или booking flow.

## Риски и ограничения

- Builder пока не используется runtime code.
- Builder не выполняет search и не проверяет session existence.
- Builder не решает idempotency.
- Builder не решает retry/lost response.
- Builder не связывает pending confirmation with created hotel search.
- Process-local pending confirmation limitation остается.

## Рекомендуемый Stage 8.31

Stage 8.31 не должен сразу включать route search execution, если failure/idempotency остаются unresolved.

Безопасный следующий шаг: review/design-only или backend-only internal execution readiness gate для confirmed-search command execution, чтобы определить:

- как безопасно вызывать `CreateHotelSearchUseCase`;
- что возвращать при search failure;
- когда вызывать `markConsumed`;
- как избежать duplicate search на repeated confirmation;
- нужен ли internal idempotency marker before route wiring;
- какие route/application tests нужны перед возвратом `show_hotel_results`.

## Verdict

Stage 8.30 выполнен как internal command builder skeleton. `BuildConfirmedSearchCreationCommandUseCase` превращает explicit `AssistantSessionId` и `ConfirmedSearchCreationPlan.ReadyToCreateSearch` в `ConfirmedSearchCreationCommandPlan.CommandReady` с `CreateHotelSearchCommand` и lifecycle policy. Routes, runtime behavior, public API, OpenAPI, frontend, generated clients, durable storage, работа с real provider, hotel search execution, `hotelSearchId`, `show_hotel_results`, `CreateHotelSearchUseCase` call и `markConsumed` call не добавлены.
