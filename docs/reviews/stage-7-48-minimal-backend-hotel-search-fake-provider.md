# Stage 7.48 — Минимальный backend-поток поиска отелей с fake provider

## 1. Verdict

Passed — минимальный backend-поток поиска отелей добавлен.

Stage 7.48 заменяет hotel-search placeholders на небольшой process-local flow, который проверяет существующую assistant session, вызывает детерминированный `FakeHotelOfferProvider` и возвращает нормализованные hotel offers без external I/O.

## 2. Объём этапа

Реализовано:

- contract-shaped `POST /api/v1/hotel-searches`;
- contract-shaped `GET /api/v1/hotel-searches/{searchId}/offers`;
- transport validation минимальных hotel search criteria;
- application use case и process-local search store;
- provider-agnostic offer boundary;
- local deterministic `FakeHotelOfferProvider`;
- targeted application/API tests;
- `HOTEL_SEARCH_NOT_FOUND` mapping.

Не реализованы real provider integration, ranking, explanation, Assistant-to-search handoff, frontend, generated clients, manifest expansion, durable storage, LLM orchestration, auth и production hardening.

## 3. Краткое описание реализации

| Область | Файлы | Решение |
|---|---|---|
| Composition root | `services/backend/src/main/kotlin/com/travelassistant/backend/Application.kt` | Assistant и hotel search используют один process-local session store; provider/store dependencies создаются явно |
| API routes и DTO | `api/HotelSearchRoutes.kt`, `api/HotelSearchRequest.kt`, `api/HotelSearchResponse.kt`, `api/HotelSearchCriteriaResponse.kt`, `api/HotelOffersResponse.kt`, `api/HotelOfferResponse.kt` | Request/response shape следует существующему Stage 6 OpenAPI без contract update |
| Application | `application/hotel/**` | `CreateHotelSearchUseCase` проверяет session, вызывает provider, сохраняет и возвращает search |
| Domain | `domain/hotel/**`, `domain/provider/HotelOfferProviderBoundary.kt` | Добавлены provider-independent criteria, search и offer models |
| Infrastructure | `infrastructure/provider/FakeHotelOfferProvider.kt` | Детерминированный local adapter без HTTP/network calls |
| Error mapping | `api/ErrorHandling.kt`, `api/ErrorResponse.kt` | Unknown search возвращает `404 HOTEL_SEARCH_NOT_FOUND` |

`POST /hotel-searches` возвращает `202 Accepted` с `completed_with_offers` для синхронно завершённого local fake search. `GET /hotel-searches/{searchId}/offers` возвращает terminal result envelope. Асинхронно-совместимая contract shape сохранена, но фоновые jobs не добавлялись.

OpenAPI contract не менялся: текущие endpoints и schemas уже описывали необходимый flow.

## 4. Validation behavior

Transport boundary проверяет:

- наличие непустого `sessionId`;
- наличие `criteria`;
- непустой `criteria.destination`;
- ISO-8601 format для `checkInDate` и `checkOutDate`;
- `checkOutDate` позже `checkInDate`;
- наличие `guests` и `guests.adults >= 1`;
- `guests.children >= 0`;
- `rooms >= 1`, если поле передано;
- наличие visible `room_count` derived assumption, если `rooms` не передан.

Invalid request возвращает `400 VALIDATION_ERROR` с точным field path. Unknown assistant session возвращает `404 SESSION_NOT_FOUND`. Unknown hotel search возвращает `404 HOTEL_SEARCH_NOT_FOUND`.

Optional `rooms` не получает скрытый default: при отсутствии поля transport boundary требует visible `room_count` entry в `derivedAssumptions`. Budget/preferences и остальные assumption fields могут быть декодированы как contract context, но текущий минимальный use case их не применяет.

## 5. Tests

Добавлены:

- `api/HotelSearchRoutesTest.kt`:
  - полный session → create search → get offers flow;
  - stable response fields для будущего UI/assistant handoff;
  - required destination validation;
  - visible `room_count` assumption без скрытого room default;
  - unknown assistant session;
  - unknown hotel search;
- `application/hotel/CreateHotelSearchUseCaseTest.kt`:
  - deterministic fake offers;
  - process-local search persistence;
  - отсутствие provider-specific или external client dependency.

Устаревший hotel-search `501` assertion удалён из `PlaceholderRoutesTest`; unknown-route coverage сохранено.

## 6. Явно вне этапа

- real hotel provider/API integration;
- external HTTP/network calls;
- frontend implementation;
- generated clients;
- manifest changes или expansion;
- CI/Gradle integration;
- conformance tool changes;
- LLM orchestration;
- ranking и explanation behavior;
- durable DB/storage;
- authentication/authorization;
- production readiness;
- финальная готовность Stage 7.

## 7. Оставшаяся работа Stage 7

- минимальное ранжирование hotel offers без изменения provider facts;
- explanation boundary поверх ранжированных/выбранных offers;
- Assistant-to-search handoff;
- минимальный frontend flow и frontend/backend integration;
- итоговая сверка hotel-only MVP slice;
- generated clients и manifest expansion остаются отложенными до отдельного фактического решения.

## 8. Проверки

| Command | Result |
|---|---|
| `git status --short` перед изменениями | Passed; working tree clean. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test --no-daemon` из `services/backend` | Passed; backend build и tests successful. |
| `git diff --check` | Passed; whitespace errors отсутствуют. |
| Targeted link/status search | Passed; report зарегистрирован в reviews index и primary roadmap. |
| Final diff scope inspection | Passed; frontend, OpenAPI, generated clients, manifest, Gradle/CI и conformance tool не менялись. |

Backend server не запускался. HTTP/network calls не выполнялись. OpenAPI validation не запускалась, потому что OpenAPI и conformance tool не менялись.
