# Stage 12.1b — контролируемая проверка filter request

## Роль и цель

Это contract-verification и ограниченное product-reconciliation решение.
Актуальный статус задает [`docs/roadmap/roadmap.md`](../roadmap/roadmap.md),
продуктовые границы —
[`docs/product/product-baseline.md`](../product/product-baseline.md).

Цель — проверить фактическое принятие четырех filter payload и `price/asc` до
создания provider DTO и mapping.

## Проверенный request contract

Оба запроса использовали один синтетический hotel search context, одну guest
group, `offset=0`, `limit=20` и следующий набор:

| Preference | Wire shape |
|---|---|
| Максимальная общая стоимость | `range`, `filterId=price`, `min=0`, `max=80000` |
| Звезды | `array`, `filterId=stars`, `values=["4","5"]` |
| Минимальный гостевой рейтинг | `radio`, `filterId=review_rating`, `value="8"` |
| Бесплатная отмена | `boolean`, `filterId=free_cancellation_allowed`, `value=true` |
| Сортировка | `field=price`, `order=asc` |

Форма соответствует локальному OpenAPI 1.0 export. Запросы выполнены без
`Authorization`, cookies, redirects и retries; использовались только
`Accept`, `Content-Type` и `X-User-Language: RU`.

## Фактические результаты

| Вызов | Status | Наблюдение |
|---|---:|---|
| `POST /api/v1/hotels/search-filters-availability` | `200` | JSON envelope принят, `payload` — пустой объект |
| `POST /api/v1/hotels/search` | `400` | `error.code=sorting_is_not_allowed_yet` |

Первый endpoint принял общий request shape, но не вернул
`filtersAvailability`. Поэтому его нельзя использовать для объяснения
доступных или ослабляемых фильтров в текущем MVP.

Filtered search явно запретил `sort`, хотя поле присутствует в Swagger. После
ответа `400` альтернативный payload без `sort` не отправлялся: лимит этапа в два
вызова и правило отсутствия автоматического подбора сохранены.

## Сверка решений

| Вопрос | Решение |
|---|---|
| `review_rating` request shape | `radio.value="8"` принят availability endpoint |
| Остальные три filter shape | Приняты availability endpoint; search требует отдельной проверки без `sort` |
| Provider sort | Не поддерживается наблюдаемым runtime search endpoint |
| Локальная замена provider sort | Не разрешена: сортировка ограниченного пула из 20 не равна глобальной сортировке provider выдачи |
| Пользовательские sort preferences в текущем MVP | Отложены до подтвержденной provider capability |
| Текущее ранжирование | Существующее рекомендуемое ранжирование остается default |
| Filter availability | Не подключается: endpoint вернул пустой payload |

## Fixture verification

В test resources добавлены обезличенные тела двух ответов и manifest. В них нет
headers, идентификатора destination, дат, cookies, session/device/tracing
metadata или пользовательских данных.

`HotelsApiFilterRequestFixtureContractTest` закрепляет:

- точные polymorphic filter shapes;
- `review_rating=radio.value="8"`;
- кандидат `price/asc`;
- `200` с пустым availability payload;
- `400 sorting_is_not_allowed_yet` для filtered search;
- ровно два вызова без retries и альтернативных payload.

## Что не входит в этап

- production DTO, mapper, provider orchestration и runtime;
- повторный filtered search без `sort`;
- application-side имитация provider-wide sorting;
- session preferences и LLM schema;
- public API, OpenAPI Travel Assistant и demo shell;
- pagination, auth, durable storage, booking и payment;
- заявление стабильности публичного server-to-server API.

## Следующий этап

Stage 12.1c должен выполнить ровно один отдельно разрешенный filtered search с
теми же четырьмя filters, но без `sort`. Повторы и альтернативные payload
запрещены. Только успешный `2xx` или точный provider outcome позволит решить,
готов ли wire mapping к Stage 12.2/12.4.

## Verdict

`FILTER_SHAPES_PARTIALLY_VERIFIED_SORT_RUNTIME_DRIFT`.

Продуктовая неопределенность сортировки закрыта ее переносом из текущего MVP.
Stage 12.2 остается заблокирован до Stage 12.1c.
