# Stage 8.20 — Post-confirmation decision composition skeleton

## Цель Stage 8.20

Добавить backend-only internal decision layer, который связывает active pending confirmation state и `ClassifyConfirmationReplyUseCase`.

Stage 8.20 не подключает decision use case к routes, не меняет runtime behavior, не создает hotel search и не создает `hotelSearchId`.

## Что было добавлено

Добавлены internal application-layer типы:

- `PlanPostConfirmationDecisionRequest`;
- `PostConfirmationDecision`;
- `PlanPostConfirmationDecisionUseCase`.

Use case принимает session id, user reply text и current time, читает active pending confirmation через `PendingConfirmationStore`, классифицирует reply через `ClassifyConfirmationReplyUseCase` и возвращает typed internal decision.

## Файлы production code

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanPostConfirmationDecisionRequest.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PostConfirmationDecision.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanPostConfirmationDecisionUseCase.kt`.

Существующие production files не изменялись.

## Тесты

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/PlanPostConfirmationDecisionUseCaseTest.kt`.

Тесты проверяют:

- explicit positive + active pending state -> `Confirmed(criteria)`;
- explicit positive без active pending state -> `NoActivePendingConfirmation`;
- expired pending state -> no confirmed;
- consumed pending state -> no confirmed;
- ambiguous reply -> `NeedsClarification`;
- negative reply -> `Declined`;
- correction reply -> `NeedsReplanning`;
- unknown reply -> `Unknown`;
- confirmed decision returns typed criteria, not raw candidate;
- отсутствие `hotelSearchId` и search creation text;
- deterministic behavior;
- отсутствие route/API/OpenAPI/frontend dependency.

## Decision outcomes

`PostConfirmationDecision` содержит:

| Outcome | Назначение |
|---|---|
| `Confirmed(criteria)` | Active pending confirmation явно подтвержден; criteria остаются internal candidate для future search-creation stage. |
| `NeedsClarification` | Reply ambiguous; confirmation недостаточно явный. |
| `Declined` | Reply negative; future flow может считать pending confirmation candidate for cancellation/consumed handling. |
| `NeedsReplanning` | Reply correction-like; old criteria не должны использоваться для search creation. |
| `NoActivePendingConfirmation` | Missing, expired или consumed pending state. |
| `Unknown` | Reply не относится к supported confirmation vocabulary. |

Эти outcomes являются internal application model, а не public DTO и не OpenAPI values.

## Active pending state rules

- `Confirmed` возможен только если `PendingConfirmationStore.findActiveBySession` вернул active state.
- Missing, expired или consumed state возвращают `NoActivePendingConfirmation`.
- Generic positive reply без active pending state не может дать `Confirmed`.
- Ambiguous reply не может дать `Confirmed`.
- Negative reply не может дать `Confirmed`.
- Correction reply не может дать `Confirmed`.
- Unknown reply не может дать `Confirmed`.

## Classifier/store composition flow

Flow остается internal:

1. `PlanPostConfirmationDecisionUseCase` принимает `PlanPostConfirmationDecisionRequest`.
2. Use case вызывает `PendingConfirmationStore.findActiveBySession(sessionId, now)`.
3. Если active state отсутствует, возвращается `NoActivePendingConfirmation`.
4. Если active state есть, reply классифицируется через `ClassifyConfirmationReplyUseCase`.
5. `ExplicitPositive` возвращает `Confirmed(activePendingConfirmation.criteria)`.
6. Остальные classifications возвращают safe internal decisions без search creation.

Use case не вызывает `save` или `markConsumed`; consumed/cancelled handling остается future composition/runtime step.

## Confirmation search boundary

Use case не:

- создает hotel search;
- создает `hotelSearchId`;
- вызывает hotel provider;
- создает `CreateHotelSearchCommand`;
- возвращает `show_hotel_results`;
- меняет public response;
- выполняет route-level mapping.

## Raw/internal leakage boundary

`Confirmed` возвращает только typed `ProceedWithCandidateCriteria`.

Use case не хранит и не раскрывает:

- raw `LlmCandidate`;
- raw candidate payload;
- `candidatePayload`;
- `modelResponse`;
- provider/model metadata;
- validation issue details;
- confirmation proposal text как public response.

## Подтверждение границ

- Route wiring не менялся.
- Runtime behavior не менялся.
- `Application.kt` не менялся.
- `AssistantLlmRouteWiringUseCase` не менялся.
- Assistant routes не менялись.
- Public API shape, OpenAPI, frontend и generated clients не менялись.
- Durable storage не добавлен.
- Внешний LLM-провайдер, network calls и API keys не добавлены.
- Hotel search не создается.
- `hotelSearchId` не создается.
- Stage 7 strict `hotel-search;` handoff сохранен.
- Bounded hotel-only MVP не расширен.

## Риски и ограничения

- Decision use case пока не подключен к runtime flow.
- `NoActivePendingConfirmation` не различает missing, expired и consumed state, потому что store boundary возвращает только active state.
- `Declined` пока не marks consumed; это будущий explicit runtime step.
- `Confirmed` не создает search и не должен использоваться как public response без отдельного route/contract review.
- Process-local pending state не является durable или production persistence.

## Рекомендуемый Stage 8.21

Stage 8.21 — review/design-only readiness gate для post-confirmation runtime wiring.

Минимальная цель:

- проверить, можно ли безопасно подключать `PlanPostConfirmationDecisionUseCase` к assistant route;
- определить, какие decisions можно отразить через existing public response shape;
- подтвердить, что `Confirmed` все еще не должен создавать search без отдельного explicit future search-creation stage;
- сохранить public API, OpenAPI и frontend unchanged.

Immediate post-confirmation search creation не рекомендуется.

## Verdict

Stage 8.20 выполнен в internal composition skeleton границах. Decision model/use case добавлены и покрыты targeted tests. Routes, runtime behavior, public API, OpenAPI, frontend, durable storage, real provider work, hotel search creation, `hotelSearchId` creation и Stage 7 strict handoff не менялись.
