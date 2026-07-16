# Stage 9.17a — согласование async provider и typed result boundaries

## Роль документа

Этот документ является review/design-артефактом Stage 9.17a. Он фиксирует
безопасную форму следующей внутренней миграции, но не меняет production code,
tests, public API или поведение runtime. Актуальный порядок этапов задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Согласовать асинхронную application/provider boundary и typed outcomes до
подключения `HotelsApiSearchOrchestrator` к runtime.

## Текущая точка

| Область | Текущее состояние |
|---|---|
| Provider boundary | `HotelOfferProviderBoundary.search()` синхронно возвращает `List<HotelOffer>` и расположен в domain package |
| Search boundary | `HotelSearchBoundary.createSearch()` синхронно возвращает `HotelSearch` |
| Assistant boundary | `AssistantSessionBoundary.acceptUserMessage()` синхронный |
| Real orchestration | `HotelsApiSearchOrchestrator.search()` является `suspend` и возвращает typed internal result |
| Location resolution | Search требует числовой `destinationId`, а runtime передает destination text |
| Runtime | `RealHotelOfferProviderAdapter` остается неподключенным skeleton; `FAKE` используется по умолчанию |

`runBlocking` внутри provider adapter не является допустимым мостом между этими
контрактами.

## Решение по асинхронной границе

Следующая code-миграция должна атомарно изменить существующую цепочку:

- `HotelOfferProviderBoundary.search()` становится `suspend`;
- `HotelSearchBoundary.createSearch()` становится `suspend`;
- `AssistantSessionBoundary.acceptUserMessage()` становится `suspend`;
- вызывающие application use cases и Ktor routes распространяют `suspend`
  естественным образом;
- `AssistantSessionBoundary.createSession()` и
  `HotelSearchBoundary.getSearch()` остаются синхронными, поскольку не содержат
  внешнего I/O в текущем scope.

Временный параллельный async contract только для `REAL` не добавляется.
`runBlocking`, скрытые background jobs и блокирующие адаптеры запрещены.

## Владение provider boundary

Typed result является частью application workflow, поэтому domain package не
должен зависеть от него. В Stage 9.17a1 текущий
`HotelOfferProviderBoundary` нужно атомарно перенести под application ownership,
сохранив его роль и не оставляя одновременно старый и новый runtime contract.

Domain продолжает владеть `HotelSearchCriteria`, `HotelOffer`, ranking rules и
другими provider-independent business concepts. Infrastructure реализует
application-owned boundary и преобразует provider DTO и ошибки на своей
границе.

## Typed provider result

Application-owned `HotelOfferProviderResult` должен различать:

- `SearchCompleted(offers)` — provider search действительно выполнен, включая
  допустимый пустой список;
- `LocationNotFound`;
- `LocationSelectionRequired(suggestions)`;
- `RequestRejected(reason)`;
- `ResponseRejected(reason)`;
- `ProviderUnavailable(reason)`.

`ProviderUnavailable` может сохранять внутреннюю категорию `TIMEOUT`,
`RATE_LIMITED`, `AUTHENTICATION_FAILED`, `UNAVAILABLE` или `UNKNOWN`, но не raw
body, URL, headers и provider exception message.

Provider-specific `HotelsApiSearchMappingError`, числовой `destinationId` и
полный `HotelLocationResolution.Candidate` не должны попадать в domain result
или public response.

Для `LocationSelectionRequired` допускается application-only
`HotelLocationSuggestion` с безопасными отображаемыми полями:

- `name`;
- `signature`;
- `typeCode`;
- `typeName`.

Числовой `destinationId` в suggestion не включается. Новый пользовательский
текст должен запускать location resolution заново; автоматический выбор первого
candidate запрещен.

## Typed search creation result

`CreateHotelSearchUseCase` должен преобразовывать provider result в
application-owned `CreateHotelSearchResult`:

- `Created(search)`;
- `LocationNotFound`;
- `LocationSelectionRequired(suggestions)`;
- `RequestRejected(reason)`;
- `ResponseRejected(reason)`;
- `ProviderUnavailable(reason)`.

Правила сохранения:

