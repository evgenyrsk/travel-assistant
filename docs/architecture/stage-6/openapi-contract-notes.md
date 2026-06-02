# Stage 6.3 — OpenAPI Contract Notes

## Назначение

Этот документ фиксирует пояснения к OpenAPI 3.1 draft для MVP hotel-only API:

- `openapi-draft.yaml`.

Stage 6.1 создал первичный draft, Stage 6.2 провел review, а Stage 6.3 внес точечные contract fixes по Major findings и разрешенным Minor follow-ups. Документ описывает client-facing backend API между frontend и backend, но не создает backend implementation, frontend implementation, DB schema, migrations, provider-specific integration code или production workflow.

## Endpoints в MVP draft

В draft входят только hotel-only endpoints:

- `GET /api/v1/health` — проверка доступности backend.
- `POST /api/v1/assistant/sessions` — создание current-session assistant session.
- `POST /api/v1/assistant/sessions/{sessionId}/messages` — продолжение assistant session и clarification flow.
- `POST /api/v1/hotel-searches` — создание hotel search request по подтвержденным или видимым критериям.
- `GET /api/v1/hotel-searches/{searchId}/offers` — получение hotel offers/search results.
- `GET /api/v1/assistant/sessions/{sessionId}/shortlist` — получение current-session shortlist.
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` — добавление или обновление hotel offer в shortlist текущей сессии.
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` — удаление hotel offer из shortlist текущей сессии.
- `POST /api/v1/assistant/sessions/{sessionId}/explanations` — explanation/comparison для hotel offers в рамках existing session.

Explanation/comparison endpoint включен, потому что Stage 5 baseline описывает assistant explanations and comparisons как часть hotel-only MVP orchestration и frontend/backend integration boundary.

## Stage 6.3 contract fixes

Stage 6.3 закрывает Major findings Stage 6.2:

- `MA-S6.2-001` — room count больше не получает silent default `1`; если `rooms` не передан явно, before-search contract должен показывать visible `DerivedAssumption` категории `room_count`.
- `MA-S6.2-002` — search terminal states представлены единым result envelope pattern: `GET /hotel-searches/{searchId}/offers` возвращает `200` с `HotelOffersResponse` для `completed_with_offers`, `completed_no_offers` и `failed`.

Также добавлены allowed Minor fixes:

- result-level metadata для partial/stale/freshness/provider degraded states;
- resource-specific not-found errors для session, hotel search, hotel offer и shortlist item;
- typed representations for provider facts, assistant assumptions, derived assumptions, user preferences and unknown data.

## Search state pattern

Search lifecycle использует единый enum:

- `accepted` — search request accepted;
- `searching` — provider/search work is still in progress;
- `completed_with_offers` — provider/search completed and returned hotel offers;
- `completed_no_offers` — provider/search completed successfully but no hotel offers matched;
- `failed` — search failed at source/application boundary.

`GET /hotel-searches/{searchId}/offers` возвращает result envelope для terminal states. No offers и provider/search failure не являются validation errors. Validation errors остаются `400`, missing resources остаются `404`, internal backend failure остается `500`.

Failed search envelope использует `HotelSearchFailure` с generic provider/search categories. Это не provider-specific error taxonomy и не retry implementation.

## Facts, assumptions, preferences and unknowns

OpenAPI draft использует typed schemas:

- `ProviderFact` — provider/source-owned fact with optional generic source/freshness marker.
- `UserPreference` — user-provided preference or hard/soft constraint.
- `AssistantAssumption` — assistant interpretation that must not become provider fact.
- `DerivedAssumption` — derived value that affects search, including `room_count` and `guest_count`.
- `UnknownData` — missing or unavailable data that must remain visible when decision-critical.

Room/guest assumptions должны быть visible as `DerivedAssumption`. `rooms` remains optional only because a visible `room_count` assumption can satisfy the Stage 3 requirement before search. This does not introduce room selection or booking flow.

## Сознательно исключено

Из draft сознательно исключены:

