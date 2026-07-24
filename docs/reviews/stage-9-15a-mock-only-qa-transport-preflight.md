# Stage 9.15a — mock-only preflight транспорта для QA

## Роль документа

Этот документ является review-артефактом Stage 9.15a. Актуальный порядок
этапов и следующий разрешенный шаг определяет
[`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Подготовить изолированную и выключенную по умолчанию границу для будущего
одиночного `POST /api/v1/hotels/search`, не выполняя сетевой запрос и не
подключая реальный provider к runtime.

## Текущая точка

Stage 9.15 выбрал прямой search call как форму будущего Stage 9.16, но выявил
четыре обязательных пробела: отсутствие test-scoped сетевого engine, явной
политики redirects, безопасного `Content-Type` в transport result и opt-in
QA harness. Stage 9.15a закрывает только эти пробелы.

## Добавленные изменения

| Область | Изменение |
|---|---|
| Transport metadata | `HotelsApiHttpResponse` сохраняет nullable `contentType` успешного ответа |
| Test dependencies | `ktor-client-cio-jvm` добавлен только в `testImplementation` |
| QA client policy | `applyHotelsApiQaPolicy` отключает redirects и включает `HttpTimeout` |
| QA harness | `HotelsApiDirectSearchQaHarness` выключен без явного environment-флага |
| Tests | Добавлены mock-only проверки policy, входных данных, запроса и безопасного результата |

Production network engine, client factory и runtime composition не добавлены.

## Входные данные будущего QA call

Harness принимает только явно заданные test inputs:

- `HOTELS_API_QA_ENABLED=true`;
- `HOTELS_API_QA_DESTINATION_ID=17039`;
- `HOTELS_API_QA_CHECKIN_DATE=2026-08-10`;
- `HOTELS_API_QA_CHECKOUT_DATE=2026-08-14`.

Request формируется детерминированно: одна комната, двое взрослых, без детей,
`offset=0`, `limit=20`. Даты сериализуются как `YYYY-MM-DD`. Некорректный или
неполный input отклоняется до создания `HttpClient`.

Эти значения являются контролируемыми QA inputs и не становятся публичным
контрактом или runtime configuration.

## Безопасность transport policy

- redirects явно запрещены;
- используется публичный target `https://hotels.tbank.ru/`;
- `Authorization`, JWT и private target не используются;
- request timeout берется из typed public target config;
- успешный ответ принимается только с JSON-compatible `Content-Type`;
- response body не входит в summary, исключения или диагностический результат;
- клиент всегда закрывается после выполнения harness.

`HotelsApiHttpResponse.contentType` содержит только media type успешного ответа.
Headers, cookies, URL parameters и response body дополнительно не раскрываются.

## Безопасный результат harness

Успешный результат содержит только:

- HTTP status;
- `Content-Type`;
- количество provider hotels и mapped offers;
- `isLoadingCompleted`;
- признак наличия `nextOffset`.

Hotel ids, названия, адреса, цены и полный provider body не возвращаются.
`nextOffset` читается только как metadata и не запускает pagination.

## Проверки

- [x] без `HOTELS_API_QA_ENABLED=true` клиент не создается;
- [x] неполные или некорректные inputs отклоняются до создания клиента;
- [x] mock request выполняется ровно один раз;
- [x] URL равен `https://hotels.tbank.ru/api/v1/hotels/search`;
- [x] request не содержит `Authorization`;
- [x] request содержит подтвержденные destination, dates, occupancy и bounded window;
- [x] redirect не выполняется и преобразуется в безопасную provider error category;
- [x] отсутствующий `Content-Type` сохраняется как `null`;
- [x] неожиданный media type отклоняется без раскрытия body;
- [x] текущие DTO и response mapper проверяются на sanitized fixture;
- [x] CIO client создается и закрывается без сетевого запроса;
- [x] все HTTP-проверки Stage 9.15a используют `MockEngine`.

## Границы

Stage 9.15a не добавляет:

- live API calls;
- `REAL` runtime wiring;
- вызов `RealHotelOfferProviderAdapter`;
- production network engine или production client factory;
- retries, polling, pagination или redirects;
- JWT, secrets, private API или auth changes;
- routes, public API, OpenAPI Travel Assistant, frontend или generated clients;
- durable storage, booking/payment flow или `bookHash`.

`FAKE` остается provider по умолчанию.

## Риски и неизвестные

- официальный server-to-server статус публичного endpoint не подтвержден;
- включение taxes/fees в `shownPrice` остается неизвестным;
- policy для `LIMITED` отсутствует;
- один QA call не подтверждает SLA, rate limits или долговременную стабильность.

Эти вопросы не заполняются предположениями и не блокируют сам изолированный
одиночный QA call после отдельного разрешения.

## Readiness verdict

`READY_FOR_SEPARATELY_AUTHORIZED_STAGE_9_16`.

Mock-only preflight завершен. Stage 9.16 может выполнить ровно один прямой
search request через этот test-scoped seam только после отдельного явного
разрешения. Текущий этап не выполнял network calls и не активировал Stage 9.16.

## Связанные документы

- [Stage 9.15 — sandbox readiness gate](stage-9-15-sandbox-readiness-gate.md)
- [Stage 9.14 — fixture verification](stage-9-14-sanitized-provider-fixture-contract-verification.md)
- [Stage 9.9 — public HTTP transport](stage-9-9-public-anonymous-hotels-api-http-transport.md)
- [Основной roadmap](../roadmap/roadmap.md)
