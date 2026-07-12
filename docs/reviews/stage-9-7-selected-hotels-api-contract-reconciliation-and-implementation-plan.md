# Stage 9.7 — Hotels API Contract Reconciliation and Implementation Plan

## 1. Цель и scope

Stage 9.7 — review/design-only intake предоставленных контрактов внутреннего
`HotelsApi`. Цель этапа — выбрать контрактный минимум для hotel-only MVP,
сопоставить его с текущими `HotelSearchCriteria` и `HotelOffer`, определить
архитектурные и эксплуатационные guardrails и разложить реализацию на отдельные
этапы.

В Stage 9.7 не изменялись production code, tests, runtime wiring, API/OpenAPI
Travel Assistant, frontend или generated clients. HTTP-запросы не выполнялись,
credentials и конфигурация не добавлялись.

## 2. OpenAPI inventory

Контракты обнаружены не в Git worktree, а среди предоставленных локальных
материалов в `Downloads`. Все три файла являются валидными JSON-документами
OpenAPI 3.0.4, хотя два имеют расширение `.txt`.

| Локальный файл | `info.version` | Paths | Schemas | SHA-256 |
|---|---:|---:|---:|---|
| `Downloads/message.txt` (local input) | 1.0 | 114 | 505 | `0a0ec1e8...b8364` |
| `Downloads/message (1).txt` (local input) | 2.0 | 18 | 248 | `7326ea84...34dba` |
| `Downloads/message (2).txt` (local input) | 3.0 | 5 | 152 | `6c9cd4dd...f201` |

В репозитории также есть собственный OpenAPI Travel Assistant
`docs/architecture/stage-6/openapi-draft.yaml`; он не является контрактом
Hotels API. Других предоставленных Hotels API Swagger/OpenAPI файлов не найдено.

### 2.1 Версионная структура

- v1 — основной и наиболее полный API: search, hotel details, booking, payment,
  cancellation, reviews, SEO, URL generation и internal endpoints.
- v2 — обновленные rates, filters, booking/card, SEO и URL endpoints.
- v3 — точечные новые версии rates, book-hash details, booking card, SEO search и
  URL generation.
- Версия API не едина для всего будущего pipeline: актуальные capabilities
  распределены между v1, v2 и v3.

### 2.2 Search/discovery endpoints

| Endpoint | Назначение | MVP verdict |
|---|---|---|
| `POST /api/v1/hotels/search` | Основная list-выдача отелей по destination, датам и occupancy | MVP |
| `GET /api/v1/hotels/{hotelId}` | Статические details, facilities, images, rules | Later, on-demand |
| `GET /api/v1/hotels/search-filters` | v1 filter definitions | Superseded для нового кода v2 |
| `POST /api/v1/hotels/search-filters-availability` | Доступность filters для конкретного запроса | Later |
| `POST /api/v1/hotels/map/search` | Поиск для карты | Future |
| `POST /api/v1/hotels/map/hotels` | List-выдача для карты | Future |
| `GET /api/v2/hotels/search-filters` | Актуальные filter definitions | Later |
| `POST /api/v2/hotels/{hotelId}/rates` | Rooms/rates для выбранного отеля | Later |
| `POST /api/v3/hotels/{hotelId}/rates` | Новая версия rooms/rates | Later, preferred over v2 |
| `POST /api/v1|v2|v3/seo/search` | SEO-oriented hotel list by location | Не использовать как primary MVP search |
| `POST /api/v1|v2|v3/hotels/urls/search` | Deeplink/search URL | Later; v3 preferred |

`POST /api/v1/hotels/autocomplete` отсутствует во всех трех контрактах. Также не
обнаружен другой endpoint с `autocomplete` в path, operation summary или schema
name. `GET /api/v1/seo/location-by-slug` и SEO location endpoints не являются
доказанной заменой fuzzy autocomplete и не должны использоваться как такая
замена без решения владельца API.

### 2.3 Booking/payment/cancellation endpoints

Booking surface значительно шире hotel-only search MVP:

- v1: booking list/card/voucher/cancel, reservation lookup, create/status tasks,
  LifeStyle/prepay flows, BNPL offer, payment setup, promocode и extra services;
- v2: booking card/cancel/voucher и internal booking operations;
- v3: booking card;
- `GET /api/v2/rates/{bookHash}` и `GET /api/v3/rates/{bookHash}` дают checkout-
  oriented rate details;
