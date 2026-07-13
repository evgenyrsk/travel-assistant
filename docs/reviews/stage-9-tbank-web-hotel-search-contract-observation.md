# Наблюдаемые контракты публичного web-поиска отелей Т-Банка

**Роль:** review/audit artifact для Stage 9. Документ фиксирует сетевое поведение
публичной web-формы на дату наблюдения. Он не является официальной спецификацией
Т-Банка, новым source of truth, accepted provider contract или разрешением
подключить эти endpoint-ы к runtime Travel Assistant.

**Дата наблюдения:** 13 июля 2026 года.

## 1. Цель и границы

Цель проверки — зафиксировать HTTP-методы, endpoint-ы, наблюдаемые request
payload и последовательность вызовов формы поиска отелей на странице
[Т-Путешествий](https://www.tbank.ru/travel/hotels/new/?internal_source=navPanelFromMainPageToHotelsMain),
а затем сопоставить их с выбранным внутренним контрактом HotelsApi из Stage 9.7,
Stage 9.10 и Stage 9.11a.

В проверку входили:

- исходная загрузка формы;
- ввод направления `Сочи`;
- выбор варианта `Big Sochi` с `locationId = 88519`;
- поиск на 18–19 июля 2026 года для двух взрослых;
- запись сетевых запросов от загрузки формы до появления результатов;
- анализ DOM формы, HTTP-методов, URL, request headers и JSON body.

Из проверки исключены:

- аналитические, рекламные и клиентские logging endpoint-ы;
- booking, payment, cancellation и пользовательские заказы;
- авторизованный сценарий;
- изменение фильтров, сортировки, количества комнат и состава детей;
- полная верификация response/error schema;
- попытка использовать web endpoint-ы как server-to-server API.

## 2. Методика и уровень достоверности

Проверка выполнена через `agent-browser 0.31.1` в изолированной анонимной
Chrome-сессии. HAR-запись содержала 546 запросов. После анализа временный HAR был
удален; уникальные идентификаторы устройства, сессии, трассировки и сетевые
данные в репозиторий не переносились.

Степень подтверждения разделена на три уровня:

| Уровень | Значение |
|---|---|
| Наблюдалось | Метод, URL, status, header или request body присутствовали в HAR/DOM. |
| Вывод | Поведение следует из последовательности запросов, но не подтверждено официальной спецификацией. |
| Неизвестно | Данные не были получены или проверены в форме, достаточной для contract implementation. |

Фиксация web-трафика подтверждает текущее поведение UI, но не стабильность,
доступность для внешнего backend или право на повторное использование endpoint-а.

## 3. Поведение формы

DOM исходной страницы показал:

- `document.forms.length === 0`;
- кнопка `Искать` имеет `type="button"`, не связана с `<form>` и не имеет
  `formMethod`;
- поле направления имеет `type="text"`, пустой `name` и `autocomplete="off"`;
- после нажатия `Искать` URL меняется без обычного HTML submit.

Следовательно, форма управляется JavaScript-клиентом. В HAR не было отдельной
document-навигации для маршрута результатов; это согласуется с client-side
router/History API, но точная frontend-функция не исследовалась.

Сначала клиент формирует маршрут:

```text
/travel/hotels/new/search/?guests=2&locationCode=city&dateFrom=2026-07-18&dateTo=2026-07-19&destination=Big+Sochi&destinationId=88519&searchSource=Search
```

После получения SEO slug маршрут заменяется на:

```text
/travel/hotels/new/countries/russia/sochi/?guests=2&locationCode=city&dateFrom=2026-07-18&dateTo=2026-07-19&destination=Big+Sochi&destinationId=88519&searchSource=Search
```

## 4. Используемые HTTP-методы

В hotel search flow наблюдались:

- `GET` — вспомогательные данные, SEO и проверка авторизации;
- `POST` — autocomplete, поиск, фильтры, данные отелей и актуальные предложения;
- `OPTIONS` — автоматические CORS preflight между `www.tbank.ru` и
  `hotels.tbank.ru`.

`OPTIONS` не является отдельной бизнес-операцией. GraphQL-вызовы в проверенном
flow не обнаружены.

## 5. Autocomplete

### 5.1 Request contract

| Поле | Наблюдаемое значение |
|---|---|
| Метод | `POST` |
| URL | `https://hotels.tbank.ru/search-api/search/autocomplete` |
| `Content-Type` | `application/json` |
| `x-api-method-name` | `createSearchAutocomplete` |
| Body | `{"input":"Сочи"}` |
| Status | `200` |

Наблюдаемый минимальный request shape:

```json
{
  "input": "Сочи"
}
```

### 5.2 Наблюдаемый результат

UI отобразил один список с двумя семантическими группами:

- location candidates: города/аэропорты с числовыми identifier-ами;
- hotel candidates: конкретные отели с отдельными identifier-ами.

В тесте были показаны пять location candidates и десять hotel candidates.
Первый location candidate — `Big Sochi`, `locationId = 88519`.

Точная JSON-оболочка ответа, обязательность полей, enum типов и error schema не
проверены. Наличие двух групп согласуется с моделями Stage 9.10, но не доказывает,
что публичный web endpoint использует тот же response contract.

## 6. Основной search flow

### 6.1 Общий request context

Основные search endpoint-ы используют `https://hotels.tbank.ru` и JSON body со
следующим общим контекстом:

```json
{
  "searchTag": "cin=20260718&cout=20260719&did=88519&adults=2&cages=",
  "locationId": 88519,
  "checkinDate": "2026-07-18",
  "checkoutDate": "2026-07-19",
  "guests": {
    "adultsCount": 2,
    "childrenAge": []
  },
  "filters": [],
  "mapFrameInput": {}
}
```

Этот пример отражает один тестовый сценарий. Он не подтверждает допустимые
диапазоны, обязательность каждого поля, формат детских возрастов, структуру
filter-ов или правила размещения по комнатам.

### 6.2 POST endpoint-ы

| Назначение | Endpoint | `x-api-method-name` | Наблюдаемые body fields | Status |
|---|---|---|---|---:|
| Точки/идентификаторы результатов | `/search-api/v2/hotels/map/searchHotelPoints` | `createMapSearchHotelPoints2` | Общий search context; `mapFrameInput.mapParameters.pinLimit`; позднее optional `etag` | 200 |
| Доступные фильтры | `/search-api/v2/hotels/searchFilters` | `createSearchFilters2` | Общий search context | 200 |
| Статические данные отелей | `/search-api/v1/hotels/getHotelStaticInfo` | `createGetHotelStaticInfo` | `hotelIds` | 200 |
| Ожидание готовности | `/search-api/v1/hotels/waitLoadingCompleted` | `createWaitLoadingCompleted` | Общий search context | 200 |
| Актуальные предложения | `/search-api/v1/hotels/getLatestHotelOffer` | `createGetLatestHotelOffer` | Общий search context и `hotelIds` | 200 |
| Достопримечательности | `/api/v2/points_of_interest/search` | `createPointsOfInterestSearch` | `hotelIds` | 200 |

`hotelIds` передаются пакетами. Числовые значения являются provider facts и не
должны автоматически трактоваться как booking offer reference.

Пример request body для map search:

```json
{
  "searchTag": "cin=20260718&cout=20260719&did=88519&adults=2&cages=",
  "locationId": 88519,
  "checkinDate": "2026-07-18",
  "checkoutDate": "2026-07-19",
  "guests": {
    "adultsCount": 2,
    "childrenAge": []
  },
  "filters": [],
  "mapFrameInput": {
    "mapParameters": {
      "pinLimit": 50
    }
  }
}
```

Пример пакетного запроса статических данных:

```json
{
  "hotelIds": [1410943, 1411329]
}
```

Массив в примере сокращен и не является fixture.

### 6.3 GET endpoint-ы

| Назначение | Endpoint | `x-api-method-name` | Status |
|---|---|---|---:|
| Бейджи выдачи | `/search-api/v1/badges` | `getV1Badges` | 200 |
| SEO-регион | `/api/v1/seo/regions/88519` | `getSeoRegions` | 200 |
| SEO slug | `/api/v2/seo/slug-by-location?locationId=88519` | `getSeoSlugByLocation` | 200 |
| Проверка авторизации | `/authorization/validate` | Не наблюдался | 401 |

Анонимная проверка авторизации вернула `401`, но перечисленные search endpoint-ы
вернули `200`. Header `Authorization` в их запросах не наблюдался. Это
подтверждает только конкретную анонимную web-сессию и не является гарантией
анонимного server-to-server доступа.

## 7. Последовательность загрузки результатов

Наблюдаемый flow:

1. Ввод текста вызывает autocomplete.
2. Выбор location сохраняет числовой `locationId` и открывает следующий шаг формы.
3. Нажатие `Искать` меняет client-side route и запускает запросы badges/SEO.
4. `searchHotelPoints` и `searchFilters` запускаются параллельно.
5. `searchHotelPoints` повторяется; в одном из последующих запросов появляется
   `etag`.
6. `getHotelStaticInfo` загружает данные пакетами по `hotelIds`.
7. `waitLoadingCompleted` проверяет завершение асинхронной загрузки.
8. `getLatestHotelOffer` догружает актуальные предложения пакетами.
9. `points_of_interest/search` обогащает набор отелей.
10. `slug-by-location` позволяет заменить временный `/search/` route на SEO URL.

Повторные `searchHotelPoints` похожи на polling/incremental loading. Точная
семантика `etag`, условия остановки и гарантия стабильности списка неизвестны.

## 8. Наблюдаемые headers

| Header/группа | Наблюдение | Решение для Travel Assistant |
|---|---|---|
| `Content-Type: application/json` | Присутствует у POST | Совпадает с текущим transport boundary. |
| `x-api-method-name` | Присутствует у business endpoint-ов | Операционное metadata; не переносить без официального контракта. |
| `X-Source-Platform: web` | Присутствует | Значение относится к web-клиенту; не копировать в backend автоматически. |
| `X-User-Language` | Наблюдался у autocomplete | Допустимые значения и обязательность требуют спецификации. |
| `X-Travel-Session-Id` | Присутствует | Session/privacy boundary; не сохранять и не генерировать без отдельного решения. |
| Device/sticky/tracing headers | Присутствуют | Динамические transport/telemetry values; не hardcode-ить. |
| Feature-toggle headers | Присутствуют | Непрозрачные и динамические; не считать частью публичного provider contract. |
| `Authorization` | Не наблюдался у search endpoint-ов | Не доказывает официальный anonymous contract. |

Значения session/device/tracing/toggle headers намеренно не документируются.

## 9. Сопоставление с текущим HotelsApi в репозитории

Текущий repository contract основан на внутреннем HotelsApi и зафиксирован в
Stage 9.7, Stage 9.10 и Stage 9.11a. Наблюдаемый web-flow отличается от него.

| Аспект | Текущий repository contract | Наблюдаемый public web-flow | Вывод |
|---|---|---|---|
| Host | `https://hotels.tcsbank.ru/` | `https://hotels.tbank.ru/` | Разные target-ы; взаимозаменяемость не доказана. |
| Autocomplete path | `/api/v1/hotels/autocomplete` | `/search-api/search/autocomplete` | Разные endpoint-ы. |
| Autocomplete body | `{"query":"..."}` | `{"input":"..."}` | DTO несовместимы без отдельного adapter. |
| Search path | `/api/v1/hotels/search` | Несколько `/search-api/v1|v2/...` endpoint-ов | Внутренний single-call contract и web orchestration не эквивалентны. |
| Destination | `destinationId` | `locationId` | Семантическая связь вероятна, но не подтверждена contract mapping. |
| Dates | Provider date-time strings | `YYYY-MM-DD` | Нельзя менять timezone/date policy по одному наблюдению. |
| Guests | `guests[]` по размещениям | Один объект `guests` в проверенном сценарии | Room/occupancy semantics различаются или скрыты. |
| Pagination/readiness | `offset`, `limit`, `nextOffset`, `isLoadingCompleted` | Повторные map calls, `etag`, отдельный `waitLoadingCompleted` | Механизмы incremental loading различаются. |
| Response | `payload` DTO из Swagger | Raw JSON schema не проверена | Прямое переиспользование mapper невозможно обосновать. |
| Auth | Anonymous public transport выбран в Stage 9.9; Swagger risk сохранен | Анонимные web search calls вернули 200 | Совпадение по одному сценарию не закрывает host/auth gate. |

Наблюдаемый web-flow не заменяет:

- `HotelsApiAutocompleteRequestDto` и `HotelsApiAutocompleteResponseDto`;
- `HotelsApiSearchRequestDto` и `HotelsApiSearchResponseDto`;
- `PublicHotelsApiHttpTransport`;
- host/auth decisions Stage 9.8a и Stage 9.9;
- roadmap gates Stage 9.11b–9.17.

## 10. Проверенное и неизвестное

### Проверено

- [x] DOM не использует обычный HTML form submit.
- [x] HTTP-методы `GET`, `POST` и CORS `OPTIONS`.
- [x] Autocomplete URL, method, request body и status.
- [x] Основные search URL, `x-api-method-name`, request body fields и status.
- [x] Client-side route parameters и SEO route transition.
- [x] Отсутствие `Authorization` в наблюдаемых business requests.
- [x] Отличия от текущих provider DTO и configured host в репозитории.

### Не проверено

- [ ] Официальные OpenAPI/Swagger для `hotels.tbank.ru/search-api/**`.
- [ ] Полные response schema и обязательность каждого поля.
- [ ] Error contract для 400/401/403/429/5xx.
- [ ] Разрешение на server-to-server использование endpoint-ов.
- [ ] Rate limits, retry policy, timeout и polling budget.
- [ ] Required cookies, gateway headers и anti-abuse constraints.
- [ ] Стабильность `x-api-method-name`, `searchTag`, `etag` и provider IDs.
- [ ] Semantics цены, taxes/fees, cancellation, availability и cashback.
- [ ] Сценарии детей, нескольких комнат, filters и pagination.

## 11. Решение по архитектурной границе

Этот artifact является входом для будущей contract reconciliation/fixture
verification, но не изменяет provider boundary и не активирует real provider.

Без отдельной roadmap-aligned задачи запрещено:

- менять current HotelsApi DTO под наблюдаемый web-flow;
- менять `HOTELS_API_PUBLIC_BASE_URL` на `hotels.tbank.ru`;
- добавлять `x-api-method-name`, feature-toggle или device headers в transport;
- подключать web endpoint-ы к `RealHotelOfferProviderAdapter`;
- объявлять web endpoint-ы публичными или стабильными;
- выполнять live calls из backend Travel Assistant.

Перед возможным использованием нужны owner-approved contract, разрешение на
server-to-server доступ, подтвержденные host/auth, обезличенные fixtures и
отдельный sandbox readiness gate.

## 12. Итог

Публичная форма Т-Банка использует не один search endpoint, а staged JSON flow:
autocomplete, параллельный поиск точек/filters, пакетные static data/latest
offers, readiness polling и SEO resolution. Этот flow подтверждает полезные
семантические ориентиры — числовой location id, даты, состав гостей, hotel ids и
incremental loading, — но технически не совпадает с выбранным внутренним
HotelsApi contract Travel Assistant.

Roadmap/status, production code, provider DTO и runtime wiring этим наблюдением
не меняются.

## 13. Связанные документы

- [Основной roadmap](../roadmap/roadmap.md)
- [Architecture baseline](../architecture/architecture-baseline.md)
- [Stage 9.7 — Hotels API contract reconciliation](stage-9-7-selected-hotels-api-contract-reconciliation-and-implementation-plan.md)
- [Stage 9.8a — Authentication configuration reconciliation](stage-9-8a-hotels-api-authentication-configuration-reconciliation.md)
- [Stage 9.9 — Public anonymous HTTP transport](stage-9-9-public-anonymous-hotels-api-http-transport.md)
- [Stage 9.10 — Autocomplete/location boundary](stage-9-10-autocomplete-location-resolution-contract-boundary.md)
- [Stage 9.11a — Search DTO](stage-9-11a-hotels-api-search-dto-without-domain-mapping.md)
