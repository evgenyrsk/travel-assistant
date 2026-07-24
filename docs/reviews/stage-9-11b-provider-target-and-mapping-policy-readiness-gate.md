# Stage 9.11b — готовность выбора целевого provider API и правил преобразования

**Роль:** review/design-only этап проверки готовности. Документ фиксирует
решение перед реализацией mapper поиска и provider adapter. Он не является
новым контрактом Hotels API, разрешением на server-to-server использование
публичного web API или заявлением о готовности real provider.

## 1. Цель и текущая точка

Цель Stage 9.11b — сопоставить выбранный внутренний Hotels API с наблюдаемым
публичным web-сценарием и определить, какой целевой API и какие правила
преобразования можно безопасно использовать дальше.

После Stage 9.10 и Stage 9.11a в репозитории уже есть:

- `HotelLocationResolverBoundary`, autocomplete DTO и location mapper без
  transport/runtime wiring;
- search request/response DTO внутреннего Hotels API без domain mapping;
- `PublicHotelsApiHttpTransport`, проверенный только через `MockEngine`;
- конфигурационная граница Hotels API с `FAKE` как режимом по умолчанию;
- audit artifact наблюдаемого сценария `hotels.tbank.ru/search-api/**`.

После отдельного разрешения владельца проекта выполнена ограниченная проверка
`https://hotels.tbank.ru/` без credentials, cookies и session/device headers:

- публичный autocomplete вернул `200 application/json`;
- `POST /api/v1/hotels/search` вернул `400` на пустой body и `200` на
  минимальный валидный request;
- `GET /api/v2/hotels/search-filters` вернул `200`;
- v2 и v3 `hotels/urls/search` вернули ожидаемый `400` на пустой body, что
  подтверждает наличие маршрутов;
- `locations[].id` из autocomplete был принят v1 search как `destinationId`;
- структура search response совместима с текущим
  `HotelsApiSearchResponseDto`.

Таким образом, `https://hotels.tbank.ru/` подтвержден как доступный base URL
выбранного внутреннего Hotels API. Успешная техническая проверка не заменяет
разрешение на постоянное server-to-server использование и не превращает
публичный web-flow в официальный provider contract.

## 2. Матрица подтверждений

| Вариант | Подтвержденность контракта | Доступность и авторизация | DTO и оркестрация | Риски | Вывод |
|---|---|---|---|---|---|
| Внутренний Hotels API на `hotels.tbank.ru` | Есть OpenAPI 1.0/2.0/3.0 для поиска и предоставленная документация autocomplete. Контракт выбран в Stage 9.7. | v1 search и v2 filters вернули `200` без `Authorization`; v2/v3 URL routes подтвердились validation response. Успешный v1 search подтверждает anonymous MVP search, но не auth policy всех операций. | Текущие search DTO совместимы с проверенным ответом. Ожидаемая последовательность: location resolution, один v1 search, mapping и bounded pagination. | Нужны обезличенные fixtures, продуктовые правила преобразования и подтверждение постоянного server-to-server использования. | Выбранный provider API и base URL подтверждены. Реализация mapper заблокирована только оставшимися mapping decisions. |
| Публичный web API `hotels.tbank.ru/search-api/**` | Есть наблюдение browser flow и один успешный контрольный autocomplete request. Официальная спецификация и обязательства стабильности отсутствуют. | Autocomplete доступен из текущей среды без `Authorization` в одном запросе. Это не подтверждает server-to-server разрешение, обязательные headers, rate limits или auth всех endpoint-ов. | Autocomplete request несовместим с текущим DTO. Search является многошаговым сценарием с `etag`, пакетными запросами и отдельным ожиданием готовности. Совместимость одного ответа не доказывает общий контракт. | Web endpoint-ы могут изменяться вместе с UI; неизвестны polling, retry, limits и error semantics. | Только отдельный кандидат после одобрения владельца; не заменяет выбранный контракт. |
| Два независимых adapter-а | Наследует разный уровень подтверждения обоих API. | Нужны отдельные правила target/auth/operations. | Требует независимых provider DTO, transport policy и orchestration с общей нормализацией только на application boundary. | Удваивает поверхность интеграции до подтверждения второго provider contract и продуктовой необходимости. | Сейчас не рекомендуется; решение отложено. |