- отдельные cashback, tranche, payment и support/internal endpoints обслуживают
  transaction lifecycle.

Все create booking, payment, cancellation и order-management endpoints —
`Future`: они не входят в bounded hotel-only MVP текущего проекта.

| Версия | Public booking/transaction paths | Решение |
|---|---|---|
| v1 | `POST .../bookings/booking_list`, `GET .../bookings/{orderId}`, `GET .../voucher/{orderId}`, `POST .../cancel`, `GET .../getReservation` | Future |
| v1 | `POST .../bookings/tasks/create`, `GET .../tasks/{taskId}/status`, LifeStyle/prepay create/status | Future |
| v1 | EVO booking card/BNPL, `POST .../shevo/{orderId}/payment/setup`, rate upgrade/promocode/extraServices | Future |
| v2 | `GET .../bookings/{orderId}`, `POST .../bookings/cancel`; voucher path declared without HTTP operation | Future; spec defect to clarify |
| v3 | `GET /api/v3/hotels/bookings/{orderId}` | Future |

`internal_api/**`, internal-bookings, maintenance, callbacks и background jobs не
являются candidate endpoints Travel Assistant и исключены из будущего adapter.

## 3. Selected API versions

| Capability | Selected contract | Решение |
|---|---|---|
| Location resolution | Не выбран | Blocking contract gap: autocomplete отсутствует |
| Hotel search | `POST /api/v1/hotels/search` | Единственный основной Hotels list search |
| Filters | `GET /api/v2/hotels/search-filters` | Latest dedicated filter contract; не нужен первому MVP slice |
| Hotel details | `GET /api/v1/hotels/{hotelId}` | Единственная detail version; только on-demand enrichment |
| Rates | `POST /api/v3/hotels/{hotelId}/rates` | v3 богаче v2; отложить до отдельного rates slice |
| Rate by hash | `GET /api/v3/rates/{bookHash}` | Latest contract; booking-adjacent, Future |
| Deeplink | `POST /api/v3/hotels/urls/search` | Упрощенный response `payload.url`; Later |
| Booking/payment/cancel | Не выбран | Future, вне MVP |

v2 rates и book-hash endpoints проанализированы как обязательный минимум задачи,
но для будущего нового кода предпочтительна v3 при подтвержденной доступности в
целевой среде. Смешивание версий должно оставаться скрытым за adapter/client
boundary.

## 4. Endpoint contract matrix

| Endpoint | Request | Response | Security по OpenAPI | Будущий pipeline |
|---|---|---|---|---|
| `POST /api/v1/hotels/autocomplete` | Отсутствует | Отсутствует | Неизвестно | Нужен до search; owner input |
| `POST /api/v1/hotels/search` | `destinationId:int`, date-time dates, `guests[] { adultsCount, childrenAge[] }`, optional `offset`, `limit`, filters, sort; optional `X-User-Language` | Envelope `payload`: hotels/counts/filters, `nextOffset?`, `isLoadingCompleted`; hotel includes `hotelId`, name, location, images, min rate, review, cashback | Наследует root `SiamBearer` | Primary MVP discovery |
| `GET /api/v2/hotels/search-filters` | Без body | Envelope с `popularFilters?` и polymorphic `filters[]` | Наследует root `SiamBearer` | Later filter UI/policy |
| `POST /api/v2/hotels/urls/search` | Body: dates, `destId`, `destType`, adults, child ages; required query `AppName`, `AppVersion`, `Platform`; optional session identifiers | `status` + payload array `{partner,url,link}` | Наследует root `SiamBearer` | Legacy deeplink; не выбирать для нового кода |
| `POST /api/v3/hotels/urls/search` | Body: dates, `destId`, `destType`, adults, child ages; optional travel/session/platform headers | Envelope `payload { url }` | Наследует root `SiamBearer` | Preferred Later deeplink |
| `GET /api/v1/hotels/{hotelId}` | Path `hotelId`; optional language/device headers | Detailed hotel payload: location, images, facilities, rules, payment methods | `security: []` явно снимает root auth | Later on-demand details |
| `POST /api/v2/hotels/{hotelId}/rates` | Path `hotelId`; dates, optional guests/filters; optional language header | Rooms plus matching/non-matching rates; `bookHash`, shown/payment prices, cancellation, meal, cashback | Наследует root `SiamBearer` | Later selected-hotel rates |
| `GET /api/v2/rates/{bookHash}` | Path `bookHash`; optional language/role/platform/version headers | Checkout-oriented hotel/rooms/payment/cashback details | `security: [{}]`; anonymous alternative по OpenAPI | Future booking preparation |
| `POST /api/v3/hotels/{hotelId}/rates` | v2 request shape plus optional role/platform/version headers | v2 fields plus search/filter/badge/discount additions | Наследует root `SiamBearer` | Preferred Later rates |
| `GET /api/v3/rates/{bookHash}` | Path + optional headers | Expanded checkout-oriented payload | Наследует root `SiamBearer` | Future |

