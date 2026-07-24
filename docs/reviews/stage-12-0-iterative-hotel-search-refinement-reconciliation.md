# Stage 12.0 — пересогласование итеративного уточнения hotel search

## Роль и цель

Это review/design-only отчет о пересогласовании функционального MVP после
успешной локальной REAL-демонстрации Stage 11.0. Текущий статус задает
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md), продуктовые границы —
[`docs/product/product-baseline.md`](../product/product-baseline.md), а
архитектурные —
[`docs/architecture/architecture-baseline.md`](../architecture/architecture-baseline.md).

Цель Stage 12.0 — выбрать следующий узкий hotel-only flow на основании уже
исследованных Hotels API, не начинать реализацию фильтров до проверки их
транспортных контрактов и не расширять текущий этап production-кодом.

## Пересогласованный функциональный MVP

Текущий демонстрационный MVP должен поддерживать цикл:

1. получить первичные предложения после явного подтверждения обязательных
   критериев;
2. принять дополнительное уточнение в чате;
3. установить, изменить или снять явно названные фильтры или сортировку;
4. показать полный обновленный набор критериев;
5. после нового подтверждения выполнить ровно один новый provider search;
6. вернуть более точную выдачу с новым `hotelSearchId`.

Необязательные предпочтения первого среза:

| Предпочтение | Семантика MVP |
|---|---|
| Максимальная стоимость | Provider total за весь период; без валюты — RUB, без конвертации |
| Звезды | Явно выбранный набор звезд |
| Минимальный гостевой рейтинг | Нижняя граница provider guest rating |
| Бесплатная отмена | Обязательное подтвержденное provider-свойство |
| Сортировка | Рекомендуемая, сначала дешевле или сначала с лучшим рейтингом |

Отсутствие предпочтений не задерживает первый поиск. Неизвестный или
неподдерживаемый фильтр приводит к уточнению. Фильтр не применяется только
к старому пулу из 20: изменение provider request требует нового подтверждения и
нового запроса. Предыдущий process-local search остается доступен.

## Контрактная основа

| Endpoint | Роль в Stage 12 |
|---|---|
| `POST /search-api/search/autocomplete` | Разрешение destination до поиска |
| `POST /api/v1/hotels/search` | Основной поиск с `filters` и `sort` |
| `GET /api/v2/hotels/search-filters` | Проверка каталога и транспортных значений фильтров |
| `POST /api/v1/hotels/search-filters-availability` | Возможное будущее объяснение доступных или ослабляемых фильтров |

Swagger указывает polymorphic filters и sort, но точные `$objectType`,
`filterId` и транспортные значения четырех выбранных фильтров должны быть
подтверждены в Stage 12.1. Наблюдение публичного web runtime не заменяет этот
gate.

Предварительное mapping-направление, которое не считается реализацией:

- `price` → range `0..maxTotalPrice`;
- `stars` → array;
- `review_rating` → range `minGuestRating..10`;
- `free_cancellation_allowed` → boolean;
- sort → `default`, `price/asc` или `review_rating/desc`.

Provider-specific identifiers и opaque filter values не должны переходить в
domain или public API.

## Архитектурные границы

- предпочтения принадлежат provider-neutral domain/application model;
- LLM извлекает только typed patch предпочтений и не создает provider facts;
- session context хранит неизмененные предпочтения между репликами;
- confirmation lifecycle остается обязательной границей до каждого нового
  provider request;
- mapping Hotels API остается в infrastructure layer;
- успешный refinement создает новый `hotelSearchId`, не изменяя предыдущий
  search;
- один request сохраняет `offset=0`, `limit=20`; pagination не добавляется;
- demo shell остается демонстрационным клиентом platform-neutral API.

## Последовательность Stage 12

| Этап | Результат |
|---|---|
| Stage 12.1 | Проверенные filter DTO, catalog, availability и filtered-search contracts |
| Stage 12.2 | Provider-neutral preferences и typed patch без изменений routes/provider |
| Stage 12.3 | Structured LLM extraction SET/CLEAR для preferences |
| Stage 12.4 | Deterministic Hotels API filter/sort mapping и подтвержденные offer facts |
| Stage 12.5 | Runtime refinement с повторным confirmation и новым search |
| Stage 12.6 | Platform-neutral optional response fields, OpenAPI и demo shell alignment |
| Stage 12.7 | Отличие no-results от failure и детерминированное ослабление одного фильтра |
| Stage 12.8 | Regression matrix и один отдельно разрешенный REAL smoke |

Stage 12.1 сначала читает локальные Swagger-артефакты. Любые live-вызовы
catalog, availability или filtered search требуют отдельного явного разрешения,
выполняются не более одного раза без retries и сохраняются только как
обезличенные fixtures.

## Что не входит в Stage 12.0

- production code и tests;
- изменения public API, OpenAPI или demo shell;
- provider/LLM calls и новые fixtures;
- pagination или filter panel;
- auth, durable storage, SDK и product clients;
- hotel details, rates, deeplink, shortlist и comparison;
- booking, payment и cancellation execution;
- заявление production readiness.

## Риски преждевременной реализации

- неверный `$objectType` или `filterId` приведет к provider rejection;
- применение фильтров к старому пулу даст неполный результат;
- изменение criteria без нового confirmation нарушит действующую safety
  boundary;
- перенос provider values в domain свяжет сервис с конкретным wire contract;
- текущий ranker может отменить выбранную provider sort без отдельной Stage 12.4
  policy.

## Verdict

`PASS_STAGE_12_0_ITERATIVE_REFINEMENT_RECONCILED`.

Stage 12 активирован в границах hotel-only MVP. Следующий шаг — Stage 12.1
filter contract verification. Реализация предпочтений и live API-вызовы до
прохождения этого gate не разрешены.
