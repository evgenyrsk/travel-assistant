# Stage 9.18 — проверка закрытия Hotels API integration

## Роль документа

Это verification review Stage 9.18. Документ фиксирует фактически выполненные
проверки и обнаруженный blocker. Он не объявляет интеграцию закрытой. Текущий
статус задает [`docs/roadmap/roadmap.md`](../roadmap/roadmap.md).

## Цель

Проверить opt-in `REAL` composition после Stage 9.17c на regression, безопасные
route outcomes и одном контролируемом runtime smoke.

## Автоматизированная проверка

Добавлена runtime integration matrix через `MockEngine`:

- успешный autocomplete + search создает search и provider offers;
- отсутствие location возвращает `400 VALIDATION_ERROR` без search ID;
- несколько location candidates возвращают безопасный `400`;
- malformed response возвращает безопасный `500 INTERNAL_ERROR`;
- timeout и `503` возвращают безопасный `500`;
- provider body, internal category и `destinationId` не попадают в ошибки;
- confirmation failure не потребляет pending state;
- `FAKE` остается deterministic default.

Полный backend suite прошел. Для существующей diagnostic frontend формы прошли
`npm test`, `npm run lint` и `npm run build`.

## Контролируемый runtime smoke

17 июля 2026 года выполнен один opt-in flow через production runtime
composition:

- mode: `REAL`;
- public base URL: default `https://hotels.tbank.ru/`;
- синтетическое направление: `Иннополис`;
- синтетические будущие даты;
- без `Authorization`, cookies и пользовательских данных.

Результат: `400 VALIDATION_ERROR`; `HotelSearch` и `hotelSearchId` не созданы.
Полный response body и provider responses не публиковались. Повторный probe не
выполнялся.

По текущей цепочке такой результат означает, что location resolution не дало
ровно одного candidate: `LocationNotFound` либо
`LocationSelectionRequired`. Transport/search success, ранее подтвержденный
Stage 9.16 по известному `destinationId`, это наблюдение не опровергает.

## Обнаруженный blocker

Текущий orchestrator безопасно запрещает автоматический выбор первого
autocomplete результата. Однако direct и assistant flows не передают выбранный
location candidate обратно в runtime. Поэтому реальный search достижим только
если autocomplete случайно возвращает ровно один candidate.

До повторного runtime smoke нужно отдельно согласовать одну политику:

1. детерминированно выбирать единственный точный location match, например по
   нормализованному `name`/`signature` и допустимому `type.code`;
2. либо добавить явный пользовательский выбор candidate отдельным public
   contract этапом;
3. либо передавать предварительно разрешенный destination через другой
   application input, не раскрывая provider details.

Выбор первого элемента без точного правила остается запрещенным.

## Scope control

На Stage 9.18 не изменялись production code, public API, OpenAPI, frontend
source, provider DTO/mapper, pagination, retries или runtime configuration.
Секреты и новые network headers не добавлены.

## Verdict

`REGRESSION_PASSED_RUNTIME_SEARCH_BLOCKED_BY_LOCATION_SELECTION`.

Hotels API integration не закрыта. Stage 9.19 не должен начинаться до решения
location-selection boundary и успешного отдельно разрешенного повторного smoke.

## Рекомендуемый следующий шаг

Stage 9.18a — bounded location-selection reconciliation:

- выбрать policy на основании текущего autocomplete contract;
- реализовать ее отдельно с `MockEngine` tests;
- не раскрывать `destinationId` и не выбирать первый candidate;
- после реализации запросить разрешение на один повторный runtime smoke.
