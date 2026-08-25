# T-Bank MCP local toolkit

**Роль:** единая локальная точка setup, doctor, запуска и проверки контрактов
для отдельных Hotels и Banking MCP. Toolkit не объединяет их tool surface или
полномочия, не является MCP-сервером и не выполняет provider-запросы в
`setup`, `doctor`, `client-config` или `contracts`.

## Одноразовая настройка

Service JWT key остаётся отдельным owner-only PEM-файлом. В локальную
конфигурацию записываются только путь, approved API URL и несекретные JWT
metadata:

```bash
chmod 600 '/absolute/secure/path/hotels-rsa-key.pem'
node tools/tbank-mcp-local/src/cli.mjs setup \
  --profile combined \
  --hotels-api-base-url 'https://<approved-hotels-origin>/' \
  --hotels-jwt-key-file '/absolute/secure/path/hotels-rsa-key.pem'
```

Конфигурация сохраняется в `~/.config/tbank-mcp/config.json` с правами `0600`.
Mobile tokens в неё не копируются: Banking продолжает использовать отдельный
owner-only session store.

Телефонная авторизация выполняется один раз в обычном Terminal, без передачи
номера, SMS-кода, пароля или PIN модели и без записи номера в shell history:

```bash
node tools/tbank-mcp-local/src/cli.mjs login
```

Завершить локальную mobile session можно так же коротко:

```bash
node tools/tbank-mcp-local/src/cli.mjs logout
```

## Offline doctor

```bash
node tools/tbank-mcp-local/src/cli.mjs doctor --profile combined
```

Doctor проверяет версии runtime, локальные executables, наличие transport
configuration, key/session files и broker socket. Он не создаёт JWT, не читает
содержимое session в отчёт, не вызывает Hotels или Banking API и не печатает
секреты.

## Подключение клиентов

Toolkit выдаёт готовую secret-free регистрацию. Сгенерированные команды всегда
запускают отдельные MCP-процессы через общий локальный launcher:

```bash
node tools/tbank-mcp-local/src/cli.mjs client-config --client opencode --profile combined
node tools/tbank-mcp-local/src/cli.mjs client-config --client codex --profile combined
node tools/tbank-mcp-local/src/cli.mjs client-config --client claude --profile combined
```

Допустимые профили: `hotels`, `banking`, `combined`. В сгенерированной combined-
конфигурации launcher любого из двух MCP при необходимости поднимает один общий
локальный broker. Поэтому результат не зависит от того, какой MCP клиент
запустил первым; закрытие отдельного MCP не обрывает общую mobile session.
Ручной запуск для обычной работы не нужен.
Старый combined config также поддерживается: launcher распознаёт сохранённую
общую mobile-session конфигурацию без обязательного нового флага.

Broker завершается автоматически при `logout`. Для диагностики и явной
остановки без удаления mobile session доступны команды:

```bash
node tools/tbank-mcp-local/src/cli.mjs run broker
node tools/tbank-mcp-local/src/cli.mjs stop-broker
```

## Versioned contracts

```bash
npm --prefix tools/tbank-mcp-local run contracts:update
npm --prefix tools/tbank-mcp-local run contracts:check
```

Manifest export запускает MCP только до `initialize` и `tools/list` в очищенном
окружении. Provider routes не вызываются. Любое изменение tool name, schema,
description или annotations становится видимым diff.

`npm test` запускает полный offline gate. В его conformance-части оба MCP
запускаются дважды и проверяются `initialize`, `tools/list`, `ping`, newline
framing, чистый `stdout`, EOF shutdown и стабильный контракт после restart.

Одна команда выполняет весь локальный release gate: unit/protocol tests Hotels,
Banking и toolkit, manifests и conformance. Родительские `TBANK_*` credentials
изолируются, provider API не вызывается:

```bash
npm --prefix tools/tbank-mcp-local run verify
```

Текущую границу готовности реальной hotel payment можно проверить отдельно:

```bash
node tools/tbank-mcp-local/src/cli.mjs payment-readiness
```

Команда не читает credentials и не обращается к provider. Она показывает
закрытые локальные gates, недостающие contract/security evidence и действия,
которые остаются запрещены. Пока `readyForPaymentExecution=false`, нельзя
вызывать Hotels payment setup, переиспользовать банковский `/v1/pay` как оплату
отеля или автоматически повторять операцию после неизвестного исхода.

Естественные пользовательские сценарии для последующего bounded smoke собраны
в [`docs/human-smoke-cases.md`](docs/human-smoke-cases.md), а независимый
release-review — в [`docs/qwen-release-review-prompt.md`](docs/qwen-release-review-prompt.md).

## Безопасное изучение booking fixture

Если уже имеется JSON-ответ по собственной брони, его не нужно копировать в
репозиторий, prompt или чат. Toolkit может локально построить отчёт только о
структуре полей и JSON-типах:

```bash
node tools/tbank-mcp-local/src/cli.mjs inspect-booking-fixture \
  --input '/absolute/private/path/booking.json' \
  --output '/absolute/private/path/booking-shape.json'
```

Команда не выполняет provider-запросы, не выводит значения, консервативно
заменяет похожие на идентификаторы динамические object keys и создаёт отчёт с
правами `0600`. Маскирование ключей эвристическое и явно отмечено в
`limitations`; отчёт перед передачей всё равно должен просмотреть владелец.
`observedInEveryObject` описывает только наблюдаемую форму fixture и не доказывает
обязательность поля в provider contract. Из отчёта также нельзя автоматически
делать вывод о payment semantics.

Исходный JSON следует хранить вне репозитория. Для дальнейшего contract review
передаётся только созданный `booking-shape.json`, предварительно просмотренный
владельцем данных.

Если исходного файла нет, отдельная команда может по явному подтверждению
прочитать первую собственную бронь выбранной категории через общий auth broker и
сразу сохранить только structure-only отчёт:

```bash
node tools/tbank-mcp-local/src/cli.mjs capture-booking-shape \
  --category active \
  --output '/absolute/private/path/booking-shape.json' \
  --acknowledge-read-own-data
```

Допустимые категории: `active`, `completed`, `cancelled`. Команда выполняет два
ограниченных hotel read-запроса — список нужной категории и карточку первой
записи; при необходимости auth broker также может обновить mobile session. Raw
payload существует только в памяти процесса, не сохраняется и не печатается.
Команда не выполняет booking/payment setup, мутации или оплату и не является
MCP-tool.

## Capability tiers

| Tier | Hotels | Banking | Статус |
| --- | --- | --- | --- |
| `hotels_read` | Search, compare, rates facts | — | Local experimental GO |
| `customer_read` | Собственные customer/booking reads через broker | Broker provider | Local experimental GO только для allowlist |
| `banking_read` | — | Accounts, spending aggregates, travel profile | Local experimental GO |
| `preview_only` | Booking preview | Payment intent preview | Local GO без HTTP writes |
| `booking_execute` | Booking/cancel/payment setup writes | — | NO-GO |
| `payment_execute` | — | Реальное списание | NO-GO |

SemVer policy: additive optional fields, новые commands и новые tools требуют
minor release; совместимые diagnostics/documentation fixes — patch;
удаление/переименование tool или required field — major release с migration note. До первого
публикуемого release manifests остаются working-tree compatibility gate.
