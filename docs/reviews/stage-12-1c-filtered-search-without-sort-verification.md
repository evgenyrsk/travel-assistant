# Stage 12.1c — проверка filtered search без сортировки

## Роль и цель

Это ограниченный contract-verification отчет. Актуальный статус задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md), продуктовые границы —
[`docs/product/product-baseline.md`](../product/product-baseline.md).

Цель — проверить четыре filter payload непосредственно на
`POST /api/v1/hotels/search` после runtime-запрета `sort`, обнаруженного в
Stage 12.1b.

## Контролируемый вызов

Выполнен ровно один анонимный запрос без `sort`, `Authorization`, cookies,
redirects и retries. Использованы `Accept: application/json`,
`Content-Type: application/json`, `X-User-Language: RU`, connect timeout 10
секунд и общий timeout 60 секунд.

Синтетический context:

- destination `17039`, даты `2026-08-10` — `2026-08-14`;
- одна guest group: два взрослых, без детей;
- `offset=0`, `limit=20`;
- `price`: range `0..80000`;
- `stars`: array `4`, `5`;
- `review_rating`: radio `8`;
- `free_cancellation_allowed`: boolean `true`.

## Фактический результат

| Проверка | Результат |
|---|---|
| HTTP status | `200` |
| Content type | `application/json; charset=utf-8` |
| Redirect | Отсутствует |
| Получено hotels | `20` |
| Currency | Только `RUB` |
| Stars | Только `4`, `5` |
| Максимальный `shownPrice` | `69843`, не выше `80000` |
| Минимальный известный guest rating | `8.2` |
| Отсутствующий review | `0` |
| Отсутствующий `freeCancellationUntil` | `0` |

Все 20 полученных предложений соответствовали наблюдаемым значениям четырех
фильтров. Это подтверждает wire shape и фактическое принятие выбранной
комбинации текущим runtime endpoint.

`isLoadingCompleted=false` и `nextOffset=20` остаются provider facts. Они не
запускают pagination или polling и не меняют single-page policy Stage 9.13.

## Fixture verification

В репозитории сохранены два обезличенных представителя исходной выдачи и
manifest с агрегированными фактами всех 20 результатов. Заменены provider и
location identifiers, названия, signatures, адреса, координаты и image URLs;
headers и session/device/tracing metadata не сохранены.

`HotelsApiFilteredSearchWithoutSortFixtureContractTest` проверяет:

- десериализацию через существующий `HotelsApiSearchResponseDto`;
- соблюдение price/stars/rating/cancellation facts;
- перенос opaque provider reference, total price, currency и review;
- отсутствие выдуманных amenities и `LIMITED`;
- отсутствие `sort` в проверенном request contract;
- один успешный вызов без retries и альтернативных payload.

## Закрытые вопросы

| Вопрос | Решение |
|---|---|
| Четыре filter payload на search endpoint | Подтверждены одним `200` вызовом |
| `review_rating` | `radio.value="8"` |
| Пользовательский `sort` | Отложен; runtime его запрещает |
| Локальная имитация sort | Не добавляется |
| Filter availability | Не подключается после пустого payload Stage 12.1b |
| Пустой успешный search | Считался бы wire-pass; фактический ответ был непустым |
| Pagination/polling | Не добавляются |

## Что не входит в этап

- production DTO, mapper, orchestration и runtime changes;
- preference/session/LLM implementation;
- public API, OpenAPI Travel Assistant и demo shell;
- повторные live-вызовы;
- pagination, auth, durable storage, booking и payment;
- заявление production readiness или официальной стабильности публичного API.

## Следующий этап

Stage 12.2 может добавить provider-neutral preference model только для четырех
подтвержденных filters. Пользовательская сортировка, provider identifiers и
wire-specific `$objectType`/`filterId` не должны переходить в domain.

## Verdict

`PASS_FILTERED_SEARCH_WITHOUT_SORT_CONTRACT`.

Stage 12.1c завершен; Stage 12.2 разблокирован.
