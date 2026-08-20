# Follow-up по ревью T-Bank Hotels MCP

**Роль документа:** review/audit artifact. Документ фиксирует результат
`review-only` аудита `qwen3.8-max`, предоставленный владельцем проекта
2026-08-20, и его первичную инженерную классификацию. Он не меняет основной
roadmap Travel Assistant и сам по себе не разрешает реальные вызовы booking,
payment, cancel или других mutating endpoints.

## 1. Scope

Аудит рассматривал текущий working tree `tools/tbank-hotels-mcp`, локальные
OpenAPI-контракты v1–v3 и запуск тестов в двух режимах окружения. Секреты не
читались, реальные мутации не были успешно выполнены.

Важное наблюдение: подключённый к CLI MCP-процесс использовал более старую
ревизию, чем working tree. После изменения `server.mjs` MCP-клиент необходимо
полностью перезапускать перед smoke-test.

## 2. Итог аудита

| Поверхность | Вердикт | Условие |
| --- | --- | --- |
| Read-only search | `GO` | После перезапуска клиента и повторной проверки версии `0.3.0` |
| Authenticated reads | `CONDITIONAL GO` | Требуется подтвердить customer context для заказов и voucher |
| Booking preparation | `CONDITIONAL GO` | Требуется закрыть typed rates input, PII redaction и contract gaps |
| Real mutations | `NO-GO` | Нужны auth/body/idempotency contracts и отдельное явное разрешение |

## 3. Triage findings

Статус `Принято` означает, что пункт включён в tool-local follow-up plan. Он не
означает, что исправление уже реализовано.

| ID | Finding | Приоритет | Решение |
| --- | --- | --- | --- |
| F-01 | Тестовый subprocess наследует `TBANK_HOTELS_*` и может обратиться к реальному API | P1 / safety gate | Принято, исправить первым |
| F-02 | `search_seo` использует одну v1-схему при default v3 | P1 | Принято |
| F-03 | Journey rates требует ручной provider payload и не подставляет даты | P1 | Принято |
| F-05 | `payment_setup` требует body, которого нет в доступном контракте | P2 | Принято, mutating остаётся `NO-GO` |
| F-06 | Имена гостей не скрываются в preview | P2 / privacy | Принято |
| F-07 | Resolver не обходит страницы location catalog | P2 | Принято после проверки pagination semantics |
| F-08 | Prepare confirmation не имеет TTL и provider idempotency неизвестна | P2 / safety | Локальный TTL принят; retry policy зависит от контракта |
| F-09 | Mutating tools сохраняют generic payload при неполных body-схемах | P2 | Заблокировано контрактами; tools должны быть помечены experimental/NO-GO |
| F-10 | JSON-RPC batch не обрабатывается | P3 | Принято |
| F-11 | `ping` не реализован | P3 | Принято |
| F-12 | Price ranking не учитывает валюту | P3 | Принято как защитное улучшение |
| F-13 | Граница «сегодня» вычисляется по UTC | P3 | Отложено до решения timezone contract |
| F-14 | Date-only работает фактически, но OpenAPI описывает date-time | Contract gap | Уточнить у владельца API |
| F-15 | Raw provider responses могут раздувать LLM context | P3 | Принято для bounded response design |

## 4. Блокирующие contract gaps

- [ ] Подтверждён auth profile для customer reads и каждого mutating endpoint.
- [ ] Объяснён `401` booking create при working service JWT.
- [ ] Предоставлены body-схемы booking list, LS create, payment setup,
  promocode apply, extra services и tranches.
- [ ] Подтверждены idempotency/retry semantics booking create после timeout.
- [ ] Подтверждён формат hotel search dates: date-only или date-time.
- [ ] Уточнена роль `sessionId` в `getReservation` без browser session.
- [ ] Подтверждены JWT `exp`/`nbf`/`kid`, clock skew и key rotation.
- [ ] Уточнены pagination semantics `/api/v1/seo/locations`.
- [ ] Подтверждена маршрутизация public/private origins и timeout policy.

## 5. Safety decision

До закрытия F-01 обычный запуск `npm test` в shell с экспортированными
`TBANK_HOTELS_*` считается небезопасным. До исправления тесты допускаются только
в гарантированно очищенном окружении. После исправления критерий gate:

- [ ] subprocess получает только allowlisted process variables и явно заданный
  test env;
- [ ] hostile parent env не попадает в child;
- [ ] полный suite проходит без внешнего DNS/HTTP;
- [ ] mutating fake-test не может достичь реального API даже при ошибке setup.

## 6. Связанные документы

- Активный tool-local план: `tools/tbank-hotels-mcp/docs/journey-tools-plan.md`.
- Инструкция MCP: `tools/tbank-hotels-mcp/README.md`.
- Промпт, использованный как основа аудита:
  `tools/tbank-hotels-mcp/docs/mcp-review-prompt.md`.

## 7. Статус реализации follow-up на 2026-08-20

Этот раздел фиксирует состояние после аудита, не переписывая исходный triage.

