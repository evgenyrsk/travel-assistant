# Stage 8.28 — Confirmed-search creation plan skeleton

## Цель Stage 8.28

Добавить backend-only internal planning layer для будущего `PostConfirmationDecision.Confirmed(criteria) -> hotel search creation`.

Stage 8.28 связывает `PostConfirmationDecision.Confirmed`, `ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper` и typed internal plan. Он не подключает этот plan к routes и не вызывает actual search creation.

## Что было добавлено

Добавлены internal application-layer типы:

- `ConfirmedSearchCreationLifecyclePolicy`;
- `ConfirmedSearchCreationPlan`;
- `PlanConfirmedSearchCreationUseCase`.

Use case принимает только `PostConfirmationDecision.Confirmed`, маппит typed criteria в `HotelSearchCriteria` и возвращает `ConfirmedSearchCreationPlan.ReadyToCreateSearch`.

## Production files

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchCreationLifecyclePolicy.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchCreationPlan.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanConfirmedSearchCreationUseCase.kt`.

Существующие production files не изменялись.

## Tests

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/PlanConfirmedSearchCreationUseCaseTest.kt`.

Тесты проверяют:

- `PostConfirmationDecision.Confirmed` -> `ReadyToCreateSearch`;
- все criteria fields сохраняются в planned `HotelSearchCriteria`;
- lifecycle policy требует consume только после будущего успешного search creation;
- failure policy не consumes pending state при search failure;
- duplicate confirmation требует future idempotency guard;
- use case использует mapper boundary;
- plan не содержит search-side-effect markers;
- deterministic behavior.

## Use case input/output

Input:

- `PostConfirmationDecision.Confirmed`.

Output:

- `ConfirmedSearchCreationPlan.ReadyToCreateSearch`.

Plan содержит:

- planned `HotelSearchCriteria`;
- `ConfirmedSearchCreationLifecyclePolicy`.

Use case не принимает generic `PostConfirmationDecision`, чтобы boundary оставался explicit: только confirmed decisions могут попасть в future search creation planning.

## Planned creation flow

Internal flow:

1. Caller получает `PostConfirmationDecision.Confirmed(criteria)` из existing post-confirmation decision layer.
2. `PlanConfirmedSearchCreationUseCase` принимает только confirmed decision.
3. Use case вызывает `ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper`.
4. Mapper возвращает `HotelSearchCriteria`.
5. Use case возвращает `ConfirmedSearchCreationPlan.ReadyToCreateSearch(criteria, lifecyclePolicy)`.

На этом flow останавливается.

Stage 8.28 не:

- добавляет `AssistantSessionId`;
- создает `CreateHotelSearchCommand`;
- вызывает `CreateHotelSearchUseCase`;
- вызывает provider;
- сохраняет hotel search;
- вызывает `markConsumed`;
- возвращает public response.

## Lifecycle / failure / idempotency policy

`ConfirmedSearchCreationLifecyclePolicy` фиксирует future route guardrails:

| Policy field | Value | Meaning |
|---|---|---|
| `pendingConsumption` | `CONSUME_AFTER_SEARCH_SUCCESS` | Future route wiring должен consume pending state только после успешного search creation. |
| `failureHandling` | `DO_NOT_CONSUME_ON_SEARCH_FAILURE` | Unknown или failed search creation не должен silent consume pending confirmation. |
| `duplicateConfirmationHandling` | `REQUIRES_IDEMPOTENCY_GUARD` | Duplicate confirmation требует отдельный idempotency guard before route search creation. |

Это policy metadata, а не runtime behavior. Stage 8.28 не меняет current `markConsumed` behavior из Stage 8.24.

## No-route-wiring boundary

Plan/use case не подключен к:

- assistant routes;
- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- pending confirmation store;
- runtime composition.

Runtime behavior не изменен.

## No-search-creation boundary

Plan/use case не:

- создает hotel search;
- создает `hotelSearchId`;
- возвращает `show_hotel_results`;
- создает `CreateHotelSearchCommand`;
- вызывает `CreateHotelSearchUseCase`;
- вызывает hotel provider;
- читает или пишет pending store;
- вызывает `markConsumed`;
- сохраняет search state.

Stage 7 strict `hotel-search;` handoff остается единственным current automatic search creation path.

## Raw/internal leakage boundary

Use case работает только с `PostConfirmationDecision.Confirmed`, который содержит typed `ProceedWithCandidateCriteria`.

Use case не принимает, не хранит и не раскрывает:

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

Stage 8.28 добавляет internal plan only. Existing Stage 7 strict `hotel-search;` handoff не менялся и остается единственным current automatic search creation path.

Future confirmed-search route wiring должно быть оформлено как explicit-confirmation exception, а не как silent natural-language search handoff.

## Durable storage / provider / network / access-key verdict

Stage 8.28 не добавляет:

- durable storage;
- database или filesystem persistence;
- real hotel provider;
- real LLM provider;
- external calls;
- access keys или environment variables;
- auth или booking flow.

## Риски и ограничения

- Plan пока не используется runtime code.
- Plan не создает `CreateHotelSearchCommand`, потому что session id относится к future route/runtime composition.
- Policy не реализует idempotency; она только фиксирует required future guard.
- Failure handling остается future route/use-case behavior.
- Process-local pending confirmation state still limits any future confirmed-search flow.

## Рекомендуемый Stage 8.29

Stage 8.29 должен остаться без immediate route search creation, если failure/idempotency остаются unresolved.

Безопасный следующий шаг: review/design-only or backend-only internal command construction gate для `ConfirmedSearchCreationPlan -> CreateHotelSearchCommand`, чтобы определить:

- где добавлять current `AssistantSessionId`;
- как представлять command-ready plan without executing search;
- как route будет handling failure до/после search creation;
- как применять idempotency guard;
- когда и где вызывать `markConsumed` после successful search creation.

## Verdict

Stage 8.28 выполнен как internal planning skeleton. `PlanConfirmedSearchCreationUseCase` маппит `PostConfirmationDecision.Confirmed(criteria)` в typed ready plan с `HotelSearchCriteria` и lifecycle policy. Routes, runtime behavior, public API, OpenAPI, frontend, generated clients, durable storage, работа с real provider, hotel search creation, `hotelSearchId`, `show_hotel_results`, `CreateHotelSearchUseCase` call и `markConsumed` call не добавлены.
