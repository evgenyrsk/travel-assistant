# Stage 9.15 — gate готовности контролируемого Hotels API QA call

**Роль:** review artifact завершенного readiness gate. Документ фиксирует
условия безопасного вызова через проектный transport, но не разрешает live
call, runtime wiring или активацию `REAL`.

## Цель и текущая точка

Stage 9.14 подтвердил autocomplete/search response DTO и search mapping policy
на обезличенных provider-derived fixtures. При этом фактический проектный flow
остается изолированным:

- `PublicHotelsApiHttpTransport` проверен только через `MockEngine`;
- runtime network engine отсутствует;
- `HotelsApiSearchOrchestrator` не подключен к adapter или runtime;
- `RealHotelOfferProviderAdapter.search()` возвращает пустой список;
- `FAKE` остается provider по умолчанию.

Цель Stage 9.15 — определить минимальный безопасный путь к одному отдельно
разрешенному QA call через проектный transport.

## Transport readiness

| Проверка | Состояние | Вывод |
|---|---|---|
| Public base URL и timeout | `https://hotels.tbank.ru/`, 60 секунд | Готово |
| Relative path и host confinement | Проверены `MockEngine` tests | Готово |
| `Authorization` и произвольные headers | Transport их не добавляет | Готово |
| HTTP status/error taxonomy | 4xx/5xx/timeout покрыты | Готово |
| Реальный network engine | В зависимостях отсутствует | Требуется preflight |
| Redirect policy | Transport не управляет `followRedirects` внедренного `HttpClient` | Требуется preflight |
| Response `Content-Type` | `HotelsApiHttpResponse` хранит только status и body | Требуется preflight |
| Безопасный opt-in QA harness | Отсутствует | Требуется preflight |

Текущий transport пригоден как основа, но еще не образует воспроизводимую и
проверяемую live-call boundary.

## Выбор формы QA call

| Вариант | Provider calls | Вердикт |
|---|---:|---|
| Прямой search через `PublicHotelsApiHttpTransport` с явным `destinationId` | 1 | Выбран после Stage 9.15a |
| Autocomplete, затем search | Не менее 2 | Отложен: request `input`/`query` не согласован, resolver implementation отсутствует |
| Полный `HotelsApiSearchOrchestrator` | Не менее 1 | Отложен: требует resolver и не решает sync/suspend seam |
| `RealHotelOfferProviderAdapter`/runtime | Зависит от flow | Запрещен до Stage 9.17 |

Для Stage 9.16 выбран один прямой `POST /api/v1/hotels/search`. Он не должен
проходить через autocomplete, orchestrator, provider factory, adapter, routes
или runtime composition.

## Autocomplete blocker

- наблюдаемый public request использует `{"input":"..."}`;
- текущий `HotelsApiAutocompleteRequestDto` содержит `query`;
- transport-backed реализация `HotelLocationResolverBoundary` отсутствует;
- автоматический выбор первого candidate запрещен действующей policy.

Autocomplete не нужен для одного direct search QA call. Его reconciliation и
transport wiring остаются отдельной будущей задачей.

## Adapter и coroutine boundary

`HotelOfferProviderBoundary.search()` синхронный, а resolver, transport и
`HotelsApiSearchOrchestrator.search()` используют `suspend`. Поэтому:

- Stage 9.16 должен обходить `RealHotelOfferProviderAdapter`;
- `runBlocking` внутри production adapter не добавляется;
- sync/suspend решение обязательно до Stage 9.17;
- standalone suspend QA entry point допустим только как явно opt-in test/manual
  harness, не подключенный к application runtime.

## Configuration readiness

Public target может быть создан напрямую через
`HotelsApiTargetConfig.publicDefault()` без credentials.

`HotelProviderConfig.fromEnvironment()` в режиме `REAL` требует
`HOTELS_API_JWT_PRIVATE_KEY`, хотя выбранный QA call является публичным и
анонимным. Поэтому QA harness не должен:

