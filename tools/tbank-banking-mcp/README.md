# T-Bank Banking MCP

> Неофициальный developer preview. Пакет использует неофициальный mobile API,
> не заявляет одобрение или поддержку со стороны Т-Банка и не экспортирует
> реальные платежи или переводы.

Отдельный experimental MCP для локальной авторизации по номеру телефона,
read-only банковских агрегатов и персонализации поиска отелей. Он подключается
к MCP-клиенту рядом с `tbank-hotels-mcp` и не расширяет полномочия Hotels MCP.

## Текущая готовность

| Возможность | Статус |
| --- | --- |
| Авторизация по телефону + SMS/password/PIN | Доступна через локальный CLI вне LLM |
| Обновление mobile session | Доступно через MIT-derived upstream client |
| Список счетов | Experimental read-only |
| Категории расходов за 30–366 дней | Experimental read-only |
| Обезличенный portfolio travel profile за 30–366 дней | Доступен без идентификаторов счетов и абсолютных сумм |
| Hotel payment preview/handoff | Доступен локально, `preview_only` |
| Общая mobile session для Banking/Hotels | Доступна через опциональный локальный auth broker |
| Mobile auth для Hotels customer reads | `customerdata`, `booking_list` и `booking_v1` подтверждены и доступны Hotels MCP через broker |
| Ваучер собственной брони | Безопасная локальная выдача PDF через broker; содержимое не попадает в MCP JSON |
| Реальное списание/перевод | Не экспортируется и не активировано |

`connection_status.paymentExecution` является fail-closed readiness-отчётом.
Он отдельно перечисляет уже закрытые локальные гарантии и неподтверждённые
provider/security gates. Пока хотя бы один blocker остаётся, setup и execution
не считаются доступными; после timeout с неизвестным исходом автоматический
повтор запрещён.

Mobile API является неофициальным capture-driven контрактом. Пакет нельзя
считать production-ready только потому, что локальная авторизация прошла.
Provenance и лицензия описаны в [UPSTREAM.md](UPSTREAM.md).

Большой vendored `upstream/client.py` сохранён как совместимая реализация
mobile login/refresh, но MCP server и auth broker больше не получают этот
объект целиком. `CuratedMobileSession` предоставляет runtime только шесть
allowlisted read-операций; payment, transfer, marketplace, messenger,
login/OTP и credential-поля через обычный runtime object graph недоступны.
Raw session используется только локальным login CLI и явно запускаемым
read-only auth probe вне MCP.

## Установка

Нужен Python 3.11+:

```bash
pipx install travel-assistant-tbank-banking-mcp
```

Для обычного пользователя рекомендуется единая автоустановка Hotels + Banking,
которая сама создаёт изолированный Python runtime и подключает оба MCP:

```bash
npx -y tbank-mcp-local@0.13.1 connect --client opencode --profile combined
```

Для Codex замените `opencode` на `codex`. Телефонный вход запускается внутри
этой команды, но остаётся обычным terminal flow вне MCP/LLM.

Editable install нужен только для разработки из checkout:

```bash
cd /absolute/path/to/travel-assistant/tools/tbank-banking-mcp
python3 -m venv .venv
.venv/bin/pip install -e .
```

Устанавливаемый wheel дополнительно предоставляет четыре обычные команды:
`tbank-banking-mcp`, `tbank-auth-broker`, `tbank-banking-login` и
`tbank-hotels-mobile-auth-probe`. Поэтому после registry-публикации или установки
локально собранного wheel checkout репозитория для запуска не нужен.

Для постоянного standalone/combined подключения используйте общий local
toolkit: [`../tbank-mcp-local/README.md`](../tbank-mcp-local/README.md). Он не
копирует mobile tokens, не объединяет полномочия серверов и генерирует
secret-free регистрацию для OpenCode, Codex CLI и Claude Code.

## Вход по телефону

Вход выполняется напрямую в терминале, а не MCP-tool вызовом:

```bash
tbank-banking-login
```