У всех JSON success responses, кроме legacy v1/v2 URL wrappers, используется
`payload` envelope. Provider error body — отдельный generic/error envelope; raw
errors не должны выходить из adapter.

## 5. Capability matrix

| Capability | API coverage | Priority | Комментарий |
|---|---|---|---|
| Autocomplete/location resolution | Не предоставлено | MVP | Blocking для `destination -> destinationId` |
| Hotel search | v1 list search | MVP | Основной real-provider slice |
| Filters | v2 filters | Later | Не требуется первому search call |
| Details | v1 hotel details | Later | Избегать N+1 в первой реализации |
| Rates | v3 hotel rates | Later | Нужен для room/rate precision, не для первого list MVP |
| Deeplink | v3 URL search | Later | Не добавлять до стабильного search mapping |
| Booking | v1 task creation и order lifecycle | Future | Исключено продуктовым scope |
| Payment | v1 payment/BNPL/tranche flows | Future | Исключено продуктовым scope |
| Cancellation | v1/v2 cancellation | Future | Исключено продуктовым scope |

## 6. Contract reconciliation

### 6.1 `HotelSearchCriteria`

| Current field/need | Hotels API | Classification | Решение |
|---|---|---|---|
| `destination: String` | Search требует numeric `destinationId` | Product decision + orchestration | Нужен verified location resolution; string нельзя парсить как id |
| Autocomplete | Contract отсутствует | Owner input | Получить отдельный spec/path и sample payload |
| Dates `LocalDate` | v1 search использует `date-time` | Adapter mapping only | Отправлять согласованную date-time normalization; timezone policy подтвердить |
| `guests.adults` | Adults находятся внутри room-like `guests[]` | Product/domain decision | Нужна occupancy distribution, не только aggregate |
| `guests.children` | API требует ages, а не count | Domain change или MVP restriction | Запросить ages; не изобретать возраст |
| `rooms: Int?` | Room count задается длиной `guests[]` | Domain/product decision | Нужен per-room occupancy либо явное adults-only single-room ограничение |
| Filters/sort | Optional API fields | Deferred | Не добавлять в первый MVP call |

До решения children/rooms безопасная реализация может поддерживать только явно
подтвержденный adults-only single-room case. Молчаливое распределение гостей по
комнатам или фиктивные child ages запрещены.

### 6.2 `HotelOffer`

| Current/provider field | Hotels API source | Classification | Решение |
|---|---|---|---|
| `id` | `hotelId` | Adapter mapping only | Stable internal offer id на основе source + hotelId |
| `providerReference` | Search дает `hotelId`; rates дает `bookHash` | Product decision | Для list MVP использовать opaque hotelId; не считать его booking offer ref |
| `hotelName` | `hotelName` | Adapter mapping only | Direct |
| `city`, `country` | `areaLocation.destinationName`, `countryName` | Adapter mapping only | Provider area может быть не city; naming semantics сохранить внутренне |
| `totalPrice`, `currency` | `rateForHotelsFeed.shownPrice` | Adapter mapping + contract clarification | Amount — весь период; tax/fee inclusion неизвестен |
| `paymentPlace` | `now`, `hotel`, `delayed_payment` | Deferred | Не помещается в current domain; Later provider facts/rates model |
| `mealType`, `mealName` | Search/rates rate fields | Deferred | Не изобретать amenity; Later explicit fact |
| Cancellation | `freeCancellationUntil`, rates policy | Deferred | List summary возможен, но current domain не переносит policy |
| Cashback | Search/rates cashback | Deferred | Loyalty-specific; не вычитать автоматически из shown price |
| `rating`, `reviewCount` | Nullable `review` object | Domain change | Current mandatory numeric fields не выражают unknown; нужны nullable/typed unknown semantics |
| Images | Search `images[]`, details/rates images | Deferred | Current `HotelOffer`/public response не поддерживает images |
| `amenities` | Нет в list search; есть в details/rates | Deferred или domain change | Не делать N+1; empty list должен означать unavailable, не «удобств нет» |
| `availability` | `availableRoomsCount` | Adapter mapping + product rule | `>0` = available; threshold для `LIMITED` требует решения |
| `freshness` | Timestamp/freshness отсутствует | Adapter mapping only | `UNKNOWN`, не `FRESH` |
| Provider ids | `hotelId`, `roomId`, `bookHash`, `searchId` | Adapter/internal only | Не раскрывать raw payload; наружу только existing opaque reference |

