# T-Bank MCP local toolkit

> Неофициальный developer preview. Booking/payment execution отключён, а
> mobile login выполняется только локально вне MCP/LLM.

**Роль:** единая локальная точка setup, doctor, запуска и проверки контрактов
для отдельных Hotels и Banking MCP. Toolkit не объединяет их tool surface или
полномочия, не является MCP-сервером и не выполняет provider-запросы в
`setup`, `doctor`, `client-config` или `contracts`.

## Установка и подключение одной командой

Команда сама устанавливает Hotels + Banking в приватный локальный runtime,
регистрирует оба MCP и запускает мобильный вход в терминале.

### Cursor

```bash
npx -y tbank-mcp-local@0.14.1 connect cursor
```

Завершите вход в терминале и полностью перезапустите Cursor. Откройте Agent и
пишите обычным языком, например: «Найди отели в Казани на выходные».

### Codex

```bash
npx -y tbank-mcp-local@0.14.1 connect codex
```

Завершите вход в терминале и перезапустите Codex. Проверить подключение можно
командой `codex mcp list`, после чего задавайте обычные запросы в Codex.

Номер телефона, SMS-код, пароль и PIN вводятся непосредственно в терминале и не
передаются модели. Для OpenCode используется тот же короткий формат:

```bash
npx -y tbank-mcp-local@0.14.1 connect opencode
```

Для анонимного поиска отелей без Banking и мобильной авторизации добавьте
`--profile hotels`, например:

```bash
npx -y tbank-mcp-local@0.14.1 connect cursor --profile hotels
```

Автоустановка поддерживает macOS/Linux, Node.js 20+ и Python 3.11+ для
combined/Banking профиля. Она не требует глобального npm/pip install или
repository checkout. Профили `hotels`, `banking` и `combined` сохраняют
раздельные MCP tool surface; combined лишь переиспользует один локальный auth
broker.

## Ручная настройка для разработки

Для разработки команды ниже можно запускать как
`node tools/tbank-mcp-local/src/cli.mjs`. Установленный toolkit предоставляет
эквивалентную команду `tbank-mcp-local`; дальнейшие примеры используют её.
Toolkit ищет `tbank-hotels-mcp`, `tbank-banking-mcp`, `tbank-auth-broker` и
`tbank-banking-login` в `PATH`, а внутри repository checkout сохраняет
development fallback. При нестандартной установке допустимы только явные
абсолютные executable overrides `TBANK_MCP_HOTELS_EXECUTABLE`,
`TBANK_MCP_BANKING_EXECUTABLE`, `TBANK_MCP_BROKER_EXECUTABLE` и
`TBANK_MCP_LOGIN_EXECUTABLE`.

Для developer-preview anonymous search достаточно transport URL:

```bash
tbank-mcp-local setup \
  --profile combined \
  --hotels-api-base-url 'https://hotels.tbank.ru/api'
```

Если владелец интеграции выдал service JWT key, его можно добавить как
необязательный owner-only PEM-файл. В локальную конфигурацию записывается только
путь, а не ключ:

```bash
chmod 600 '/absolute/secure/path/hotels-rsa-key.pem'
tbank-mcp-local setup \
  --profile combined \
  --hotels-api-base-url 'https://hotels.tbank.ru/api' \
  --hotels-jwt-key-file '/absolute/secure/path/hotels-rsa-key.pem'
```

Конфигурация сохраняется в `~/.config/tbank-mcp/config.json` с правами `0600`.
Mobile tokens в неё не копируются: Banking продолжает использовать отдельный
owner-only session store.

Если для Hotels существует секция в local config, launcher считает её
каноничным transport/auth-профилем: старый inline PEM, token, auth headers,
`TBANK_HOTELS_ENABLE_MUTATIONS` или mutation execution profile из shell/`.env`
не могут конфликтовать с
key-file и не требуют ручного `unset`. Без key-file используется anonymous
read-only search. Явный `TBANK_HOTELS_API_BASE_URL`
остаётся transport override, чтобы launcher не подменял уже проверенный origin
устаревшим URL из local config. При отсутствии override используется URL из
local config. Полностью environment-driven Hotels сохраняется для запуска без
local config.