SMS-код, пароль и PIN вводятся скрыто. Они не передаются модели. По умолчанию
сессия сохраняется в
`~/.local/share/tbank-banking-mcp/session.json` атомарно с правами `0600`.
Другой путь можно задать переменной `TBANK_BANKING_SESSION` одновременно для
login CLI и MCP-процесса.

Сессия содержит действующие access/refresh/mobile tokens и cookies. Не
копируйте её в репозиторий, чат, MCP config или prompt. Для production-класса
хранения нужен OS Keychain или корпоративный secret store.

Локальный выход сначала останавливает общий broker, затем удаляет сохранённые credentials, но не заявляет отзыв сессии на
стороне банка, потому что официальный revoke/logout endpoint не подтверждён:

```bash
tbank-banking-login --logout
```

После logout перезапустите MCP-клиенты.

## Общая авторизация для двух MCP

Каждый MCP может подключаться отдельно. Banking MCP по умолчанию продолжает
работать напрямую с session-файлом. Для совместного подключения используйте
сгенерированную конфигурацию local toolkit: launcher любого MCP автоматически
обеспечивает один общий broker независимо от порядка ленивого запуска клиента.
Ручной запуск нужен только для диагностики:

```bash
export TBANK_AUTH_BROKER_SOCKET="$HOME/.local/share/tbank-auth-broker/auth.sock"
.venv/bin/tbank-auth-broker
```

Передайте тот же `TBANK_AUTH_BROKER_SOCKET` процессам обоих MCP. Broker не
является MCP, не виден модели, единолично обновляет session и не возвращает
токены клиентам. Unix socket и его директория создаются с правами владельца.

Broker protocol v2 разделяет Banking и Hotels allowlist; это предотвращает
случайное использование операций не тем MCP. Поле client scope не является
защитой от вредоносного процесса под тем же OS-пользователем: все процессы с
доступом к owner-only socket находятся в одной локальной границе доверия.

Hotels scope содержит только `hotels.get_customer`, `hotels.list_bookings`,
`hotels.get_booking_v1` и `hotels.save_voucher_v1`. Первые две операции прошли
auth-effect probe, третья — live read-only smoke с собственной бронью, а
voucher отдельно подтвердил `401` без auth и `200 application/pdf` с Bearer.
Для всех четырёх принят
Bearer-only профиль; broker не добавляет cookies, `sessionid`, device query или
`x-real-ip`. EVO, task status, payment и mutations в allowlist не входят без
отдельного route-level evidence.

Не запускайте direct-file Banking MCP одновременно с broker для одного
`TBANK_BANKING_SESSION`. Межпроцессная блокировка защищает refresh rotation, но
в combined-режиме broker должен оставаться единственным владельцем session, а
обоим MCP нужно передать `TBANK_AUTH_BROKER_SOCKET`.

Read-only broker-операции могут включать refresh и provider request. Их timeout
по умолчанию равен 45 секундам и при необходимости задаётся от 1000 до 120000 мс:

```bash
export TBANK_AUTH_BROKER_TIMEOUT_MS=45000
```

### Локальное сохранение Hotels voucher

Операция `hotels.save_voucher_v1` вызывается Hotels MCP только по явному
пользовательскому запросу. Broker проверяет `application/pdf`, сигнатуру PDF и
лимит 5 MiB, записывает документ в owner-only каталог и возвращает только путь
и метаданные. Содержимое документа, provider `orderId` и credentials через
broker JSON не возвращаются. Файл автоматически удаляется по TTL:

```bash
export TBANK_HOTELS_VOUCHER_DIRECTORY='/absolute/owner-only/path/tbank-vouchers'
export TBANK_HOTELS_VOUCHER_TTL_SECONDS=900
```

Допустимый TTL — 60–3600 секунд; default — 900. По умолчанию используется
`~/.local/share/tbank-banking-mcp/vouchers`.

Если подключён только Banking MCP, можно не запускать broker. Если подключён
только Hotels MCP и нужен mobile customer read, broker запускается как локальный
auth provider; сам Banking MCP подключать к интегратору не требуется.

