# Stage 12.1 — проверка filter contract Hotels API

## Роль и цель

Это review/contract-verification отчет Stage 12.1. Актуальный статус задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md). Документ фиксирует
предоставленные OpenAPI-контракты и один отдельно разрешенный provider-derived
catalog fixture, но не объявляет публичный Hotels API стабильным или готовым к
промышленной эксплуатации.

Цель — проверить transport shape четырех фильтров Stage 12 до создания domain
preferences, DTO и mapping.

## Источники

- три предоставленных OpenAPI 3.0.4 export HotelsApi версий 1.0, 2.0 и 3.0;
- один анонимный `GET /api/v2/hotels/search-filters` от 21 июля 2026 года;
- существующие обезличенные search fixtures Stage 9.14.

OpenAPI формально наследует root `SiamBearer`, однако разрешенный анонимный
catalog-вызов вернул `200`. Это подтверждает только наблюдаемое поведение
публичного host и не заменяет официальный server-to-server contract.

## Swagger-сверка

`POST /api/v1/hotels/search` и
`POST /api/v1/hotels/search-filters-availability` принимают один
`SearchParametersListApiRequest`. Поле `filters` содержит `oneOf`:

- `ArraySearchFilterApi` — обязательные `$objectType`, `filterId`, `values[]`;
- `BooleanSearchFilterApi` — обязательные `$objectType`, `filterId`, `value`;
- `RadioSearchFilterApi` — обязательные `$objectType`, `filterId`, `value`;
- `RangeSearchFilterApi` — обязательные `$objectType`, `filterId`, `min`, `max`.

Discriminator подтверждает значения `array`, `boolean`, `radio`, `range`.
`SearchSortingApi` допускает поля `default`, `price`, `review_rating` и порядок
`asc`/`desc`, но фактическое принятие выбранных combinations в Stage 12.1 не
проверялось.

## Результат catalog-вызова

Вызов выполнен ровно один раз без `Authorization`, cookies, redirects и
retries. Использованы `Accept: application/json`, `X-User-Language: RU`,
connect timeout 10 секунд и общий timeout 60 секунд.

| Проверка | Результат |
|---|---|
| HTTP status | `200` |
| Content type | `application/json; charset=utf-8` |
| `payload.filters` | 14 элементов |
| `payload.popularFilters` | 9 элементов |
| Redirect | Отсутствует |

## Сверка четырех фильтров

| Product preference | Ожидание Stage 12.0 | Catalog fact | Verdict |
|---|---|---|---|
| Максимальная стоимость | `price`, `range` | `price`, `range` | Shape подтвержден; request еще не проверен |
| Звезды | `stars`, `array` | `stars`, `array`, строковые `5..0` | Shape и значения подтверждены |
| Минимальный рейтинг | `review_rating`, `range` | `review_rating`, `radio`, значения `9..5` | **Contract drift** |
| Бесплатная отмена | `free_cancellation_allowed`, `boolean` | `boolean`; popular value `true` | Shape подтвержден; request еще не проверен |

Главное расхождение: provider catalog моделирует минимальный гостевой рейтинг
как дискретный `radio`, а не как произвольный `range`. Поэтому mapping
`minGuestRating..10` нельзя реализовывать как подтвержденный contract.

## Fail-closed остановка

После обнаружения drift не выполнялись:

- `POST /api/v1/hotels/search-filters-availability`;
- filtered `POST /api/v1/hotels/search`.

Автоматические повторы и альтернативные payload не отправлялись. Stage 12.1 не
меняет production DTO, mapper, provider orchestration или runtime.

## Fixture verification

В test resources добавлены обезличенные:

- `stage-12-1/search-filters-catalog.json` — provider-derived body без headers
  и пользовательских данных;
- `stage-12-1/fixture-manifest.json` — status, media type, исключенные категории
  metadata и две остановленные проверки.

`HotelsApiSearchFilterCatalogFixtureContractTest` закрепляет:

- `price=range`;
- `stars=array` и строковые значения `5..0`;
- фактический `review_rating=radio` и значения `9..5`;
- `free_cancellation_allowed=boolean` и popular value `true`;
- ровно один выполненный вызов и две fail-closed остановки.

## Следующее решение

Stage 12.1a должен выбрать provider-neutral семантику минимального рейтинга до
любого mapping:

- ограничить product input дискретными порогами `5`, `6`, `7`, `8`, `9`;
- либо определить явное округление произвольного значения с подтверждением
  владельца продукта;
- либо временно исключить rating filter из первого refinement slice.

Нельзя автоматически округлять, ослаблять или усиливать пользовательский
порог. Новые live-вызовы требуют отдельной задачи после решения Stage 12.1a.

## Что не входило в этап

- production code, filter DTO и mapper;
- preferences/session/LLM changes;
- availability и filtered-search calls после drift;
- public API, OpenAPI Travel Assistant и demo shell;
- pagination, auth, durable storage, booking и payment;
- заявление production readiness.

## Verdict

`CONTRACT_DRIFT_DETECTED_STAGE_12_2_BLOCKED`.

Stage 12.1 завершен как fail-closed contract verification. Stage 12.2 нельзя
начинать до отдельного Stage 12.1a reconciliation минимального гостевого
рейтинга.
