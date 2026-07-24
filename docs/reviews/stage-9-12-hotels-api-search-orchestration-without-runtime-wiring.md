# Stage 9.12 — orchestration поиска Hotels API без runtime wiring

**Роль:** review artifact завершенного internal orchestration этапа. Документ
фиксирует композицию location resolution, одного search request и domain
mapping без подключения к `RealHotelOfferProviderAdapter` или runtime factory.

## Цель и исходная точка

После Stage 9.11c отдельно существовали:

- `HotelLocationResolverBoundary`;
- `PublicHotelsApiHttpTransport`;
- request/response DTO и mapper-ы Hotels API;
- provider-neutral `HotelOffer`;
- синхронный `HotelOfferProviderBoundary` и отключенный
  `RealHotelOfferProviderAdapter`.

Цель Stage 9.12 — проверить их безопасную последовательную композицию, не
активируя REAL mode и не выполняя live calls.

## Реализация

Добавлен internal `HotelsApiSearchOrchestrator` со `suspend`-методом `search`.
Он:

1. передает destination и optional language в `HotelLocationResolverBoundary`;
2. останавливается, если candidates нет;
3. требует явного выбора, если candidates несколько, и не берет первый;
4. при единственном candidate вызывает `HotelsApiSearchRequestMapper`;
5. сериализует request и выполняет один
   `POST /api/v1/hotels/search` через существующий transport;
6. декодирует response и вызывает `HotelsApiSearchResponseMapper`;
7. возвращает typed internal outcome.

## Outcomes и ошибки

Поддерживаются:

- `Success(location, offers)`;
- `LocationNotFound`;
- `LocationSelectionRequired(candidates)`;
- `RequestRejected(error)`;
- `ResponseRejected(errors)`.

Невалидный JSON преобразуется в безопасный `HotelProviderException` с
категорией `INVALID_RESPONSE`. Raw response body и parser cause наружу не
передаются. HTTP status/network errors остаются ответственностью существующего
transport и его provider error taxonomy.

## Ограничение одной страницы

Stage 9.12 всегда выполняет максимум один search call. `nextOffset` и
`isLoadingCompleted=false` не запускают дополнительный запрос. Это сохраняет
pagination orchestration для отдельного Stage 9.13.

## Adapter и runtime boundary

`HotelOfferProviderBoundary.search` сейчас синхронный, а resolver и transport —
асинхронные. Поэтому orchestrator намеренно не подключен к
`RealHotelOfferProviderAdapter`, `HotelOfferProviderFactory` или
`Application.kt`. Искусственная блокировка coroutine или изменение действующей
domain boundary в этом этапе не добавлялись.

Фактические следствия:

- `FAKE` остается default;
- `REAL` skeleton по-прежнему не выполняет I/O;
- routes и runtime behavior не меняются;
- решение sync/suspend seam требуется до Stage 9.17 runtime wiring.

## Тесты

Все HTTP-проверки используют `MockEngine`. Targeted tests подтверждают:

- query/language → resolver;
- один candidate → точный path, date-only JSON, child ages и один HTTP call;
- отсутствие `Authorization`;
- ноль или несколько candidates → отсутствие search call;
- request mapping rejection → отсутствие search call;
- response mapping rejection → typed result без pagination retry;
- malformed JSON → безопасный `INVALID_RESPONSE`.

## Документация и статусы

- основной roadmap обновлен по фактическому завершению Stage 9.12;
- `docs/ROADMAP.md` остается навигационным и не дублирует текущий статус;
- review index дополнен недостающими role entries Stage 9.8a–9.12;
- исторические review artifacts не переписывались.

## Что не входит в этап

- live API calls и fixture capture;
- autocomplete transport implementation;
- pagination, polling, retry или `etag` orchestration;
- `RealHotelOfferProviderAdapter` и runtime wiring;
- routes, public API, OpenAPI, frontend или generated clients;
- auth/JWT, secrets, storage, booking/payment и `bookHash`.

## Verdict

Stage 9.12 завершен как изолированная orchestration, проверенная через
`MockEngine`. Следующий roadmap-кандидат — Stage 9.13: bounded pagination поверх
этой orchestration, по отдельной явной задаче и без runtime wiring.

## Связанные документы

- [Stage 9.11c — search domain mapping](stage-9-11c-hotels-api-search-domain-mapping.md)
- [Stage 9.9 — public HTTP transport](stage-9-9-public-anonymous-hotels-api-http-transport.md)
- [Stage 9.10 — location resolution](stage-9-10-autocomplete-location-resolution-contract-boundary.md)
- [Основной roadmap](../roadmap/roadmap.md)