| Подключение | Конфигурация авторизации |
| --- | --- |
| Только Banking MCP | Direct session-файл или broker |
| Только Hotels MCP, поиск | Service JWT/static auth; broker не нужен |
| Только Hotels MCP, mobile customer read | Auth broker; Banking MCP в клиент не подключается |
| Оба MCP | Один auth broker и один `TBANK_AUTH_BROKER_SOCKET` для обоих процессов |

## Read-only проверка mobile auth для Hotels

Отдельный CLI проверяет только фиксированный набор customer-read маршрутов на
`https://hotels.t-bank-app.ru`. Он не является MCP tool, не доступен модели и
не умеет принимать произвольные URL, path или HTTP method. Пробник не вызывает
создание/изменение брони, оплату, отмену, промокоды или payment setup.

Перед запуском остановите auth broker: прямой CLI и broker не должны
одновременно владеть одной mobile session. После локального входа выполните:

```bash
cd /absolute/path/to/travel-assistant/tools/tbank-banking-mcp
.venv/bin/tbank-hotels-mobile-auth-probe --acknowledge-read-own-data
```

Без идентификаторов проверяются только `customerdata` и `booking_list`. Для
собственной брони или собственной booking task можно явно добавить:

```bash
.venv/bin/tbank-hotels-mobile-auth-probe \
  --acknowledge-read-own-data \
  --order-id '<own-order-id>' \
  --task-id '<own-task-id>'
```

Либо probe может локально получить identifiers из списка собственных заказов:

```bash
.venv/bin/tbank-hotels-mobile-auth-probe \
  --acknowledge-read-own-data \
  --discover-own-identifiers
```

Discovery читает booking list и не более пяти собственных booking details.
Значения identifiers, содержимое заказов и PII в отчёт не включаются.

Пробник сначала выполняет `no_auth_control`, затем последовательно сравнивает
ограниченные профили `bearer_only` → `bearer_session` → `capture_compatible` и
останавливается после первого auth-варианта с `2xx`. Тела обычных route-ответов
не сохраняются; для EVO анализируются только безопасный provider code и имена
top-level полей, а discovery локально читает собственные booking payloads.
Отчёт не содержит token, cookie,
`sessionid`, `orderId`, `taskId`, PII или response body, поэтому его можно
передать на review. Значения интерпретируются узко:

- `auth_effect_confirmed` означает, что control получил `401/403`, а конкретный
  auth-вариант — `2xx`;
- `public_or_auth_not_required` означает `2xx` и без Authorization, поэтому
  влияние Bearer не доказано;
- `http_accepted_auth_effect_unconfirmed` означает `2xx` с auth при
  неоднозначном control;
- `auth_boundary_passed_non_success` означает `401/403` у control и иной
  неуспешный HTTP status с auth: auth-граница, вероятно, пройдена, но рабочий
  endpoint contract не подтверждён;
- `401/403` означает `auth_rejected` для конкретного варианта;
- любой иной HTTP status или transport error остаётся `inconclusive`;
- результат одного route не переносится на другие Hotels endpoints.

`x-real-ip` намеренно не добавляется: он не нужен для исследуемых read-only
маршрутов по имеющимся контрактам. Если provider потребует его, это будет
отдельный contract gap, а не повод подставлять выдуманный IP.

## Privacy-safe smoke двух MCP

После запуска auth broker можно проверить Banking и Hotels MCP одним локальным
CLI вне LLM:

```bash
.venv/bin/tbank-mcp-safe-smoke --acknowledge-read-own-data
```

Если `TBANK_AUTH_BROKER_SOCKET` не задан, runner использует стандартный
owner-only socket `~/.local/share/tbank-auth-broker/auth.sock` для обоих MCP.
Search/rates/booking-preview проверяются только когда Hotels service transport
и auth отдельно доступны в окружении; mobile customer reads от этого не
зависят.

Runner вызывает только фиксированные read-only provider routes и локальные
`preview_only` операции. Он не принимает URL, provider identifiers или
произвольные tool names, не вызывает booking/payment execution и выводит только
readiness/pass/fail. `accountRef`, `bookingRef`, `orderId`, `paymentIntentId`,
ФИО, контакты, суммы, названия счетов/отелей и содержимое броней остаются внутри
локальных MCP-процессов и не включаются в отчёт. Hotels search/rates/preview
проверяются только если search auth profile присутствует в окружении runner.

