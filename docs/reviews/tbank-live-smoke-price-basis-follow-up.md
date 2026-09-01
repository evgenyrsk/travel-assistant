# Follow-up live smoke: профиль, price basis и booking preview

**Статус:** natural-language live acceptance completed.

**Версии после исправления:** Hotels MCP `0.23.4`, Banking/broker `0.14.1`,
local toolkit `0.6.6`.

## Scope

Разобрать естественные read-only/preview-only кейсы после восстановления
Hotels transport, закрыть подтверждённые ошибки композиции без новых provider
вызовов и без активации booking/payment mutations.

## Evidence

- Москва и Санкт-Петербург прошли через `plan_stay → compare_stay_options`;
- breakfast-фильтр применился без low-level перебора;
- rates и локальный booking preview прошли без PII и write-запроса;
- customer booking summary и Banking portfolio profile прошли через общий
  broker;
- первый поиск Казани завершился terminal timeout, повтор был выполнен только
  после нового явного пользовательского запроса и прошёл успешно.

## Findings

1. Модель вызвала `create_booking_preview` одновременно с rates до выбора
   `rateOptionId`, затем восстановилась через `select_stay_rate`.
2. В персонализированном поиске модель передала только `ranking=best_value`, но
   не `hotelPreferences`, после чего ошибочно заявила применение профиля.
3. Provider `shownPrice`, который проект маппит как total за выбранный период,
   сравнивался с `pricePerNight`-диапазоном. Для двух ночей это дало ложные
   `within/outside range` и пользовательские рекомендации.

## Исправление

- Banking profile возвращает готовый `hotelPreferences` и usage contract для
  передачи без преобразований;
- Hotels явно запрещает считать один `ranking=best_value` доказательством
  применения профиля; evidence — только `preferencesApplied.applied=true`;
- journey и rates разделяют provider total и MCP-derived цену за ночь;
- `best_value` и `pricePreferenceFit` используют цену за ночь, вычисленную из
  total и `stayNights`;
- comparison/rates Markdown содержит отдельные колонки «За поездку» и «За
  ночь»;
- tool guidance фиксирует порядок rates → select rate → preview и запрещает
  параллельный preview до выбора тарифа.

## Повторный live gate

- ordinary search, breakfast, sequential rates-preview, booking summary и
  portfolio profile прошли;
- multi-night total/per-night значения и `pricePreferenceFit` стали
  непротиворечивыми;
- `preferencesApplied.applied=true` вернулся для персонализированного поиска;
- выявлено, что `best_value_v1` награждал любую цену ниже нижней границы:
  вариант за `1 700 ₽/ночь` возглавил диапазон `6 000–13 000 ₽/ночь`;
- ordinary preview раскрыл пользователю внутреннее имя `x-real-ip`.

Hotels `0.23.3` вводит `best_value_v2`: максимальная price utility находится
внутри диапазона, а utility вариантов снаружи убывает с расстоянием. Сильное
отклонение больше не лидирует только из-за низкой цены. Journey preview
возвращает только безопасный публичный execution status без имён headers;
диагностические детали остаются в отдельном `connection_status`.

## Третий focused gate

`best_value_v2` прошёл: top-5 Казани целиком попал в диапазон, total/per-night
не смешивались, а far-below вариант не вытеснил релевантные отели. Однако
автоматически добавленный `countryName=Россия` дал пустой provider catalog;
модель затем сделала четыре лишних `resolve_destination` с вариантами Казань /
Kazan / Russia и только после этого повторила `plan_stay` с `destinationId`.

Hotels `0.23.4` выполняет bounded internal fallback: после пустого
country-filtered catalog загружает общий каталог, сопоставляет локализованное и
международное название страны локально и продолжает исходный `plan_stay`.
Clarification-ответ теперь явно запрещает автоматический model-side перебор.

## Проверки

- финальный offline gate для Hotels `0.23.4` / Banking `0.14.1` / toolkit
  `0.6.6` вне sandbox: toolkit `14/14`, Hotels `54/54`, Banking `49/49`,
  manifests/conformance pass;
- `contracts:check`, Hotels syntax check и Banking compile check: pass;
- documentation gate и `git diff --check`: pass;
- provider requests во время исправления: `0`.

## Следующий gate

Финальный repeat прошёл ровно через три ожидаемых tool calls: portfolio profile
→ один `plan_stay` → `compare_stay_options`. Ответ подтвердил
`preferencesApplied.applied=true`, `stayNights=2`, непротиворечивые total/per
night значения и top-5 внутри мягкого диапазона. Отдельных
`resolve_destination`, model-side retry и writes не было.

Неблокирующий P3 presentation issue: Markdown-таблица верно показала рейтинг
«Лампы» `8.9`, но модель в последующем пояснении приписала ей `9.2` и другое
название. Structured MCP facts корректны; это единичная ошибка пересказа модели,
не дефект поиска или ranking. Следующий шаг — финальный review release candidate,
а не ещё один live retry. Реальные mutations остаются запрещены.
