# Stage 8.17 — Pending confirmation state skeleton

## Цель Stage 8.17

Добавить backend-only internal pending confirmation state model и process-local store boundary для будущей обработки ответа на подтверждение.

Stage 8.17 не подключает state к routes, не меняет runtime behavior и не создает hotel search.

## Что было добавлено

Добавлены internal application-layer типы:

- `PendingProceedWithCandidateConfirmation`;
- `PendingConfirmationStatus`;
- `PendingConfirmationStore`;
- `InMemoryPendingConfirmationStore`.

Store является process-local in-memory boundary для будущего wiring. Он не является public DTO, public contract или долговременным хранилищем.

## Файлы production code

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PendingProceedWithCandidateConfirmation.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PendingConfirmationStatus.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PendingConfirmationStore.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/InMemoryPendingConfirmationStore.kt`.

Существующие production files не изменялись.

## Тесты

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/InMemoryPendingConfirmationStoreTest.kt`.

Тесты проверяют:

- сохранение и загрузку active pending confirmation;
- expired confirmation не возвращается как active;
- consumed confirmation не возвращается как active;
- `markConsumed` обновляет status и timestamp;
- pending confirmation изолирован по session;
- отсутствие raw candidate fields;
- отсутствие `hotelSearchId` и текста о создании search;
- deterministic time inputs;
- отсутствие зависимости от provider, external call или credentials.

## Поля state model

`PendingProceedWithCandidateConfirmation` содержит:

- `sessionId`;
- accepted typed `ProceedWithCandidateCriteria`;
- safe `ProceedWithCandidateConfirmationProposal`;
- `createdAt`;
- `updatedAt`;
- `expiresAt`;
- `status`.

Модель хранит typed criteria/proposal, а не raw candidate payload.

## Store boundary

`PendingConfirmationStore` определяет:

- `save`;
- `findActiveBySession`;
- `markConsumed`.

`InMemoryPendingConfirmationStore` реализует этот boundary process-local in memory. Store не подключен к runtime composition и не вызывается из routes.

## Поведение expiry и consumed

Правила:

- active pending confirmation возвращается только если status равен `PENDING` и `now` раньше `expiresAt`;
- confirmation считается expired, если `now >= expiresAt`;
- consumed confirmation не active;
- `markConsumed` переводит state в `CONSUMED` и обновляет `updatedAt`;
- missing session не возвращает active confirmation.

## Process-local и non-durable ограничение

State находится только в памяти процесса и теряется при restart.

Это намеренное ограничение Stage 8.17. Durable persistence, cross-instance coordination, account ownership и recovery behavior остаются будущими отдельными решениями.

## Граница утечки raw candidate

Pending state не хранит:

- raw `LlmCandidate`;
- raw candidate payload;
- `candidatePayload`;
- `modelResponse`;
- extracted constraints map;
- validation issue names;
- internal warnings/conflicts;
- provider/model metadata;
- `hotelSearchId`.

## Подтверждение границ

- Route wiring не менялся.
- Runtime behavior не менялся.
- `Application.kt` не менялся.
- `AssistantLlmRouteWiringUseCase` не менялся.
- Assistant routes не менялись.
- Hotel search не создается.
- `hotelSearchId` не создается.
- Stage 7 strict `hotel-search;` handoff сохранен.
- Public API shape, OpenAPI, frontend и generated clients не менялись.
- Внешний LLM-провайдер, external calls и ключи доступа не добавлены.
- Bounded hotel-only MVP не расширен.

## Риски и ограничения

- Pending confirmation state пока не подключен к route/runtime flow.
- Confirmation reply recognition пока не реализован.
- Search creation после confirmation остается отложенным.
- Process-local state не переживает restart и не является production persistence.
- Нет public confirmation id или structured frontend state.

## Рекомендуемый Stage 8.18

Stage 8.18 — review/design-only confirmation reply recognition boundary.

Минимальная цель:

- определить, как распознавать explicit positive/negative confirmation только при active pending state;
- определить поведение для missing/expired/consumed state;
- сохранить запрет на search creation до отдельного будущего runtime step;
- не менять public API, OpenAPI или frontend.

Немедленное создание search после подтверждения не рекомендуется.

## Verdict

Stage 8.17 выполнен в state/store skeleton границах. Internal pending confirmation state и process-local store добавлены и покрыты targeted tests. Routes, runtime behavior, public API, OpenAPI, frontend, durable persistence, real provider work, hotel search creation и Stage 7 strict handoff не менялись.