## 3. Решение по целевому provider API

- Внутренний Hotels API остается текущим выбранным контрактом.
- `https://hotels.tbank.ru/` принимается как подтвержденный base URL для
  внутренних `/api/v1|v2|v3` endpoint-ов.
- `HOTELS_API_PUBLIC_BASE_URL` следует изменить отдельным configuration-only
  этапом с targeted tests.
- Публичный `/search-api/**` autocomplete остается отдельной resolver-операцией
  с request field `input`; он не переопределяет DTO внутренних API.
- Реализация публичной search orchestration запрещена до подтверждения
  server-to-server использования, обязательных headers, rate limits,
  polling/`etag` semantics и error contract.
- Два adapter-а допустимы архитектурно, но не оправданы текущими данными и
  bounded hotel-only MVP.

## 4. Решение об изоляции DTO

Одинаковая наблюдаемая структура ответа не разрешает совместно использовать
provider DTO двух независимых API без отдельного решения.

Если публичный web API будет одобрен, для него нужны:

- отдельные request/response DTO в собственной provider namespace;
- отдельные transport и orchestration rules;
- явное преобразование к общей application/domain boundary;
- независимые fixtures и contract tests.

Текущие `HotelsApiAutocomplete*Dto` и `HotelsApiSearch*Dto` остаются моделями
выбранного внутреннего Hotels API. Их нельзя молча переопределить под web-flow.

## 5. Правила преобразования назначения

- `locations[].id` подтвержден контрольным v1 search как рабочий
  `destinationId` для location candidate.
- `hotels[].id` идентифицирует конкретный отель и не должен преобразовываться в
  destination id.
- Автоматический выбор первого autocomplete результата запрещен: порядок
  подсказок не является доказательством однозначного намерения пользователя.
- При нескольких подходящих location candidates application flow должен
  сохранить явный выбор пользователя или запросить уточнение.
- Hotel candidate требует отдельной продуктовой ветки; его нельзя незаметно
  передавать в location-based search.

## 6. Блокеры преобразования search DTO

Реализация mapper между `HotelSearchCriteria`, provider request DTO и
`HotelOffer` пока небезопасна.

| Область | Нерешенный вопрос | Риск преждевременного mapping |
|---|---|---|
| Guests и rooms | Как распределять взрослых и детей по элементам `guests[]`; означает ли элемент комнату | Неверная occupancy и цена |
| Child ages | Допустимый диапазон, обязательность и порядок возрастов | Provider validation error или неверное предложение |
| Dates и timezone | Точный wire format и timezone для `checkinDate`/`checkoutDate` | Смещение дат и неверная доступность |
| Review и rating | Шкала, nullable review и значение отсутствующего рейтинга | Искаженное ранжирование |
| `shownPrice` | Включены ли taxes/fees, за ночь или за весь stay, валюта и количество комнат | Некорректное отображение и сравнение цены |
| Offer semantics | `paymentPlace`, cancellation, meal и availability | Потеря существенных provider facts |

Эти вопросы требуют данных владельца контракта. Adapter mapping не должен
заменять продуктовые решения значениями по умолчанию или предположениями.

## 7. Архитектурные последствия

- Существующей provider boundary достаточно для одного выбранного API.
- `HotelLocationResolverBoundary` остается правильной application boundary:
  transport-specific request shape не должен попадать в domain.
- Для внутреннего API ожидается orchestration
  `location resolver -> v1 search -> provider mapper`, но ее нельзя подключать к
  runtime до закрытия mapping и fixture blockers.
