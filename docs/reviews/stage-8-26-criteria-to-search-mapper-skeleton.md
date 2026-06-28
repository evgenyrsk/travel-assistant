# Stage 8.26 — Criteria-to-search mapper skeleton

## Цель Stage 8.26

Добавить backend-only internal mapper, который преобразует typed `ProceedWithCandidateCriteria` в existing hotel search criteria model, используемый Stage 7 hotel search flow.

Stage 8.26 не подключает mapper к routes, не запускает hotel search, не создает `hotelSearchId` и не возвращает `show_hotel_results`.

## Что было добавлено

Добавлен internal application-layer mapper:

- `ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper`.

Mapper принимает только accepted typed `ProceedWithCandidateCriteria` и возвращает `HotelSearchCriteria`.

## Production files

Создан:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper.kt`.

Существующие production files не изменялись.

## Tests

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateCriteriaToHotelSearchCriteriaMapperTest.kt`.

Тесты проверяют:

- mapping всех required fields;
- сохранение dates без дополнительной normalization;
- mapping guests и rooms, включая `children = 0`;
- destination как already accepted typed value;
- отсутствие silent defaults сверх значений criteria;
- deterministic behavior;
- отсутствие search-side-effect markers или raw LLM markers в mapped result.

## Mapper input/output

Input:

- `ProceedWithCandidateCriteria`.

Output:

- `HotelSearchCriteria`.

Mapper не возвращает `CreateHotelSearchCommand`, потому что command требует `AssistantSessionId`, а Stage 8.26 по scope принимает только criteria. Future route/runtime composition должен добавить session-bound command construction отдельно.

## Mapping rules

| `ProceedWithCandidateCriteria` | `HotelSearchCriteria` |
|---|---|
| `destination` | `destination` |
| `checkInDate` | `checkInDate` |
| `checkOutDate` | `checkOutDate` |
| `guests.adults` | `guests.adults` |
| `guests.children` | `guests.children` |
| `rooms` | `rooms` |

Правила:

- mapper не добавляет hidden defaults;
- mapper не меняет dates;
- mapper не нормализует destination повторно;
- mapper не интерпретирует raw user text;
- mapper не вызывает hotel search use case или provider boundary.

## Validation assumptions

Mapper assumes accepted input.

Validation остается responsibility of `ProceedWithCandidateCriteriaValidator` and confirmation planning flow:

- destination уже non-blank;
- dates уже parseable и `checkOutDate` позже `checkInDate`;
- adults >= 1;
- children >= 0;
- rooms >= 1;
- unsupported/non-hotel, partial, conflicting или unsafe candidate уже rejected до mapper.

Stage 8.26 не дублирует validator, чтобы mapper остался простым boundary conversion.

## No-route-wiring boundary

Mapper не подключен к:

- assistant routes;
- `Application.kt`;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- pending confirmation store;
- runtime composition.

Runtime behavior не изменен.

## No-search-creation boundary

Mapper не:

- создает hotel search;
- создает `hotelSearchId`;
- возвращает `show_hotel_results`;
- вызывает `CreateHotelSearchUseCase`;
- вызывает hotel provider;
- сохраняет search state.

Stage 7 strict `hotel-search;` handoff остается единственным current automatic search creation path.

## Raw/internal leakage boundary

Mapper работает только с typed `ProceedWithCandidateCriteria`.

Он не принимает, не хранит и не раскрывает:

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

Stage 8.26 добавляет только internal criteria mapper. Existing Stage 7 strict `hotel-search;` handoff не менялся и остается единственным current path, который вызывает hotel search creation.

## Durable storage / provider / network / access-key verdict

Stage 8.26 не добавляет:

- durable storage;
- database или filesystem persistence;
- real hotel provider;
- real LLM provider;
- external calls;
- access keys или environment variables;
- auth или booking flow.

## Риски и ограничения

- Mapper пока не используется runtime code.
- Mapper не решает lifecycle/idempotency для future `Confirmed(criteria) -> search creation`.
- Mapper не решает search failure behavior.
- Mapper не добавляет public confirmation id.
- Future `CreateHotelSearchCommand` construction still needs session-bound composition.

## Рекомендуемый Stage 8.27

Stage 8.27 должен остаться без immediate search creation.

Безопасный следующий шаг: review/design-only lifecycle and command-construction gate for `PostConfirmationDecision.Confirmed(criteria) -> CreateHotelSearchCommand`, чтобы определить:

- где добавлять session id;
- когда вызывать mapper;
- consume-before/after-search ordering;
- retry/idempotency behavior;
- failure response mapping;
- какие route tests нужны перед actual search creation.

## Verdict

Stage 8.26 выполнен как backend-only internal mapper skeleton. Добавлен deterministic criteria-to-search mapper и targeted unit tests. Routes, runtime behavior, public API, OpenAPI, frontend, generated clients, durable storage, real provider work, hotel search creation, `hotelSearchId` и `show_hotel_results` не добавлены.