Телефонная авторизация выполняется один раз в обычном Terminal, без передачи
номера, SMS-кода, пароля или PIN модели и без записи номера в shell history:

```bash
tbank-mcp-local login
```

Завершить локальную mobile session можно так же коротко:

```bash
tbank-mcp-local logout
```

## Offline doctor

```bash
tbank-mcp-local doctor --profile combined
```

Doctor проверяет версии runtime, локальные executables, наличие transport
configuration, key/session files и broker socket. Он не создаёт JWT, не читает
содержимое session в отчёт, не вызывает Hotels или Banking API и не печатает
секреты.

## Ручное подключение клиентов

Toolkit выдаёт готовую secret-free регистрацию. Текущая acceptance matrix
включает Cursor, OpenCode и Codex CLI; генератор Claude Code сохраняется как
необязательный профиль, но его подключение не требуется. Сгенерированные команды всегда
запускают отдельные MCP-процессы через общий локальный launcher:

```bash
tbank-mcp-local client-config --client opencode --profile combined
tbank-mcp-local client-config --client codex --profile combined
tbank-mcp-local client-config --client claude --profile combined
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
tbank-mcp-local run broker
tbank-mcp-local stop-broker
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
изолируются, regression-тест проверяет конфликт stale inline PEM с каноничным
key-file, provider API не вызывается:

```bash
npm --prefix tools/tbank-mcp-local run verify
```

Gate также проверяет два installable artifact candidates без сети:

- Hotels npm tarball содержит только `package.json`, `README.md` и runtime
  server, устанавливается во временный каталог вне checkout и отвечает на
  `initialize` через установленную bin-команду;
- Banking wheel собирается из изолированной копии, не содержит session/env/test
  файлов, устанавливается вне checkout и отвечает на `initialize`.

Совместимый набор публикации: `tbank-hotels-mcp@0.28.1`,
`travel-assistant-tbank-banking-mcp@0.17.0` и `tbank-mcp-local@0.14.1`.
Автоматический `connect` устанавливает их в owner-only runtime и закрепляет
абсолютные executable paths в локальном конфиге, поэтому глобальные npm/pipx
команды и repository checkout не нужны. Npm-пакеты опубликованы с
`UNLICENSED`: это разрешает установку preview из registry, но не предоставляет
отдельную open-source лицензию на повторное использование кода. До полного
stable release остаются выбор публичной лицензии, SBOM/provenance и remote
Streamable HTTP transport. Локальные Cursor/OpenCode/Codex подключения работают через
stdio; ChatGPT web/mobile не может подключить такой локальный процесс напрямую.

Текущую границу готовности реальной hotel payment можно проверить отдельно:

```bash
node tools/tbank-mcp-local/src/cli.mjs payment-readiness
```

Команда не читает credentials и не обращается к provider. Она показывает
закрытые локальные gates, недостающие contract/security evidence и действия,
которые остаются запрещены. Пока `readyForPaymentExecution=false`, нельзя
вызывать Hotels payment setup, переиспользовать банковский `/v1/pay` как оплату
отеля или автоматически повторять операцию после неизвестного исхода.

После офлайн-сверки `HotelsApi.Payments` отчёт версии `2.0` фиксирует hosted
payment form как целевой публичный маршрут и считает request/response schemas и
payment task states подтверждёнными. Raw-card, fingerprint и 3-D Secure
endpoints исключены из MCP. Оставшиеся blockers относятся к внешнему origin,
customer access, trusted client IP, idempotency/reconciliation, owner-bound
выдаче payment link и явному non-production разрешению.

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