| Provider outcome | Создать `HotelSearch` | Создать `hotelSearchId` | Сохранить state |
|---|---:|---:|---:|
| `SearchCompleted(non-empty)` | да | да | да, `COMPLETED_WITH_OFFERS` |
| `SearchCompleted(empty)` | да | да | да, `COMPLETED_NO_OFFERS` |
| Любой другой outcome | нет | нет | нет |

Таким образом, location ambiguity, mapping rejection и provider failure не
маскируются под `COMPLETED_NO_OFFERS`.

## Application и route semantics

### Assistant flow

- `Created` сохраняет существующий успешный путь к `show_hotel_results`;
- `LocationNotFound`, `LocationSelectionRequired` и `RequestRejected`
  возвращают существующий `ask_clarification` без `hotelSearchId`;
- `ResponseRejected` и `ProviderUnavailable` используют безопасное сообщение о
  невозможности выполнить поиск и существующий `ask_clarification`, без raw
  provider details;
- pending confirmation не потребляется при любом outcome, кроме успешно
  созданного и записанного search;
- список location candidates не добавляется в public response на этом этапе.

### Direct hotel-search flow

- `Created` сохраняет текущий `202 Accepted`;
- location outcomes и `RequestRejected` используют существующий
  `ValidationErrorResponse` с полем `destination` или `body`, без
  `hotelSearchId` и списка candidates;
- provider/response failures используют существующую безопасную
  `ErrorResponse`, не раскрывая внутреннюю категорию;
- новый public error schema или новый endpoint не вводятся.

## Cancellation и failures

- coroutine cancellation не преобразуется в provider failure и должна
  распространяться вверх;
- transport timeout и безопасная provider taxonomy преобразуются infrastructure
  adapter в application-owned result;
- raw provider exception не передается в route, assistant reply, state store или
  logs текущего контракта;
- Stage 9.17a1 не добавляет retries, pagination, polling или fallback provider.

## Влияние на существующие реализации

- `FakeHotelOfferProvider` возвращает `SearchCompleted` с тем же
  детерминированным набором offers;
- `RealHotelOfferProviderAdapter` до Stage 9.17c остается неисполняющим
  skeleton и не должен заявлять успешный пустой search;
- `CreateHotelSearchUseCase` ранжирует и сохраняет offers только внутри
  `SearchCompleted`;
- strict hotel-search handoff и confirmed-search flow используют одинаковую
  typed search boundary;
- Ktor routes остаются тонкими и не вызывают infrastructure напрямую.

## Что не входит в Stage 9.17a

- Kotlin production code и tests;
- transport-backed autocomplete resolver;
- `RealHotelOfferProviderAdapter` orchestration;
- production `HttpClient` или CIO dependency;
- configuration reconciliation;
- `Application.kt`, factory или route wiring;
- public API, OpenAPI, frontend и generated clients;
- live calls, retries, pagination, polling и JWT;
- durable storage, booking/payment и `bookHash`.

## Следующая последовательность

1. **Stage 9.17a1 — backend-only async/result contract migration.**
   Атомарно изменить существующие boundaries, use cases и tests; сохранить
   текущее поведение `FAKE`, не подключать real transport.
2. **Stage 9.17b — autocomplete resolver transport adapter.**
   Добавить отдельный request DTO с `input`, transport-backed resolver и
   `MockEngine` tests без runtime wiring.
3. **Stage 9.17c — opt-in REAL runtime wiring.**
   Разделить public/private configuration, определить lifecycle production
   `HttpClient`, подключить orchestrator и сохранить `FAKE` default.
4. **Stage 9.18 — integration closure.**
   Проверить regression и обработку failures без заявления production
   readiness.

## Verdict

`READY_FOR_STAGE_9_17A1_NOT_READY_FOR_REAL_RUNTIME_WIRING`.

Async и typed result direction согласованы. Фактический provider transport,
resolver и `REAL` runtime wiring остаются заблокированными до отдельных этапов.

## Связанные документы

- [Stage 9.17 — REAL runtime wiring readiness gate](stage-9-17-real-runtime-wiring-readiness-gate.md)
- [Stage 9.12 — search orchestration](stage-9-12-hotels-api-search-orchestration-without-runtime-wiring.md)
- [Backend layering rules](../architecture/backend-layering-rules.md)
- [Основной roadmap](../roadmap/roadmap.md)
