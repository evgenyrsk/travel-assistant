# Stage 7.49 — Минимальное ранжирование hotel offers

## 1. Verdict

Passed — минимальное ранжирование hotel offers добавлено.

Stage 7.49 добавляет небольшой deterministic ranking policy поверх process-local offers из Stage 7.48 и возвращает короткое стабильное объяснение порядка без LLM, персонализации или real provider integration.

## 2. Объём этапа

Реализовано:

- provider-independent ranking policy в domain layer;
- ранжирование offers до сохранения `HotelSearch`;
- стабильный `matchSummary` для каждого ranked offer;
- targeted domain, application и API tests;
- намеренно отличный provider order в `FakeHotelOfferProvider`, чтобы API test подтверждал применение ranking.

Не реализованы AI/LLM ranking, персонализация, criteria-aware scoring, real provider integration, frontend, generated clients, manifest expansion, durable storage, auth и production hardening.

## 3. Краткое описание реализации

| Область | Файлы | Решение |
|---|---|---|
| Domain policy | `domain/hotel/HotelOfferRanker.kt`, `domain/hotel/RankedHotelOffer.kt` | Ranking отделен от provider и transport layers; результат объединяет исходный provider offer и стабильный `matchSummary` |
| Search model | `domain/hotel/HotelSearch.kt` | Process-local search хранит уже ranked offers |
| Application | `application/hotel/CreateHotelSearchUseCase.kt` | Provider output ранжируется перед вычислением status и сохранением search |
| API mapping | `api/HotelOfferResponse.kt`, `api/HotelOffersResponse.kt` | Существующее поле `matchSummary` получает deterministic explanation; metadata явно исключает personalization и LLM |
| Fake provider | `infrastructure/provider/FakeHotelOfferProvider.kt` | Local provider order отличается от итогового ranked order и не содержит ranking logic |

## 4. Поведение ранжирования

Offers сортируются последовательно:

1. подтвержденная availability: `available`, затем `limited`, затем `unknown`;
2. rating по убыванию;
3. total stay price по возрастанию;
4. `offerId` по возрастанию как стабильный tie-breaker.

Текущий `FakeHotelOfferProvider` возвращает offers в одной валюте (`EUR`), поэтому price comparison ограничен текущим local single-currency набором и не заявляет currency normalization. Правило достаточно для Stage 7 foundation: оно детерминировано, не меняет provider facts и легко проверяется без scoring engine.

`matchSummary` является короткой стабильной строкой, зависящей от availability bucket. Это объяснение foundation order, а не персонализированная рекомендация и не AI-generated explanation.

## 5. API / OpenAPI

OpenAPI не менялся.

Существующий `HotelOffer.matchSummary` в `docs/architecture/stage-6/openapi-draft.yaml` уже позволяет вернуть краткое объяснение, основанное на criteria и provider facts. Stage 7.49 использует это optional contract field и не добавляет новую response shape.

Generated clients не создавались и generated-client readiness не заявлена.

## 6. Tests

Добавлен `domain/hotel/HotelOfferRankerTest.kt`, который проверяет:

- availability priority;
- rating order;
- total price order;
- стабильный `offerId` tie-breaker;
- deterministic `matchSummary`.

Обновлены:

- `application/hotel/CreateHotelSearchUseCaseTest.kt` — подтверждает ranked offers и explanation после provider call;
- `api/HotelSearchRoutesTest.kt` — подтверждает итоговый API order и наличие `matchSummary`.

Полный backend test suite также подтверждает, что Stage 7.48 validation и `SESSION_NOT_FOUND` / `HOTEL_SEARCH_NOT_FOUND` behavior не сломаны. `FakeHotelOfferProvider` остается local adapter без HTTP/network client dependency.

## 7. Явно вне этапа

- real hotel provider/API integration;
- external HTTP/network calls;
- frontend implementation;
- generated clients;
- manifest changes или expansion;
- CI/Gradle integration;
- conformance tool changes;
- LLM orchestration;
- AI-generated или персонализированные explanations;
- сложный scoring engine;
- durable DB/storage;
- authentication/authorization;
- production readiness;
- финальная готовность Stage 7.

## 8. Оставшаяся работа Stage 7

- минимальный `Assistant-to-search handoff` для явно собранных structured requirements;
- минимальный frontend flow и frontend/backend integration;
- дальнейшее explanation/shortlist behavior только отдельными практическими задачами;
- итоговая сверка hotel-only MVP slice;
- generated clients и manifest expansion остаются отложенными до отдельного фактического решения.

## 9. Проверки

| Command | Result |
|---|---|
| `git status --short` перед изменениями | Passed; working tree clean. |
| `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test --no-daemon` из `services/backend` | Passed; backend build и tests successful. |
| `git diff --check` | Passed; whitespace errors отсутствуют. |
| Targeted link/status search | Passed; report зарегистрирован в reviews index и primary roadmap. |
| Final diff scope inspection | Passed; frontend, OpenAPI, generated clients, manifest, Gradle/CI и conformance tool не менялись. |

Backend server не запускался. HTTP/network calls не выполнялись. OpenAPI validation не запускалась, потому что OpenAPI и conformance tool не менялись.