- flight search endpoints;
- combined itinerary или combined hotel + flight endpoints;
- booking flow;
- payment flow;
- account management;
- account history;
- persistent saved trips;
- full auth/profile endpoints;
- provider-specific endpoints и provider DTO;
- DB/storage schema, migrations и retention model;
- Redis/cache API или cache invalidation contract;
- real LLM provider contract, prompt templates, streaming protocol и tool calling;
- telemetry, monitoring, DevOps и security/testing backlog.

Current-session shortlist в draft не является account history, persistent saved trips, booking intent или price/availability guarantee.

## Assumptions

- API описывает frontend/backend boundary для Next.js + React + TypeScript frontend и Spring Boot backend, но сам contract остается framework-agnostic.
- `sessionId`, `searchId`, `offerId` и `providerOfferRef` являются opaque identifiers.
- Hotel offers приходят через абстракцию `HotelOfferProvider`; draft не раскрывает real provider contract.
- Provider facts, assistant assumptions, user-provided constraints и unknown data должны оставаться разделенными в response payloads.
- Hotel search может быть asynchronous-friendly: `POST /hotel-searches` возвращает `202`, а results читаются через `GET /hotel-searches/{searchId}/offers`.
- `completed_no_offers` отделен от `failed`, чтобы не смешивать отсутствие подходящих офферов с проблемой источника данных.
- Session persistence ограничена current-session scope. Вопрос, переживает ли session page refresh, остается открытым.

## Ошибки

Draft фиксирует базовые error categories:

- `VALIDATION_ERROR` — invalid request или missing required fields.
- `provider_unavailable` / `provider_failed` — search failure categories inside `HotelSearchFailure`, not no-offers states or HTTP error codes.
- `SESSION_NOT_FOUND` — assistant session не найдена.
- `HOTEL_SEARCH_NOT_FOUND` — hotel search не найден.
- `HOTEL_OFFER_NOT_FOUND` — hotel offer не найден в текущем session/search context.
- `SHORTLIST_ITEM_NOT_FOUND` — shortlist item не найден.
- `INTERNAL_ERROR` — внутренняя ошибка backend.

Точная provider error taxonomy, retry policy, user-facing wording и observability details остаются follow-up темами Stage 6.x / implementation stages.

## Связь со Stage 5 architecture baseline

OpenAPI draft следует Stage 5 baseline:

- сохраняет hotel-only MVP v1 scope;
- держит hotel facts за provider/source boundary;
- не превращает provider abstraction в provider-specific API contract;
- отделяет provider facts от assistant assumptions и unknown data;
- поддерживает chat-first, not chat-only UX через assistant session, Search Intent Summary, hotel results и shortlist;
- сохраняет current-session shortlist как session-level selection aid, а не account storage;
- не добавляет booking, payment, flight, combined itinerary или account flows;
- не создает DB schema, backend/frontend implementation, provider adapter или production integration.

## Открытые вопросы для следующих Stage 6.x

- Какие exact provider fields и freshness/source markers будут доступны после предоставления hotel provider/API contract?
- Нужно ли current-session state переживать page refresh, и если да, на каком минимальном уровне?
- Нужен ли отдельный endpoint для search status, если provider/LLM operations станут long-running, или result envelope достаточно?
- Какой точный validation contract нужен для broad/open destination hotel requests?
- Должен ли explanation/comparison endpoint поддерживать streaming, или обычного synchronous response достаточно для MVP draft?
- Нужна ли отдельная contract review перед генерацией TypeScript/OpenAPI clients?
- Нужен ли отдельный details endpoint, или inline `HotelOffer` достаточно для MVP offer details screen?

## Recommendations, not executed

- После предоставления provider/API contract отдельно проверить mapping provider facts, freshness и limitations.
- Отдельно решить current-session persistence/page refresh behavior before any DB schema or storage model.
- Провести отдельную Stage 6.x review/remediation before client generation, если потребуется дальнейшая детализация details endpoint, long-running search status или generated clients.
