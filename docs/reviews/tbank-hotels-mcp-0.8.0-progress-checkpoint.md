# T-Bank Hotels MCP 0.8.0 — checkpoint прогресса

**Дата:** 2026-08-21  
**Роль:** completed progress checkpoint / handoff  
**Активный план:** `tools/tbank-hotels-mcp/docs/journey-tools-plan.md`

## 1. Границы checkpoint

В scope входят API-driven MCP под `tools/tbank-hotels-mcp`, его tool schemas,
process-local journey flow, auth/readiness diagnostics, safety guards, тесты и
tool-local документация.

Вне scope остаются core backend/frontend Travel Assistant, основной product
roadmap, browser automation, хранение пользовательской сессии, данные карты,
production activation и реальные booking/payment/cancel запросы.

## 2. Текущее состояние

| Область | Статус | Основание |
| --- | --- | --- |
| MCP transport | `GO` | stdio, Node.js 20+, browser dependency отсутствует |
| Search/compare | `GO` для read-only QA | Typed journey contract, bounded pagination/partial results, локальный ranking |
| Rates | `GO` для read-only QA | Даты/гости переносятся из journey; один timeout retry в общем бюджете |
| Safe booking preview | `GO` | Preview без PII, draft, `bookHash` и HTTP-вызова |
| Customer reads | `CONDITIONAL` | Требуется подтверждённый customer auth profile |
| Booking preparation с PII | `CONDITIONAL` | Локальный draft/checkout реализован, но execution contracts не закрыты |
| Реальные mutations | `NO-GO` | Не подтверждены customer auth, `x-real-ip`, idempotency и timeout recovery |

Версия checkpoint: `0.8.0`. Автоматизированный suite содержит 32 fake-transport
теста и не обращается к Hotels API.

## 3. Реализованный функциональный срез

- Natural-language agent flow скрывает `destinationId`, provider search DTO,
  `hotelId` и rates DTO на основном пути.
- Search собирает bounded provider-выборку, показывает completeness metadata и
  ранжирует локально без неподдерживаемого provider sort.
- Пустой `rates` возвращает `no_bookable_rates`; search-feed цена не становится
  фиктивным бронируемым тарифом.
- Timeout rates повторяется один раз внутри tool. После исчерпания бюджета
  возвращается `rates_temporarily_unavailable` с диагностикой без ручного retry
  со стороны LLM.
- `tbank_hotels_create_booking_preview` показывает выбранный stay/rate и
  occupancy без сбора ФИО, email и телефона.
- Journey/draft TTL, checkout freshness, prepare/execute confirmation,
  single-flight и `outcome_unknown` защищают process-local flow от stale/replay
  и неизвестного результата повторного write.
- `connection_status` раздельно показывает search, customer и booking
  readiness, не раскрывая URL, токены, ключи и headers.
- Реальные mutations выключены по умолчанию и не считаются готовыми только по
  наличию service JWT.

## 4. Проверенные пользовательские сценарии

Предыдущий read-only smoke подтвердил поиск Москвы, сравнение пяти вариантов,
выбор отеля и загрузку тарифов на версии `0.7.x`. Он также выявил два edge case:

1. HTTP 200 с пустым `rates` — закрыт в `0.7.1`.
2. Повторные timeout rates и избыточный запрос PII для preview — закрыты в
   `0.8.0`.

После обновления MCP-клиента требуется повторить smoke именно на `0.8.0`.
Реальные booking/payment/cancel действия в проверках не выполнялись.

## 5. Safety и privacy

- Secrets поступают только через окружение процесса и не являются tool
  arguments.
- Test subprocess не наследует `TBANK_HOTELS_*` родительского shell.
- Provider errors не возвращают raw response body; response size и redirects
  ограничены.
- Safe preview не собирает PII и не выдаёт `bookHash`.
- PII допускается только в booking draft после явного намерения пользователя
  оформить реальную бронь; preview остаётся redacted.
- `x-real-ip` не запрашивается у пользователя и не генерируется MCP.

## 6. Открытые блокеры

- Customer auth profile для чтения заказов и mutations.
- Доверенный источник и точная семантика `x-real-ip`.
- Provider idempotency/deduplication и reconciliation после timeout booking
  create.
- JWT `exp`/`nbf`/`kid`, rotation и clock-skew policy.
- Date/timezone contract, роль `sessionId`, public/private routing.
- Разрешённый non-production sandbox и тестовые данные без денежных последствий.

## 7. Дальнейший порядок

1. Перезапустить MCP-клиент и выполнить read-only smoke версии `0.8.0`.
2. Провести независимый review кода, schemas, тестов и обезличенного smoke.
3. Получить ответы владельца Hotels API по auth/header/idempotency contracts.
4. Проверить переносимость stdio-конфигурации в OpenCode, Codex CLI и Claude
   Code.
5. Только после закрытия внешних gates подготовить отдельный разрешённый
   non-production mutation smoke plan.

Детальные проверяемые критерии находятся в активном tool-local плане. Этот
checkpoint является audit trail и не заменяет roadmap или список дальнейших
задач.

## 8. Проверки checkpoint

- [x] `node --check tools/tbank-hotels-mcp/src/server.mjs`
- [x] `npm test --prefix tools/tbank-hotels-mcp` — 32/32
- [x] `git diff --check`
- [x] Реальные Hotels API mutations не выполнялись
- [x] Локальные contract `.txt`, `.env` и значения секретов не включены