Stage 9.2 zero-domain-change assumption был сделан без выбранного provider
contract. Реальный contract выявил минимум один domain blocker: nullable review
data. Изменение domain/public contract не выполняется в Stage 9.7; оно требует
отдельного решения до Stage 9.11 mapping.

## 7. Pagination policy

v1 search принимает optional `offset`/`limit` и возвращает `nextOffset?` и
`isLoadingCompleted`. Контракт не задает max/default limit, rate limit, polling
interval или гарантию монотонности offset.

Безопасная MVP policy:

1. Первый запрос: `offset = 0`, `limit = 20` только после подтверждения допустимого
   limit владельцем API.
2. Stage 9.12 может ограничиться одной страницей и явно считать результат partial.
3. Stage 9.13 добавляет bounded pagination: максимум 3 страницы/60 уникальных
   hotels и общий time budget.
4. Остановиться при `isLoadingCompleted = true`, `nextOffset = null`, пустой
   странице, повторном/неувеличивающемся offset или достижении budget.
5. Deduplicate по `hotelId`; first stable occurrence wins.
6. Не интерпретировать `isLoadingCompleted = false` как ошибку и не запускать
   бесконечный polling.

До получения semantics от owner нельзя утверждать, что `nextOffset` означает
обычную pagination, а не продолжение асинхронной загрузки.

## 8. Authentication and configuration

Во всех версиях объявлены:

- `Bearer`: HTTP bearer JWT, описанный как user/session SSO token;
- `SiamBearer`: OAuth2 client credentials;
- root security: `SiamBearer` со scope
  `hotels-api:similar-hotels-feed-api`.

Search, filters, rates и URL operations без локального `security` наследуют
root `SiamBearer`. Однако scope назван для similar-hotels feed и выглядит
подозрительно узким. До подтверждения owner нельзя считать его корректным scope
для search/rates. `GET /api/v1/hotels/{hotelId}` явно публичен по spec;
`GET /api/v2/rates/{bookHash}` содержит empty security requirement и формально
допускает anonymous access. Эти различия также требуют подтверждения.

Предлагаемые config keys для Stage 9.8 (значения и secrets не добавлять):

| Config/env | Secret | Назначение |
|---|---|---|
| `HOTEL_PROVIDER_MODE` | No | Existing `FAKE`/`REAL`; default `FAKE` |
| `HOTELS_API_BASE_URL` | No | Environment-specific base URL; отсутствует в spec |
| `HOTELS_API_TOKEN_URL` | No | OAuth token endpoint override |
| `HOTELS_API_CLIENT_ID` | Yes | OAuth client id |
| `HOTELS_API_CLIENT_SECRET` | Yes | OAuth client secret |
| `HOTELS_API_SCOPE` | No | Owner-confirmed search scope |
| `HOTELS_API_CONNECT_TIMEOUT_MS` | No | Bounded connect timeout |
| `HOTELS_API_REQUEST_TIMEOUT_MS` | No | Bounded request timeout |
| `HOTELS_API_USER_LANGUAGE` | No | Optional `X-User-Language` |
| `HOTELS_API_SOURCE_PLATFORM` | No | Optional v3 `x-source-platform` |
| `HOTELS_API_APP_VERSION` | No | Optional v3 `x-app-version` |

Transport headers: `Authorization: Bearer <access-token>`,
`Content-Type: application/json`, optional language/platform/version headers.
`x-travel-session-id` нельзя отправлять без отдельной privacy/session decision.
Secrets нельзя логировать, класть в fixtures, commit или error payload.

REAL mode должен fail closed при неполной config. Silent fallback REAL → FAKE
скрывает misconfiguration и недопустим. FAKE остается default.

## 9. Architecture decisions

### 9.1 Existing boundary

`HotelOfferProviderBoundary.search(HotelSearchCriteria): List<HotelOffer>` можно
сохранить как provider-independent entry point. Routes, Stage 7 handoff и Stage 8
confirmation lifecycle менять не требуется.

