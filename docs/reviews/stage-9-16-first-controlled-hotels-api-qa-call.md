# Stage 9.16 — первый контролируемый Hotels API QA call

## Роль документа

Этот документ является review-артефактом Stage 9.16. Актуальный статус и
следующий разрешенный шаг определяет
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Выполнить один отдельно разрешенный анонимный
`POST /api/v1/hotels/search` через проектный
`PublicHotelsApiHttpTransport` и проверить live response текущими DTO и
response mapper без runtime wiring.

## Граница вызова

Вызов выполнен через test-scoped `HotelsApiDirectSearchQaHarness` и CIO client:

- public base URL: `https://hotels.tbank.ru/`;
- path: `/api/v1/hotels/search`;
- destination: `17039`;
- даты: `2026-08-10`–`2026-08-14`;
- одна guest group, двое взрослых, без детей;
- `offset=0`, `limit=20`;
- redirects отключены;
- `Authorization`, JWT, cookies и private target не использовались;
- retries, pagination и polling не выполнялись.

QA inputs не становятся публичным контрактом или runtime configuration.

## Наблюдаемый результат

| Проверка | Результат |
|---|---|
| Provider calls | 1 |
| HTTP status | `200` |
| `Content-Type` | `application/json; charset=utf-8` |
| Hotels в payload | 20 |
| Успешно mapped offers | 20 |
| `isLoadingCompleted` | `false` |
| `nextOffset` | Присутствует |

Response успешно десериализован в `HotelsApiSearchResponseDto` и полностью
преобразован `HotelsApiSearchResponseMapper`. Наличие `nextOffset` и
`isLoadingCompleted=false` не запустило повторный request: действующая policy
Stage 9.13 сохраняет один ограниченный пул кандидатов без pagination.

Полный response body обрабатывался только в памяти процесса, не сохранялся в
репозитории или `/tmp` и был отброшен после проверки. Provider identifiers,
названия, адреса, изображения и полный body не публиковались.

## Техническая проверка запуска

Первый Gradle invocation был пропущен как `UP-TO-DATE`, поэтому provider call
не произошел. После явной проверки этого факта test task был запущен с
`--rerun-tasks`; именно этот запуск выполнил единственный фактический request.
Автоматического повтора provider call не было.

Для воспроизводимого opt-in запуска добавлен
`HotelsApiDirectSearchQaCallTest`. Без `HOTELS_API_QA_ENABLED=true` test
завершается до создания `HttpClient`, поэтому обычный backend test suite не
использует сеть.

## Подтвержденные выводы

- публичный host и выбранный v1 search path доступны из текущей среды;
- анонимный request с текущим body принят;
- live response совместим с текущими search response DTO;
- live response совместим с текущей mapper policy;
- bounded request возвращает 20 mapped candidates;
- provider pagination metadata существует, но не требует pagination в MVP;
- test-scoped transport seam достаточен для контролируемой проверки.

## Что не подтверждено

- официальный server-to-server статус, SLA и rate limits;
- долговременная стабильность публичного endpoint;
- включение taxes/fees в `shownPrice`;
- policy для `LIMITED`;
- готовность `RealHotelOfferProviderAdapter` и runtime composition.

Один успешный QA call не является заявлением production readiness.

## Границы этапа

Stage 9.16 не добавляет:

- production network engine или production client factory;
- `REAL` runtime wiring;
- вызов provider из adapter, routes или application runtime;
- autocomplete, retries, redirects, polling или pagination;
- JWT, secrets, private API или auth changes;
- public API, OpenAPI Travel Assistant, frontend или generated clients;
- новый fixture, durable storage, booking/payment flow или `bookHash`.

`FAKE` остается provider по умолчанию.

## Проверки

- [x] opt-in entry point без флага не создает сетевой клиент;
- [x] выполнен ровно один фактический provider request;
- [x] status и media type проверены;
- [x] response DTO десериализация прошла;
- [x] response mapping прошел для всех 20 hotels;
- [x] полный provider body не опубликован и не сохранен;
- [x] повторный request не выполнен;
- [x] adapter и runtime composition не использовались.

## Readiness verdict

`QA_CALL_SUCCEEDED_RUNTIME_WIRING_NOT_STARTED`.

Stage 9.16 завершен как один контролируемый live QA call. Следующий roadmap
этап — Stage 9.17, отдельное opt-in `REAL` runtime wiring при сохранении `FAKE`
по умолчанию. Этот этап требует отдельной задачи и не активирован Stage 9.16.

## Связанные документы

- [Stage 9.15a — mock-only QA transport preflight](stage-9-15a-mock-only-qa-transport-preflight.md)
- [Stage 9.15 — sandbox readiness gate](stage-9-15-sandbox-readiness-gate.md)
- [Stage 9.14 — fixture verification](stage-9-14-sanitized-provider-fixture-contract-verification.md)
- [Основной roadmap](../roadmap/roadmap.md)
