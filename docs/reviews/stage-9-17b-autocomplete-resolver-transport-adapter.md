# Stage 9.17b — transport adapter разрешения направления

## Роль документа

Этот документ является implementation review-артефактом Stage 9.17b. Он
фиксирует добавленный внутренний adapter для autocomplete и не заменяет
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md) как источник статуса и
последовательности этапов.

## Цель

Подключить существующую application-границу `HotelLocationResolverBoundary` к
публичному autocomplete endpoint через уже проверенный
`PublicHotelsApiHttpTransport`, не добавляя runtime wiring, live calls или
автоматический выбор направления.

## Что добавлено

Production-код:

- `PublicHotelsApiAutocompleteRequestDto` с единственным wire-полем `input`;
- `PublicHotelsApiLocationResolverAdapter`, реализующий
  `HotelLocationResolverBoundary`;
- вызов `POST /search-api/search/autocomplete` через внедренный
  `PublicHotelsApiHttpTransport`;
- декодирование ответа существующим строгим `HotelsApiJson`;
- преобразование только `payload.locations` через
  `HotelsApiAutocompleteLocationMapper`.

Tests:

- `PublicHotelsApiLocationResolverAdapterTest` на `MockEngine`;
- provider-derived sanitized autocomplete fixture используется как
  контрактный пример ответа.

## Контракт request и response

Adapter передает:

- JSON body `{"input":"<query>"}`;
- `X-User-Language` только при наличии `RU` или `EN`;
- относительный путь публичного API;
- без `Authorization`.

Старый `HotelsApiAutocompleteRequestDto` с полем `query` не используется и не
подменяет наблюдаемый публичный контракт.

Текущий `HotelsApiAutocompleteResponseDto` переиспользуется осознанно: его
структура подтверждена sanitized fixture Stage 9.14. Это решение относится
только к совместимой response shape и не объединяет request-контракты разных
endpoint.

## Политика выбора направления

Adapter возвращает все location candidates в исходном порядке:

- отсутствие `locations` либо пустой список дает пустой application result;
- один location candidate остается единственным кандидатом;
- несколько candidates сохраняются полностью;
- первый candidate автоматически не выбирается;
- `payload.hotels[].id` не преобразуется в `destinationId`.

Различение `LocationNotFound` и `LocationSelectionRequired` остается задачей
существующего orchestration layer.

## Ошибки

- HTTP, timeout и network failures сохраняют безопасную категорию
  `HotelProviderException`, сформированную transport;
- malformed или несовместимый JSON преобразуется в `INVALID_RESPONSE`;
- raw body, headers и provider exception text не включаются в новое сообщение
  ошибки;
- coroutine cancellation не перехватывается.

## Проверки

Targeted tests подтверждают:

- точный endpoint и поле `input`;
- отсутствие `Authorization`;
- условную передачу `X-User-Language`;
- сохранение всех location candidates;
- игнорирование hotel identifiers при location mapping;
- пустой результат без выдуманного candidate;
- безопасный invalid-response outcome;
- сохранение transport error category.

Полный backend test suite и `git diff --check` выполняются перед commit.

## Границы этапа

Не добавлены:

- изменения `Application.kt` и runtime composition;
- создание production `HttpClient`;
- подключение `RealHotelOfferProviderAdapter`;
- live calls;
- автоматический выбор location;
- public API, OpenAPI, frontend или generated clients;
- pagination, polling, retries и web search orchestration;
- auth, secrets, durable storage и booking flow.

`FAKE` остается provider по умолчанию.

## Риски и ограничения

- публичный endpoint подтвержден эмпирически, но его официальный
  server-to-server статус и долговременная стабильность не подтверждены;
- query validation остается вне transport adapter;
- adapter пока не участвует в runtime lifecycle;
- production engine и корректное закрытие `HttpClient` должны быть решены
  отдельным этапом.

## Следующий этап

Stage 9.17c — узкое opt-in `REAL` runtime wiring:

- public-only configuration для активируемого flow;
- production `HttpClient` lifecycle;
- composition resolver → search orchestrator → REAL provider adapter;
- `FAKE` как неизменный default;
- targeted runtime tests без расширения public contract.

## Verdict

`READY_FOR_STAGE_9_17C_NOT_RUNTIME_WIRED`.

Transport-backed autocomplete resolver реализован и проверен только через
`MockEngine`. Реальные provider calls и runtime wiring отсутствуют.

## Связанные документы

- [Stage 9.17a1 — backend async/result contract migration](stage-9-17a1-backend-async-result-contract-migration.md)
- [Stage 9.14 — sanitized provider fixture verification](stage-9-14-sanitized-provider-fixture-contract-verification.md)
- [Stage 9.10 — autocomplete/location boundary](stage-9-10-autocomplete-location-resolution-contract-boundary.md)
- [Основной roadmap](../roadmap/roadmap.md)
