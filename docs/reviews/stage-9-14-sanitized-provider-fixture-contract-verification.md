# Stage 9.14 — проверка контрактов по обезличенным provider fixtures

**Роль:** review artifact завершенного этапа проверки фактических ответов
Hotels API. Документ не заменяет официальный provider contract и не является
заявлением о готовности интеграции к промышленной эксплуатации.

## Цель и исходная точка

Stage 9.10–9.13 добавили autocomplete/search DTO, mapper-ы и ограниченную
orchestration с одним search call, но проверяли их только на синтетических
данных и `MockEngine`.

Цель Stage 9.14 — сверить текущие DTO и mapping policy с обезличенными
provider-derived ответами без изменения production code и без runtime wiring.

## Полученные наблюдения

Каждый запрос выполнен ровно один раз из отдельного анонимного browser context
без `Authorization`, cookies и сохраненного session state.

| Наблюдение | HTTP status | Структура |
|---|---:|---|
| `POST /search-api/search/autocomplete` | 200 | `payload` object, 5 locations, 1 hotel |
| `POST /api/v1/hotels/search` | 200 | `payload` object, 20 hotels |
| `POST /api/v1/hotels/search`, две guest groups | 400 | `error.code=invalid_rooms_count`, `details` object |

Autocomplete location для запроса «Казань» выбран только по явному совпадению
названия и `type.code=city`. Первый элемент результата автоматически не
использовался.

## Fixtures и обезличивание

В test resources добавлены:

- `autocomplete-success.json`;
- `search-success.json`;
- `search-invalid-rooms.json`;
- `fixture-manifest.json`.

В fixtures заменены location/hotel identifiers, названия, signatures,
адреса, координаты и image URLs. JSON-типы, nesting, порядок массивов,
nullability, enum-like strings, currency/payment values, pagination metadata
и неизвестные provider fields сохранены. Headers, cookies, tokens,
session/device/tracing metadata, внутренние hosts/IP и исходные идентификаторы
в репозиторий не переносились.

Raw responses хранились только во временном каталоге и удалены после
успешного переноса обезличенных файлов и contract verification.

## Результат сверки контрактов

### Autocomplete

- response десериализуется в `HotelsApiAutocompleteResponseDto`;
- location id остается числом, hotel id — строкой;
- `type` подтвержден как object с `code` и `name`;
- `HotelsApiAutocompleteLocationMapper` создает candidates только из
  `payload.locations` и не превращает hotel id в `destinationId`.

Наблюдаемый request использует поле `input`, тогда как текущий internal
`HotelsApiAutocompleteRequestDto` использует `query`. Это известное
несовпадение не исправлялось в Stage 9.14 и должно быть согласовано отдельным
этапом до autocomplete transport wiring.

### Hotel search

- response десериализуется в `HotelsApiSearchResponseDto` без адаптации
  production DTO;
- неизвестные provider fields сохраняются в fixture, но корректно игнорируются
  текущей JSON policy;
- `HotelsApiSearchResponseMapper` сохраняет opaque `hotelId` как
  `providerReference`;
- `shownPrice.amount` и currency переносятся без пересчета;
- guest rating берется из `review`, а `starRating` не используется как
  fallback;
- amenities остаются `null`, `LIMITED` не создается;
- `isLoadingCompleted=false` и `nextOffset` читаются из DTO, но не запускают
  pagination или повторный вызов.

### Provider error

Ошибка нескольких guest groups подтверждена как JSON object с точным
`error.code=invalid_rooms_count` и object-полем `details`. Она проверяется как
raw `JsonObject`, без создания нового production error DTO.

## Тесты

Добавлен `HotelsApiProviderFixtureContractTest`, который проверяет:

- autocomplete identifier types, type object и location-only mapping;
- search DTO deserialization и действующую response mapping policy;
- отсутствие выдуманных rating/amenities/`LIMITED`;
- чтение pagination metadata без orchestration behavior;
- точную структуру `invalid_rooms_count`;
- признак обезличивания и состав fixture manifest.

Существующие synthetic tests сохранены.

## Границы

- production DTO, mapper-ы, transport и configuration не изменены;
- `RealHotelOfferProviderAdapter`, provider factory, routes и runtime wiring не
  изменены;
- public API, OpenAPI Travel Assistant, frontend и generated clients не
  изменены;
- polling, pagination, retries, booking/payment и `bookHash` не добавлены;
- Stage 9.14 не подтверждает официальный server-to-server статус, SLA, rate
  limits или долговременную стабильность публичных endpoints.

## Остаточные риски

- autocomplete request `input`/`query` требует отдельного reconciliation;
- включение taxes/fees в `shownPrice` остается неизвестным;
- policy для `LIMITED` отсутствует и не должна быть выведена из fixture;
- анонимная browser-origin доступность не равна готовности проектного
  transport/runtime flow.

## Verdict

Stage 9.14 завершен: provider-derived fixtures совместимы с текущими
autocomplete/search response DTO и search mapper policy, contract drift по
моделируемым response paths не обнаружен. Следующий разрешенный этап —
Stage 9.15, sandbox readiness gate без автоматической активации `REAL`.

## Связанные документы

- [Stage 9.13 — single-page candidate window](stage-9-13-single-page-hotel-candidate-window.md)
- [Stage 9.12 — search orchestration](stage-9-12-hotels-api-search-orchestration-without-runtime-wiring.md)
- [Stage 9.11c — search domain mapping](stage-9-11c-hotels-api-search-domain-mapping.md)
- [Stage 9.10 — autocomplete boundary](stage-9-10-autocomplete-location-resolution-contract-boundary.md)
- [Основной roadmap](../roadmap/roadmap.md)