## Подключение к OpenCode

```json
{
  "mcp": {
    "tbank-banking": {
      "type": "local",
      "command": [
        "/absolute/path/to/travel-assistant/tools/tbank-banking-mcp/.venv/bin/tbank-banking-mcp"
      ],
      "enabled": true
    }
  }
}
```

После изменения конфигурации полностью перезапустите OpenCode. Сначала
вызовите только `tbank_banking_connection_status`.

Для совместного подключения задайте одинаковый socket обоим процессам; команды
MCP при этом остаются независимыми:

```json
{
  "mcp": {
    "tbank-banking": {
      "type": "local",
      "command": ["/absolute/path/to/tbank-banking-mcp/.venv/bin/tbank-banking-mcp"],
      "environment": {
        "TBANK_AUTH_BROKER_SOCKET": "/absolute/secure/path/tbank-auth.sock"
      },
      "enabled": true
    },
    "tbank-hotels": {
      "type": "local",
      "command": ["node", "/absolute/path/to/tbank-hotels-mcp/src/server.mjs"],
      "environment": {
        "TBANK_AUTH_BROKER_SOCKET": "/absolute/secure/path/tbank-auth.sock"
      },
      "enabled": true
    }
  }
}
```

Конкретное имя поля окружения (`environment` или `env`) зависит от MCP-клиента;
оно не является частью MCP-протокола. Hotels search дополнительно сохраняет свой
обычный `TBANK_HOTELS_*` auth profile.

Общий план распространения Banking и Hotels MCP для CLI, local sidecar и
будущего Streamable HTTP находится в
[`../tbank-hotels-mcp/docs/portability-and-distribution-roadmap.md`](../tbank-hotels-mcp/docs/portability-and-distribution-roadmap.md).

## MCP tools

| Tool | Назначение |
| --- | --- |
| `tbank_banking_connection_status` | Локальная readiness без сети |
| `tbank_banking_list_accounts` | Нормализованный список счетов с process-local `accountRef` |
| `tbank_banking_spending_summary` | Агрегированные расходы/поступления и категории |
| `tbank_banking_build_travel_profile` | Объяснимый spending-based travel profile |
| `tbank_banking_build_portfolio_travel_profile` | Обезличенный travel profile по всем доступным счетам без передачи модели идентификаторов и абсолютных сумм |
| `tbank_banking_prepare_hotel_payment_handoff_preview` | Меж-MCP preview по выпущенному broker `paymentHandoffRef`; без provider order ID и сетевого запроса |
| `tbank_banking_payment_status` | Состояние локального payment intent |

## Композиция с Hotels MCP

Рекомендуемый пользовательский сценарий:

1. `tbank_banking_build_portfolio_travel_profile(days=90)` — Banking MCP сам
   анализирует агрегированные категории доступных счетов и возвращает модели только обезличенный профиль,
   ценовой диапазон и уровень уверенности.
2. Агент предлагает использовать диапазон и получает согласие пользователя.
3. Готовый объект `hotelPreferences` из ответа передаётся без преобразований в
   одноимённый аргумент `tbank_hotels_plan_personalized_stay`; `hotelDefaults` сохранён как
   совместимый alias. Счета, категории и суммы между MCP не передаются.
4. Hotels MCP применяет `best_value` локально и сохраняет диапазон мягким:
   результаты вне него не скрываются, а provider search body не получает
   ценовой фильтр.

`best_value` является объяснимым MCP-derived score на основе provider rating,
числа отзывов и цены. Он не является банковской оценкой, provider sort или
оценкой дохода пользователя. Пользовательский `ranking`, переданный явно,
имеет приоритет над profile default. Наличие одного `ranking=best_value` не
доказывает применение профиля: агент проверяет
`preferencesApplied.applied=true` в ответе Hotels.