| Findings | Статус после исправлений |
| --- | --- |
| F-01, F-02, F-03, F-05, F-06 | Закрыты кодом и regression tests |
| F-07 | Закрыт bounded pagination каталога с защитным лимитом |
| F-08 | Локальный TTL подтверждения реализован; provider idempotency остаётся contract gap |
| F-09 | Закрыт для доступной OpenAPI: обычная и LS-бронь, booking list, cancel, payment setup, promocode, extra services, tranches и BNPL типизированы; auth/required headers остаются отдельным contract gap |
| F-10, F-11, F-12, F-15 | Закрыты кодом и regression tests |
| F-13, F-14 | Отложены до подтверждения timezone/date contract владельцем API |

Safety gate F-01 пройден: test subprocess использует allowlist окружения,
hostile parent `TBANK_HOTELS_*` не наследуется, provider transport в suite
заменён fake-реализациями. Runtime версии `0.6.0` также блокирует все реальные
mutations по умолчанию; активация требует отдельного
`TBANK_HOTELS_ENABLE_MUTATIONS=true`.

Дополнительная проверка после аудита показала расхождение OpenAPI и runtime:
production search ответил `sorting_is_not_allowed_yet`, поэтому provider sort
не отправляется, а ranking выполняется локально. Search journey сохраняет
bounded collection по `nextOffset`, ограниченный polling partial response,
deduplication, общий 11-секундный бюджет и явный `searchCoverage`. Ranking
следующих journey-вызовов наследуется из `plan_stay`; явный ranking применяется
ко всей journey-выборке, а не к выбранным LLM `optionIds`. Поведение покрыто
fake-transport regression tests; mutating Hotels API запросы не выполнялись.

Отдельный end-to-end разбор длительного booking preview выявил истечение
15-минутного journey, ручной повтор checkout после timeout/короткого freshness
окна, бесполезный customer `401` под `service_jwt` и запрос подтверждения при
выключенных mutations. В `0.6.0` journey и draft получили независимый TTL 60
минут, checkout — freshness 5 минут и один внутренний retry, customer preflight
стал локальным, а выключенные mutations дают только `preview_only` без
confirmation hash/phrase. `confirm_booking` проверяет activation gate первым.
Поведение покрыто fake-transport тестами; Hotels API не вызывался.

Оставшийся `NO-GO` относится к реальным booking/payment/cancel операциям, а не
к read-only search journey. Следующий внешний шаг — ответы владельца API по
customer auth, обязательным `x-real-ip`/customer headers, `401` booking create,
idempotency/timeout recovery, JWT claims, date/timezone, `sessionId` и
public/private routing.

## 8. Follow-up после повторного аудита версии 0.6.0

Повторный review подтвердил исправления preview-flow и выявил ложную execution
readiness, отсутствие single-flight confirm, несогласованность generic prepare,
устаревший runtime note, отсутствие общего бюджета location catalog и ranking
цен без известной валюты. В версии `0.7.0` локально закрыты эти пункты:

- `connection_status` разделяет search, customer и booking readiness;
- booking/LS требуют настроенный trusted `x-real-ip`, но MCP не принимает его
  от модели и не выдумывает источник;
- все prepare возвращают `preview_only` без confirmation/hash при неготовом
  execution profile, а execute проверяют readiness первыми;
- journey confirm использует single-flight и блокирует повтор после
  `outcome_unknown`;
- location catalog ограничен общим 10-секундным бюджетом;
- неизвестная валюта блокирует `lowest_price` ranking;
- TTL/retry/concurrency/header поведение покрыто fake-transport тестами.

Уточнение аудита: `x-real-ip` обязателен в доступном OpenAPI для обычного и
LS booking create, но не указан для `payment/setup`. Реальные mutations остаются
`NO-GO` до подтверждения источника header, customer auth, idempotency и timeout
recovery владельцем Hotels API.

Smoke-test версии `0.7.0` выявил отдельный agent UX edge: provider может вернуть
HTTP 200 с пустым обязательным массивом `rates`. Версия `0.7.1` возвращает в
этом случае `no_bookable_rates` / `canCreateBookingDraft=false`, запрещает
запрашивать guest PII и поясняет, что search-feed цена не является выбираемым
тарифом без `rateOptionId`/`bookHash`. Rates-запрос ограничен 13 секундами.

## 9. Checkpoint версии 0.8.0

Последующий smoke подтвердил корректный `no_bookable_rates`, но показал ручные
повторы rates-вызова моделью и избыточный запрос guest PII для локального
preview при `bookingExecution.available=false`.

В `0.8.0` добавлен отдельный `tbank_hotels_create_booking_preview`, который
возвращает выбранный stay/rate и occupancy без PII, `bookHash`, booking draft и
HTTP-вызова. `get_selected_stay_rates` повторяет один timeout внутри общего
13-секундного бюджета; после его исчерпания возвращает структурированный
`rates_temporarily_unavailable` и запрещает автоматический повтор LLM.

Текущий handoff, safety gates и дальнейший порядок сохранены в
`docs/reviews/tbank-hotels-mcp-0.8.0-progress-checkpoint.md`. Исполнимый план
остаётся только в `tools/tbank-hotels-mcp/docs/journey-tools-plan.md`.
