# Stage 12.6 — согласование платформонезависимого ответа

## Цель

Сделать результат повторного hotel search понятным любому HTTP-клиенту:
вернуть активные предпочтения поиска и подтверждённые факты о предложениях,
не раскрывая DTO Hotels API и не добавляя новые endpoint-ы.

## Публичный контракт

`GET /api/v1/hotel-searches/{searchId}/offers` дополнен необязательными полями:

- `appliedPreferences` на уровне результата поиска;
- `starRating` на уровне предложения;
- `freeCancellationUntil` на уровне предложения.

`appliedPreferences` содержит только активные ограничения: максимальную общую
стоимость, звёзды, минимальный гостевой рейтинг и требование бесплатной отмены.
Сумма возвращается точной десятичной строкой вместе с валютой `RUB`, чтобы
публичный ответ не вносил погрешность floating-point. Звёзды упорядочиваются по
возрастанию.

Пустой набор предпочтений не создаёт `appliedPreferences`. Неизвестные
`starRating` и `freeCancellationUntil` также отсутствуют в JSON. Отсутствие
поля не трактуется как ноль, отсутствие звёзд или невозможность бесплатной
отмены.

## Границы данных

- `starRating` остаётся категорией отеля и не подменяет гостевой `rating`.
- `freeCancellationUntil` передаёт только известный срок бесплатной отмены.
- `shownPrice` по-прежнему означает provider total за весь период без
  перерасчёта; включение налогов и сборов остаётся неизвестным.
- Provider filter IDs, polymorphic DTO, `destinationId`, `bookHash` и raw
  response не входят в public API.
- `appliedPreferences` отражает критерии конкретного сохранённого поиска;
  предыдущий `hotelSearchId` сохраняет прежний набор.

## Demo shell

Локальная demo shell по-прежнему показывает не более пяти предложений из
backend-ранжированного пула. Карточка отображает звёзды и срок бесплатной
отмены только при наличии соответствующих полей. Над результатами показывается
краткое перечисление активных предпочтений. Filter panel, локальное
переранжирование и повторный provider request во frontend не добавлены.

## OpenAPI и проверки соответствия

OpenAPI draft описывает новые поля как необязательные и запрещает неизвестные
поля внутри `appliedPreferences`. Проверка conformance контролирует:

- необязательность трёх новых частей ответа;
- точный набор четырёх preference fields;
- сохранение закрытой schema без `additionalProperties`;
- прежний ограниченный набор platform-client endpoint-ов.

Статус всего OpenAPI остаётся `not_ready`; готовность generated clients не
заявляется.

Выполнены:

- точечные backend-тесты API mapping и REAL adapter route integration через
  `MockEngine`;
- полный backend test suite;
- `npm test` и `npm run check` для OpenAPI conformance;
- frontend tests, lint и build;
- `git diff --check` и проверка границ diff.

## Вне этапа

Не добавлены новые endpoint-ы, provider calls, filters, пользовательская
сортировка, pagination, `search-filters-availability`, auth, durable storage,
SDK, product clients, booking или payment. Search orchestration, confirmation
lifecycle и provider runtime не менялись. Live-вызовы не выполнялись.

## Следующий этап

Stage 12.7 должен отдельно обработать успешный поиск без предложений и
предлагать ослабить одно активное ограничение за раз. Автоматическое применение
ослабления и подключение `search-filters-availability` не разрешены этим
этапом.

## Verdict

`PASS_STAGE_12_6_PLATFORM_NEUTRAL_RESPONSE_ALIGNMENT`.

Подтверждённые offer facts и применённые preferences доступны через прежний
platform-neutral endpoint без утечки provider contract и без изменения
поискового runtime.