### 9.2 Location resolution

Отдельный internal `HotelLocationResolverBoundary` оправдан, потому что
user-facing destination и provider `destinationId` — разные concepts. Boundary
должен быть внутренним для provider integration и возвращать typed resolved
location, а не public DTO. Его точный contract нельзя фиксировать до получения
autocomplete spec.

### 9.3 Orchestration

Нужен отдельный infrastructure/application integration orchestrator:

```text
RealHotelOfferProviderAdapter
  -> HotelLocationResolverBoundary
  -> HotelsApi search client
  -> bounded pagination
  -> Hotels API DTO mapper
  -> List<HotelOffer>
```

HTTP/auth/token handling принадлежит transport infrastructure; normalization и
provider error translation — adapter mapping. Ktor routes не должны знать о
`destinationId`, OAuth, versions или provider headers.

### 9.4 Storage

Дополнительный provider storage для MVP не нужен. Location cache, response cache
и durable token/search storage отложены. Короткоживущий in-memory OAuth token
cache может быть частью transport позже, но это не domain/provider result store.

## 10. Remaining unknowns и required owner inputs

До HTTP/search реализации нужны ответы владельца API:

1. Autocomplete/location endpoint contract, включая request, response,
   destination type/id и examples.
2. QA/sandbox base URL и network/VPN requirements.
3. Корректные OAuth token URL, scope/audience и service identity для search.
4. Подтверждение root security inheritance и anonymous overrides.
5. Поддерживаемая API version matrix в целевой среде.
6. Допустимые values для `X-User-Language`, `x-source-platform`,
   `x-app-version`; нужен ли `x-user-role`.
7. Max/default `limit`, rate limits и точная семантика `nextOffset` /
   `isLoadingCompleted`.
8. Child-age bounds и правила распределения adults/children по rooms.
9. Rating scale и semantics отсутствующего review.
10. Включает ли `shownPrice` taxes/fees и какая цена должна ранжироваться.
11. Semantics `paymentPlace`, cashback и cancellation для display.
12. Sanitized request/response/error fixtures и expected non-200 examples.
13. TTL/stability `hotelId`, `bookHash`, deeplink и provider search ids.
14. Разрешение на первый QA call и безопасный test destination/date set.

Дополнительные contract-quality unknowns: во всех specs `servers[0].url` равен
relative `/`, поэтому environment base URL не определен; v2 содержит path
`/api/v2/hotels/bookings/voucher/{orderId}` без HTTP operation.

## 11. Detailed implementation roadmap

| Stage | Scope | Preconditions / exit gate |
|---|---|---|
| 9.8 | Configuration skeleton | Typed config, validation/redaction, FAKE default, REAL fail-closed; no HTTP |
| 9.9 | HTTP transport + OAuth boundary | Mock transport tests only; no live call; owner-confirmed auth fields |
| 9.10 | Autocomplete/location resolution | Только после предоставления missing contract; DTO/client/resolver tests |
| 9.11 | Search DTO + mapping reconciliation | Resolve children/rooms/review unknown decisions; fixture-driven mapper |
| 9.12 | Real adapter orchestration | Resolver → one-page v1 search → mapper; no runtime wiring |
| 9.13 | Bounded pagination | Offset guards, dedupe, page/offer/time budgets |
| 9.14 | Fixture contract verification | Sanitized v1/v2/v3 fixtures, schema drift/error mapping checks |
| 9.15 | Sandbox readiness gate | Docs/review-only: config, network, auth, logging, data handling checklist |
| 9.16 | First QA call | Separate explicit opt-in; one controlled non-production call, no secrets in output |
| 9.17 | Runtime wiring | REAL opt-in only; FAKE default; Stage 7/8 regression tests |
| 9.18 | Integration closure | Failure/pagination/compatibility verification; no production-readiness claim |

Stages 9.9–9.12 остаются blocked, пока owner inputs 1–8 не закрыты. Booking,
payment, cancellation, durable cache/storage и frontend contract changes не
включены в этот roadmap slice.

## 12. Risks

- Missing autocomplete делает destination mapping недетерминированным.
- Aggregate guests model может отправить неверную occupancy и цену.
- Nullable provider facts конфликтуют с mandatory domain numbers.
- Root OAuth scope может быть generated-spec artifact, а не рабочим permission.
- N+1 details/rates calls ухудшат latency и rate-limit profile.
- Unbounded `nextOffset` handling может создать цикл и нагрузку.
- `bookHash` и deeplink — booking-adjacent identifiers; нельзя раскрывать или
  сохранять без отдельного lifecycle decision.