- Для публичного web API потребовался бы отдельный multi-step orchestrator.
  Переиспользование внутреннего single-call adapter привело бы к смешению
  несовместимых lifecycle и pagination semantics.
- Дополнительный provider storage сейчас не требуется. Web polling state и
  `etag` нельзя моделировать до подтвержденного контракта.

## 8. Обязательные данные владельца контракта

До Stage 9.11c нужны:

1. Для внутреннего API — обезличенные autocomplete/search/error fixtures и
   подтверждение endpoint-level auth matrix за пределами проверенных операций.
2. Для публичного web API, если он рассматривается дальше, — разрешение на
   server-to-server использование, обязательные headers, rate limits,
   timeout/retry policy, `etag` и polling stop conditions, error schemas и
   обезличенные fixtures.
3. Правила выбора location при нескольких autocomplete candidates и поведение
   для hotel candidate.
4. Правила rooms/guests, диапазон child ages и wire timezone/date format.
5. Шкала rating, nullable semantics review и значение отсутствующего рейтинга.
6. Состав `shownPrice`, taxes/fees, период цены, currency, `paymentPlace`, meal,
   cancellation и availability semantics.

## 9. Что не входит в Stage 9.11b

- production code и tests;
- изменение DTO, mapper, transport или configuration;
- дополнительные live API calls и сохранение response fixtures;
- public API, OpenAPI Travel Assistant, frontend и generated clients;
- runtime wiring режима `REAL`;
- новые headers или изменение provider base URL;
- выбор публичного web API как официального provider contract.

## 10. Риски преждевременной реализации

- молчаливое смешение двух независимых API под одними DTO;
- выбор неверного `destinationId` или hotel id вместо location id;
- неверная occupancy, дата или стоимость;
- зависимость backend от нестабильного web-клиентского flow;
- бесконечный либо чрезмерный polling из-за неизвестных stop conditions;
- нарушение rate limits или server-to-server policy;
- утечка provider-specific ошибок и metadata в публичный контракт.

## 11. Решение по roadmap и Stage 9.11c

Stage 9.11b завершается как review/design-only readiness gate. Выбранный
provider API и base URL подтверждены, но этап не включает search mapper
implementation.

Перед mapper требуется отдельный Stage 9.11b1: заменить default public base URL
с `https://hotels.tcsbank.ru/` на `https://hotels.tbank.ru/`, обновить targeted
configuration tests и не менять transport/runtime wiring.

Следующий возможный этап переименовывается в Stage 9.11c:
`Hotels API search domain mapping implementation`. Его начало разрешено только
после получения продуктовых данных из раздела 8. До этого Stage 9.11c имеет
статус `Заблокирован`.

Минимальный scope будущего Stage 9.11c:

- mapper между `HotelSearchCriteria` и request DTO выбранного API;
- mapper provider response в `HotelOffer` с явными nullable/price rules;
- targeted mapping tests на обезличенных либо утвержденных synthetic fixtures;
- без transport call, runtime wiring, pagination orchestration и live request.

Публичный web adapter, dual-provider routing и multi-step polling не входят в
Stage 9.11c без отдельного owner-approved roadmap step.

## 12. Итоговый вывод о готовности

| Область | Вывод |
|---|---|
| Целевой provider API | Подтвержден: внутренний API на `https://hotels.tbank.ru/`. |
| DTO isolation | Граница определена: DTO разных API должны оставаться независимыми. |
| Destination mapping | Техническая связь location id -> `destinationId` подтверждена; hotel id и автоматический первый результат запрещены. |
| Search domain mapping | Не готов; заблокирован данными владельца контракта. |
| Public web orchestration | Не готова и не разрешена к реализации. |
| Stage 9.11b | Завершен как readiness gate без implementation. |
| Stage 9.11b1 | Разрешен configuration-only base URL reconciliation. |
| Stage 9.11c | Заблокирован до подтверждения продуктовых правил mapping. |

Stage 9.11b не означает готовность real provider, transport integration или
готовность к промышленному использованию.