- включать `HOTEL_PROVIDER_MODE=REAL`;
- читать private JWT configuration;
- создавать `HotelOfferProviderFactory`;
- использовать private target.

Для direct public call credentials, JWT и secrets не нужны.

## Response verification policy

Stage 9.16 должен:

1. получить HTTP status и `Content-Type` через безопасную transport boundary;
2. сохранить raw body только во временный каталог;
3. десериализовать body в `HotelsApiSearchResponseDto`;
4. выполнить `HotelsApiSearchResponseMapper`;
5. показать только status, media type, количество hotels/offers и безопасные
   категории результата;
6. не публиковать provider identifiers, names, addresses, images или raw body;
7. удалить raw data после анализа;
8. не добавлять новый fixture автоматически.

Stage 9.14 fixtures остаются эталоном структуры, но не основанием для
побайтового сравнения live response.

## Stop conditions

Один QA request выполняется без retry. Работа останавливается при:

- redirect;
- DNS, TLS или timeout error;
- HTTP 401/403, 404/405, 429 или 5xx;
- неожиданном `Content-Type`;
- invalid JSON или contract drift;
- пустом hotel result, если mapper verification невозможна.

Любой повторный request требует отдельного разрешения пользователя.

## Обязательные входные данные

До Stage 9.16 нужны:

- подтвержденный числовой test `destinationId`, передаваемый локально и не
  закрепляемый как публичный contract;
- точные будущие check-in/check-out dates;
- отдельное разрешение на один live call;
- согласие на test-scoped Ktor network engine и opt-in QA harness.

Рекомендуемый bounded request: одна guest group, двое взрослых, без детей,
`offset=0`, `limit=20`.

## Stage 9.15a — обязательный preflight

Перед live call нужен отдельный backend-only Stage 9.15a без network calls и
runtime wiring. Он должен подготовить:

- test-scoped Ktor network engine;
- явно отключенные redirects;
- безопасный `Content-Type` в transport response metadata;
- opt-in QA harness, который не запускается полным test suite без явного флага;
- `MockEngine` tests для redirect, media type и отсутствия повторных requests;
- подтверждение, что harness не использует `REAL`, JWT, adapter или runtime.

Точный состав Stage 9.15a должен оставаться минимальным. Production network
client factory и постоянный runtime engine в этот preflight не входят.

## Что не входит в Stage 9.15

- Kotlin/Gradle/tests changes;
- network engine или QA harness;
- live API calls;
- autocomplete reconciliation;
- adapter, factory, routes или runtime wiring;
- retry, polling, pagination или durable storage;
- JWT, secrets, private API, booking/payment и `bookHash`;
- public API, OpenAPI Travel Assistant, frontend или generated clients.

## Риски

- public endpoint не имеет подтвержденного официального server-to-server SLA;
- `shownPrice` taxes/fees semantics остается неизвестной;
- `LIMITED` policy отсутствует;
- test-only live-call tooling может случайно стать постоянным runtime seam,
  если Stage 9.15a не сохранит явную изоляцию.

## Readiness verdict

`READY_AFTER_STAGE_9_15A`.

Прямой search QA call выбран как безопасная форма Stage 9.16, но сейчас
заблокирован отсутствием network engine, explicit redirect policy, response
media-type metadata и opt-in harness. Следующий разрешенный шаг — Stage 9.15a;
Stage 9.16 не активирован.

## Связанные документы

- [Stage 9.14 — fixture verification](stage-9-14-sanitized-provider-fixture-contract-verification.md)
- [Stage 9.13 — single-page candidate window](stage-9-13-single-page-hotel-candidate-window.md)
- [Stage 9.12 — search orchestration](stage-9-12-hotels-api-search-orchestration-without-runtime-wiring.md)
- [Stage 9.9 — public HTTP transport](stage-9-9-public-anonymous-hotels-api-http-transport.md)
- [Основной roadmap](../roadmap/roadmap.md)