## 13. Readiness verdict

**Conditionally ready только для Stage 9.8 configuration skeleton.**

Contract corpus достаточен, чтобы выбрать v1 hotel search и спроектировать
transport/config boundaries. Он недостаточен для безопасной autocomplete/search
реализации: отсутствует location-resolution contract, не определены occupancy и
nullable review mappings, base URL и рабочая auth policy.

Real provider, network calls, runtime wiring и production readiness не заявлены.

## 14. Готовый prompt для Stage 9.8

```text
# Stage 9.8 — Hotels API Configuration Skeleton

Мы продолжаем проект travel-assistant.

Текущая точка:
- Stage 8 завершен.
- Stage 9.1–9.7 завершены.
- Stage 9.7 выбрал внутренний HotelsApi и POST /api/v1/hotels/search как
  primary MVP search contract.
- Hotels API OpenAPI 1.0/2.0/3.0 проанализированы.
- HTTP transport, DTO, mapper, autocomplete и real calls не реализованы.
- Fake provider остается default.

Задача: выполнить Stage 9.8 как backend-only configuration skeleton без HTTP.

Перед изменениями:
1. Проверить git status/branch/HEAD; при dirty tree остановиться.
2. Прочитать AGENTS.md, roadmap, backend rules, Stage 9.6 и Stage 9.7 reports.
3. Прочитать HotelProviderConfig, HotelProviderMode, HotelOfferProviderFactory,
   Application.kt и current config tests.

Implementation:
1. Расширить internal HotelProviderConfig typed settings для будущего Hotels API:
   base URL, token URL, client id, client secret, scope, connect/request timeout,
   optional user language/source platform/app version.
2. Использовать env names из Stage 9.7:
   HOTEL_PROVIDER_MODE, HOTELS_API_BASE_URL, HOTELS_API_TOKEN_URL,
   HOTELS_API_CLIENT_ID, HOTELS_API_CLIENT_SECRET, HOTELS_API_SCOPE,
   HOTELS_API_CONNECT_TIMEOUT_MS, HOTELS_API_REQUEST_TIMEOUT_MS,
   HOTELS_API_USER_LANGUAGE, HOTELS_API_SOURCE_PLATFORM,
   HOTELS_API_APP_VERSION.
3. FAKE остается default и не требует Hotels API config.
4. REAL mode должен fail closed с typed internal configuration error, если
   обязательные values отсутствуют/invalid. Не делать silent REAL -> FAKE fallback.
5. Secrets не должны появляться в toString, logs, exception messages, fixtures,
   docs или test output. Добавить redacted secret wrapper/value handling.
6. Не фиксировать реальные URL/credentials/scope defaults. Token URL/scope должны
   приходить из config до owner confirmation.
7. Не менять provider boundary, routes, public API/OpenAPI или frontend.
8. Не создавать HttpClient, DTO, mapper, OAuth call или network call.
9. Не подключать RealHotelOfferProviderAdapter к runtime execution сверх
   существующего mode seam.

Tests:
- FAKE mode works with no Hotels API env/config.
- REAL mode accepts complete explicit test config.
- REAL mode rejects each missing required field.
- invalid URL/timeout rejected.
- secret redacted from toString/errors.
- no HTTP/client/provider call dependency.

Documentation:
- создать docs/reviews/stage-9-8-hotels-api-configuration-skeleton.md;
- обновить docs/reviews/README.md и docs/roadmap/roadmap.md;
- не менять root README/docs/ROADMAP unless explicitly required.

Checks:
- targeted config tests;
- full backend ./gradlew test with project JAVA_HOME;
- git diff --check;
- scope check: no routes/API/OpenAPI/frontend/generated/CI/network additions;
- secret leakage grep.

Границы:
- no real credentials;
- no HTTP/network;
- no autocomplete/search DTO or mapper;
- no production readiness claim;
- no Stage 9.9 work.

Финальный отчет на русском: files, config model/env names, validation/redaction
behavior, tests/checks, scope confirmations, remaining owner inputs и Stage 9.9
recommendation. Commit не создавать без отдельной команды.
```

## 15. Verdict

Stage 9.7 завершает contract intake и implementation planning. Safe next step —
Stage 9.8 configuration skeleton; actual transport и search остаются blocked до
закрытия owner inputs.
