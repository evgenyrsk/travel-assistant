# Stage 9.17a1 — backend async/result contract migration

## Роль документа

Этот документ является implementation review-артефактом Stage 9.17a1. Он
фиксирует выполненную внутреннюю миграцию provider/search/assistant цепочки и не
является новым источником roadmap. Актуальный следующий этап задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Атомарно перевести существующую I/O-цепочку на `suspend` и application-owned
typed results, сохранив текущее успешное поведение `FAKE` и не подключая
transport-backed resolver либо `REAL` runtime.

## Изменения production-кода

### Контракты

- `HotelOfferProviderBoundary` перенесен из domain в
  `application.hotel` и теперь объявляет `suspend fun search(...)`;
- добавлены `HotelOfferProviderResult`, `CreateHotelSearchResult` и безопасный
  `HotelLocationSuggestion` без `destinationId`;
- `HotelSearchBoundary.createSearch()` и
  `AssistantSessionBoundary.acceptUserMessage()` стали `suspend`;
- `createSession()` и `getSearch()` остались синхронными.

Typed provider result различает:

- выполненный поиск, включая пустой список;
- ненайденное или неоднозначное направление;
- отклонение request/response mapping;
- временную недоступность provider.

Причины остаются provider-independent. Raw body, headers, URL, exception text и
provider candidates в public response не передаются.

### Search state

`CreateHotelSearchUseCase` создает `HotelSearch`, генерирует `hotelSearchId`,
ранжирует offers и сохраняет state только для
`HotelOfferProviderResult.SearchCompleted`:

| Результат provider | Search state |
|---|---|
| Непустой список | `COMPLETED_WITH_OFFERS` |
| Пустой список после выполненного поиска | `COMPLETED_NO_OFFERS` |
| Любой typed failure | search и ID не создаются |

### Provider implementations

- `FakeHotelOfferProvider` возвращает прежние детерминированные offers внутри
  `SearchCompleted`;
- неисполняющий `RealHotelOfferProviderAdapter` возвращает
  `ProviderUnavailable(UNAVAILABLE)` и больше не имитирует успешный пустой
  поиск;
- factory продолжает выбирать provider по прежней конфигурации;
- transport, resolver и runtime composition не подключены.

### Routes и assistant flow

- direct hotel-search сохраняет `202 Accepted` для `Created`;
- location и request outcomes используют существующий
  `400 VALIDATION_ERROR`;
- response/provider failures используют существующий безопасный
  `500 INTERNAL_ERROR`;
- assistant flow возвращает `ask_clarification` без `hotelSearchId` для всех
  typed failures;
- location suggestions и внутренние причины не раскрываются;
- `show_hotel_results` возвращается только для `Created`.

Public request/response schema не расширялась.

### Confirmed-search transition

- execution и response composition стали `suspend`;
- добавлен `SearchNotCreated` с сохранением typed outcome;
- попытка помечается существующим `SEARCH_CREATION_FAILED`;
- pending confirmation не потребляется при неуспешном создании search;
- coroutine cancellation пробрасывается без преобразования в failure.

## Измененные области

Production:

- `application.hotel` boundaries, results и `CreateHotelSearchUseCase`;
- assistant handoff, confirmed transition и response mapping;
- direct hotel-search route;
- fake/real provider implementations и factory import.

Tests:

- application search и assistant use cases;
- confirmed-search execution/composition/mapping;
- direct route и provider seam integration;
- fake/real provider behavior и suspend test doubles.

`Application.kt`, configuration, HTTP transport и provider orchestration не
изменялись.

## Проверки

- targeted tests для use cases, routes и provider seam — пройдены;
- полный backend `./gradlew test` — пройден;
- production `runBlocking` отсутствует;
- `git diff --check` выполняется перед commit.

## Границы этапа

Не добавлены:

- transport-backed autocomplete resolver;
- вызовы Hotels API и новые network engines;
- `REAL` runtime wiring;
- retries, pagination или polling;
- OpenAPI, frontend или generated clients;
- durable storage, booking/payment и `bookHash`.

`FAKE` остается provider по умолчанию.

## Риски и ограничения

- `REAL` остается явным неисполняющим skeleton;
- runtime location resolution отсутствует;
- public configuration и lifecycle production `HttpClient` остаются задачей
  Stage 9.17c;
- location candidates пока не представлены отдельным public contract;
- in-memory stores не обеспечивают production durability.

## Следующий этап

Stage 9.17b — transport-backed autocomplete resolver через `MockEngine`:

- отдельный request DTO с полем `input`;
- безопасное преобразование public autocomplete response в application result;
- отсутствие автоматического выбора первого candidate;
- без runtime wiring и live calls.

## Verdict

`READY_FOR_STAGE_9_17B_NOT_READY_FOR_REAL_RUNTIME_WIRING`.

Async/result migration завершена, `FAKE` behavior сохранен, а ложный
`COMPLETED_NO_OFFERS` для неисполняющего REAL provider устранен.

## Связанные документы

- [Stage 9.17a — согласование async provider и typed result boundaries](stage-9-17a-async-provider-result-contract-reconciliation.md)
- [Stage 9.17 — gate готовности REAL runtime wiring](stage-9-17-real-runtime-wiring-readiness-gate.md)
- [Backend layering rules](../architecture/backend-layering-rules.md)
- [Основной roadmap](../roadmap/roadmap.md)
