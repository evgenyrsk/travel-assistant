# Stage 8.19 — Confirmation reply classifier skeleton

## Цель Stage 8.19

Добавить backend-only internal confirmation reply classifier skeleton для следующего user reply после confirmation prompt.

Stage 8.19 не подключает classifier к routes, не читает pending confirmation store, не меняет runtime behavior и не создает hotel search.

## Что было добавлено

Добавлены internal application-layer типы:

- `ConfirmationReplyClassification`;
- `ClassifyConfirmationReplyUseCase`.

Classifier принимает только raw reply text как application input и возвращает typed internal classification. Он не является public DTO, OpenAPI shape или route-level mapper.

## Файлы production code

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmationReplyClassification.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ClassifyConfirmationReplyUseCase.kt`.

Существующие production files не изменялись.

## Тесты

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/ClassifyConfirmationReplyUseCaseTest.kt`.

Тесты проверяют:

- explicit positive examples;
- negative examples;
- correction examples;
- ambiguous examples;
- unknown/unrelated examples;
- whitespace, case и punctuation normalization;
- deterministic classification;
- отсутствие active pending state input requirement;
- отсутствие `hotelSearchId`, search creation text, provider/network/API-key leakage.

## Classification outcomes

`ConfirmationReplyClassification` содержит:

| Outcome | Назначение |
|---|---|
| `ExplicitPositive` | Reply явно подтверждает предложенные criteria. |
| `Ambiguous` | Reply короткий или недостаточно ясный. |
| `Negative` | Reply явно отменяет или отклоняет confirmation. |
| `Correction` | Reply содержит сигнал изменения criteria. |
| `Unknown` | Reply не относится к confirmation vocabulary. |

Каждый outcome имеет internal `Reason`. Эти reasons не являются public response fields.

## Conservative recognition rules

`ExplicitPositive` распознается только для ограниченного словаря:

- `да`;
- `да, ищи`;
- `да, проверь отели`;
- `подтверждаю`;
- `всё верно`;
- `yes`;
- `confirm`;
- `looks good`;
- `ок, ищи`.

`Negative` распознается для коротких явных отказов:

- `нет`;
- `не надо`;
- `cancel`;
- `stop`;
- `no`.

`Correction` имеет приоритет над positive/negative classification, если reply содержит:

- markers вроде `лучше`, `измени`, `поменяй`, `change`;
- guest/room-like signals вроде `2 взрослых`;
- date-like signals вроде `с 10 по 15 июля`.

`Ambiguous` покрывает короткие неопределенные replies:

- `ок`;
- `угу`;
- `давай`;
- `go`;
- `maybe`.

Остальной текст классифицируется как `Unknown`.

## Active pending state boundary

Classifier не проверяет active pending confirmation state.

Это намеренное ограничение Stage 8.19:

- classifier не принимает `PendingConfirmationStore`;
- classifier не вызывает `findActiveBySession`;
- classifier не вызывает `markConsumed`;
- classifier не вызывает `save`;
- classifier не знает про session ownership или expiry.

Проверка active/expired/consumed state должна быть отдельным future composition step.

## Confirmation search boundary

Classifier не:

- создает hotel search;
- создает `hotelSearchId`;
- вызывает hotel provider;
- меняет session state;
- выполняет route-level mapping;
- вызывает LLM;
- выполняет network calls.

## Raw/internal leakage boundary

Classifier не хранит и не раскрывает:

- raw `LlmCandidate`;
- raw candidate payload;
- `candidatePayload`;
- `modelResponse`;
- provider/model metadata;
- validation issue details;
- pending confirmation criteria/proposal.

## Подтверждение границ

- Route wiring не менялся.
- Runtime behavior не менялся.
- `Application.kt` не менялся.
- `AssistantLlmRouteWiringUseCase` не менялся.
- `PendingConfirmationStore` не подключался.
- Hotel search не создается.
- `hotelSearchId` не создается.
- Stage 7 strict `hotel-search;` handoff сохранен.
- Public API shape, OpenAPI, frontend и generated clients не менялись.
- Внешний LLM-провайдер, network calls и API keys не добавлены.
- Durable storage, auth и booking flow не добавлены.
- Bounded hotel-only MVP не расширен.

## Риски и ограничения

- Classifier сам по себе не доказывает, что reply относится к active confirmation prompt.
- Ambiguous replies intentionally do not become confirmation.
- Correction detection остается conservative heuristic и не заменяет future state-aware composition.
- Search creation после confirmation остается отдельным будущим runtime step.
- Classifier не должен становиться public contract без отдельной OpenAPI/frontend review.

## Рекомендуемый Stage 8.20

Stage 8.20 — backend-only internal post-confirmation decision composition skeleton без route wiring и без search creation.

Минимальная цель:

- связать active pending state result и `ConfirmationReplyClassification`;
- вернуть typed internal outcome для confirmed / ambiguous / declined / correction / missing-expired-consumed state;
- не подключать composition к routes;
- не создавать `hotelSearchId`;
- не менять public API, OpenAPI или frontend.

Immediate post-confirmation search creation не рекомендуется.

## Verdict

Stage 8.19 выполнен в classifier-skeleton границах. Internal deterministic classifier добавлен и покрыт targeted tests. Routes, runtime behavior, public API, OpenAPI, frontend, pending store wiring, durable storage, real provider work, hotel search creation и Stage 7 strict handoff не менялись.
