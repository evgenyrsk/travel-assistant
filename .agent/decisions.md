# Decisions

- Общий broker живёт дольше отдельного stdio MCP и завершается явно через
  logout/lifecycle shutdown, чтобы lazy-start порядок клиента не влиял на auth.
- В combined profile оба launcher-а используют `--ensure-broker`; standalone
  Hotels сохраняет service-auth режим без обязательной mobile session.
- `--with-broker` сохранён как обратносуместимый alias.
- Persistent broker остаётся локальным user-session process, не production daemon.

## Broker lifecycle must not belong to one MCP child

- **Decision:** combined client entries для Hotels и Banking независимо
  гарантируют наличие одного общего broker; завершение отдельного MCP не
  завершает shared session daemon.
- **Reason:** lazy-start порядок клиента непредсказуем, а ownership одним MCP
  ломает другой при отсутствии/завершении владельца.
- **Boundary:** broker остаётся локальной инфраструктурой вне LLM и сохраняет
  scoped allowlist ADR-0004.

## Explicit graceful shutdown

- **Decision:** toolkit получает локальную lifecycle-операцию остановки, а
  logout сначала останавливает broker и только затем удаляет session.
- **Reason:** persistent broker требует детерминированного cleanup без ручного
  поиска PID и без stale socket.

## Public completion uses hosted checkout handoff

- **Decision:** публичный MCP доводит пользователя до безопасного перехода в
  официальную public page выбранного отеля, но не проводит card/OTP/3DS и не обещает перенос
  exact rate, дат/гостей или резервирование номера.
- **Reason:** переданные Swagger-контракты подтверждают booking/payment DTO, но
  не закрывают customer execution auth, trusted `x-real-ip`, provider
  idempotency, timeout reconciliation и полный payment lifecycle.
- **Boundary:** direct booking/payment execution остаётся fail-closed и требует
  отдельного evidence + review; hotel-page handoff не содержит PII, `bookHash`,
  provider identifiers или платёжные реквизиты.

## Hosted checkout preserves only verified public search context

- **Decision:** для одной комнаты без детей hotel-page handoff добавляет
  `dateFrom`, `dateTo` и `guests`; для сложной occupancy — только даты.
- **Reason:** read-only public-page проверка подтвердила эти параметры для
  простого сценария, но не дала контракта exact-rate или сложного состава гостей.
- **Boundary:** `bookHash`, `rateOptionId`, exact rate, PII и неподтверждённые
  query-параметры не попадают в URL.

## Banking runtime is restricted by an allowlisted facade

- **Decision:** Banking MCP и auth broker получают только
  `CuratedMobileSession` с шестью подтверждёнными read-операциями.
- **Reason:** vendored mobile client нужен для совместимого login/refresh, но
  его широкая банковская поверхность не должна быть достижима из MCP runtime.
- **Boundary:** login CLI и явный read-only auth probe могут создавать raw
  mobile session вне MCP tool surface; деньги, переводы и marketplace методы
  через facade недоступны.

## Mobile login remains a terminal command, not an MCP tool

- **Decision:** телефонная авторизация поставляется как
  `tbank-banking-login` и вызывается общим launcher, но не дублируется в Hotels
  или Banking MCP tool surface.
- **Reason:** номер, SMS-код, пароль и PIN не должны попадать в model context;
  одна packaged terminal-команда одинаково обслуживает standalone и combined
  профили без расширения полномочий MCP.
- **Boundary:** toolkit находит login/broker/server commands из `PATH` или
  проверенных абсолютных overrides; никакие credentials не записываются в
  client config.

## Public preview uses anonymous Hotels search

- **Decision:** Hotels search запускается без auth header, если static/service
  credentials не настроены; mobile authorization остаётся для allowlisted
  customer reads через broker.
- **Reason:** публичный search endpoint фактически работает без авторизации, а
  обязательный JWT создавал лишний onboarding и не нужен обычному пользователю.
- **Boundary:** anonymous mode не активирует mutations или customer reads;
  `401/403` не вызывает перебор credentials, service JWT не поставляется вместе
  с package.

## Preview publication is installable but not open-source licensed

- **Decision:** npm artifacts публикуются с `UNLICENSED` до отдельного решения
  владельца по публичной лицензии.
- **Reason:** это позволяет не приписывать проекту невыбранную лицензию и явно
  отделяет installable developer preview от разрешения на reuse/redistribution.