Низкоуровневые `tbank_banking_list_accounts`,
`tbank_banking_spending_summary` и `tbank_banking_build_travel_profile` нужны
для явного запроса детального анализа конкретного счёта. Для обычного запроса
на обезличенные предпочтения агент не должен вызывать их дополнительно.

Профиль не является оценкой дохода или кредитоспособности. Default tiers
`economy`, `balanced`, `comfort`, `premium` основаны на нормализованных
расходах и должны быть переопределяемы пользователем.

Portfolio tool не возвращает `accountRef`, количество, названия и идентификаторы счетов,
балансы, абсолютные суммы или разбивку категорий расходов. Агрегированные
категории используются только внутри MCP для вычисления профиля. В низкоуровневом сценарии
raw account ID и баланс также не возвращаются; `accountRef` живёт только в
текущем MCP-процессе и после перезапуска получается заново через
`list_accounts`.

## Payment boundary

Hotels MCP преобразует process-local `bookingRef` в короткоживущий
`paymentHandoffRef` через общий local auth broker. Banking MCP принимает только
этот одноразовый capability и атомарно поглощает его у того же broker при первом
preview. Повторный preview требует нового handoff. Сырой provider `orderId` и
`paymentToken` не являются частью меж-MCP контракта. Ответ явно содержит
`bookingBindingVerified=true` и `amountBindingVerified=true`: сумма и валюта
переносятся из наблюдаемого `booking v1 rateData.paymentData.paymentPrice`, а
raw `paymentStatus` передаётся без интерпретации. Это не доказывает, что сумма
является текущей задолженностью или может быть списана.
Сумма хранится в preview как каноническая decimal-строка `amountDecimal`.
Capability также переносит локальное время наблюдения booking facts и
ограниченное окно свежести; протухшие facts отклоняются до создания intent.
Выбранный `accountRef` должен быть выпущен текущим Banking MCP-процессом и
проверяется до атомарного поглощения capability. Provider JSON к этому моменту
уже разобран, поэтому механизм не доказывает исходную точность или официальный
decimal scale контракта.

В readiness используется термин `bookingBindingSupported`: он означает только
доступность механизма. `bookingBindingVerified=true` появляется лишь в уже
выпущенном capability. Raw status возвращается как
`paymentStatusObservation` с `interpretation=not_interpreted`, а не как
подтверждённое состояние возможности оплаты.

Текущий Banking preview не вызывает upstream `/v1/pay`, Hotels payment setup или
денежный provider endpoint и не создаёт иллюзию исполненной операции. Один
booking v1 read выполняется broker при выпуске handoff. Preview
возвращает:

- `status=preview_only`;
- redacted source account;
- hash и TTL intent;
- `executionAvailable=false`;
- структурированный `executionReadiness` со статусом
  `contract_evidence_required`;
- provenance booking read, `paymentSetupPerformed=false` и
  `paymentExecutionPerformed=false`;
- список отсутствующих contract/security gates.

Старый experimental tool с `booking_order_id` удалён из публичного MCP-контракта,
чтобы LLM не могла случайно выбрать небезопасную границу. Единственный
публичный путь — `tbank_banking_prepare_hotel_payment_handoff_preview`.

Следующий payment slice разрешён только после подтверждения:

- официальной связи Hotels `orderId/payment setup` с banking payment;
- trusted human confirmation channel с button-capable elicitation;
- provider idempotency semantics;
- reconciliation после timeout/unknown outcome;
- допустимого источника device/antifraud attributes;
- sandbox/non-production approval.

OTP, пароль, PIN, PAN, CVV и mobile session token не должны становиться
аргументами MCP tools.

## Проверка

```bash
PYTHONPATH=. python3 -m unittest discover -s test -v
python3 -m compileall -q src login_cli.py test
```

MCP transport реализован без зависимости от Python MCP SDK; runtime-зависимость
только `requests`. Unit/protocol-тесты не выполняют сетевые запросы и не читают
пользовательскую сессию. Отдельный offline packaging test собирает wheel в
изолированной временной директории, проверяет его содержимое, устанавливает
вне checkout и выполняет MCP `initialize` без provider-сети.
