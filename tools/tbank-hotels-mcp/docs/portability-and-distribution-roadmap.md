# Roadmap переносимости и распространения T-Bank MCP

**Роль документа:** tool-local source of truth для будущего подключения
`tbank-hotels-mcp`, `tbank-banking-mcp` и локального auth broker к разным
MCP-клиентам и приложениям. Документ не меняет основной product roadmap,
hotel-only MVP или порядок этапов Kotlin backend.

**Статус:** `In progress`. Локальный toolkit `0.15.0` закрыл локальную
установку, раздельное/совместное подключение и versioned contracts без
активации remote transport или execution capabilities.

**Принятое решение от 2026-08-24:** сначала довести и стабилизировать локальный
read-only/preview-only MCP, затем выпустить воспроизводимый локальный `stdio`
профиль и только после этого реализовывать удалённый Streamable HTTP. Это не
означает откладывать переносимость архитектуры: domain/tool logic должна быть
отделена от transport adapters уже в локальной реализации. Remote deployment,
mobile customer auth и execution capabilities не активируются автоматически
после завершения локального релиза.

**Нормативный ориентир на дату плана:** MCP `2026-07-28` — официальные
спецификации
[`stdio`](https://modelcontextprotocol.io/specification/2026-07-28/basic/transports/stdio),
[`Streamable HTTP`](https://modelcontextprotocol.io/specification/2026-07-28/basic/transports/streamable-http)
и
[`Authorization`](https://modelcontextprotocol.io/specification/2026-07-28/basic/authorization).
Перед реализацией P0/P5 версию нужно сверить повторно.

## 1. Цель

Поддержать три независимых способа использования без привязки к браузерному
агенту или конкретной LLM:

| Профиль | Для кого | Transport | Приоритет |
| --- | --- | --- | --- |
| Локальный CLI | OpenCode и Codex CLI; Claude Code остаётся необязательным generated profile вне acceptance matrix | `stdio` | Первый |
| Локальное приложение или sidecar | Desktop-приложение, локальный сервис, self-hosted automation | `stdio` или localhost Streamable HTTP | После стабильного CLI-релиза |
| Удалённый сервис | Web/mobile/backend-интегратор и multi-user deployment | Streamable HTTP | Отдельный security и operations этап |

Hotels MCP и Banking MCP остаются отдельными логическими серверами и могут
подключаться вместе или по одному. Общий installer, launcher или deployment
bundle не должен объединять их полномочия. Это следует
[`ADR-0003`](../../../docs/decisions/adr-0003-banking-mcp-and-hotels-composition-boundary.md)
и
[`ADR-0004`](../../../docs/decisions/adr-0004-shared-mobile-auth-broker.md).

## 2. Текущее состояние

| Область | Сейчас | Ограничение для распространения |
| --- | --- | --- |
| Hotels MCP | Node.js 20+, `stdio`, package bin, protocol `2025-03-26`; anonymous read-only search; `0.28.1` опубликована, `0.29.0` — локальный resumable-search candidate | Для нового candidate нужны review/smoke/upload; нет checksums/SBOM/provenance и полной OS/client matrix |
| Banking MCP | Python 3.11+, `stdio`, protocol `2025-03-26`; `0.17.0` опубликована и устанавливается вне checkout | Нет кроссплатформенного secure storage, checksums/SBOM/provenance и полной OS matrix |
| Общая авторизация | Локальный broker через owner-only Unix socket | Unix socket и session-файл не подходят как remote/multi-tenant boundary |
| Состояние journey | Process-local opaque handles с TTL | После перезапуска теряется; для нескольких HTTP instances нужен общий secure store |
| Read-only функции | Search, customer reads и агрегаты прошли ограниченные smoke/fake gates | Нужен повторный независимый review и воспроизводимый compatibility suite |
| Mutations | Выключены и остаются `NO-GO` | Не блокируют read-only distribution, но не входят в первый релиз |

Одинаковый язык реализации не является требованием MCP. Переписывать Banking
MCP с Python на Node.js только ради упаковки не планируется. Решение можно
пересмотреть отдельным ADR, если измеримые затраты на два runtime окажутся выше
стоимости миграции и повторной проверки auth/payment поведения.

## 3. Целевая архитектура подключения

### 3.1. Локальный профиль

- MCP-клиент запускает каждый выбранный сервер как отдельный `stdio` subprocess.
- JSON-RPC выводится только в `stdout`; диагностика — только в `stderr`.
- Секреты не передаются в arguments tools, prompt или command-line flags.
- Hotels search может работать автономно без auth; service/static auth остаётся опциональным integration override.
- Banking MCP может работать автономно с локальной mobile session.
- При совместном использовании один локальный auth broker владеет refresh и
  выдаёт обоим MCP только allowlisted высокоуровневые операции.
- Один setup/doctor CLI может установить и проверить компоненты, но в
  конфигурации интегратора остаются два независимых MCP entry.

### 3.2. Локальный sidecar

- Контейнер или localhost HTTP adapter является дополнительным способом
  запуска, а не обязательным условием для CLI.
- Localhost transport привязывается только к `127.0.0.1`, проверяет `Origin` и
  требует локальную аутентификацию.
- Телефонный login и secure storage остаются вне LLM и не должны выполняться
  через MCP tool.
- Docker не является первым способом установки: проброс OS Keychain, mobile
  session и локального broker в контейнер усложняет самый частый desktop flow.

### 3.3. Удалённый профиль

- Отдельные Streamable HTTP endpoints для Banking и Hotels либо gateway с
  раздельными scopes и policy; объединённая tool-поверхность по умолчанию не
  создаётся.
- Входящая MCP-авторизация и исходящая авторизация T-Bank — разные границы.
  Токен MCP-клиента нельзя прозрачно передавать в upstream API.
- Нужны OAuth discovery, audience validation, минимальные scopes, TLS,
  `Origin` validation и tenant isolation.
- Journey/rate/draft handles остаются непрозрачными. Customer PII и mobile
  credentials нельзя кодировать в handle; они хранятся в зашифрованном
  per-user store с TTL или не сохраняются вовсе.
- Mobile auth на основе неофициального capture-driven контракта остаётся
  local/self-hosted до отдельного legal/security решения и подтверждённого
  server-side authorization flow. Первый remote профиль может быть только
  read-only Hotels search с официальным service credential.
- Нужны quotas, rate limits, bounded concurrency, audit без PII, egress
  allowlist, secret manager, rotation и operational kill switch.

## 4. Этапы

Обязательная последовательность:

```text
рабочие read-only/preview flows
→ стабильный versioned tool contract
→ воспроизводимый локальный stdio release
→ удалённый read-only Streamable HTTP
→ отдельно подтверждённая remote customer auth
→ отдельно разрешённые booking/payment execution tiers
```

Переход к следующему пункту разрешён только после gate предыдущего. P0 можно
проектировать параллельно с локальным hardening, но P5 не начинается до
завершения P0–P3. P6 остаётся независимо заблокирован внешними контрактами даже
после успешного P5.

### P0. Зафиксировать compatibility baseline

**Статус:** `In progress`.

- [x] Зафиксировать versioned tool manifest: names, input schemas,
  annotations, error taxonomy и capability/readiness fields.
- [x] Определить SemVer policy: additive tool/schema change, deprecation и
  breaking release.
- [x] Добавить golden protocol manifests и snapshot совместимости без provider
  network.
- [ ] Подтвердить стратегию совместимости текущего handshake-based protocol
  `2025-03-26` с MCP `2026-07-28`; не выполнять breaking upgrade без client
  matrix.
- [x] Описать отдельные capability tiers: `hotels_read`, `customer_read`,
  `banking_read`, `preview_only`, `booking_execute`, `payment_execute`.

**Gate:** одинаковый read-only контракт воспроизводится после перезапуска в
fake transport; изменения схем обнаруживаются CI до релиза.

### P1. Универсальный локальный `stdio`

**Статус:** `In progress`.

- [x] Проверить framing, EOF shutdown, clean restart и отсутствие постороннего
  `stdout`/`stderr` у обоих MCP; cancellation остаётся отдельной проверкой.
- [x] Проверить каждый MCP отдельно и оба вместе в OpenCode и Codex CLI;
  добавить ещё один независимый MCP client/Inspector. Claude Code исключён из
  текущей acceptance matrix решением владельца, но генератор config сохраняется.
- [ ] Проверить macOS и Linux; Windows включить после выбора замены Unix socket
  или документированного local broker transport.
- [x] Создать генерируемые config templates без секретов для каждого клиента и
  режимов Hotels-only, Banking-only и combined.
- [x] Добавить локальный `doctor`, который проверяет runtime, paths, broker,
  readiness и версии, но не выполняет provider request по умолчанию.

**Gate:** fresh-machine сценарий от установки до `connection_status` занимает
одну документированную последовательность; тест не обращается к production.

### P2. Воспроизводимая установка и release artifacts

**Статус:** `In progress`.

- [x] Загрузить Hotels versioned npm package с bin-командой и проверить
  fresh-install вне checkout; новая версия проходит тот же gate перед upload.
- [x] Загрузить Banking versioned wheel в PyPI; editable install оставить
  только для разработки.
- [x] Проверять локальный Hotels npm tarball и Banking wheel в изолированных
  временных каталогах: allowlisted contents, установка вне checkout и MCP
  `initialize` без provider network.
- [x] Добавить общий launcher/config generator, который регистрирует один или
  два MCP, не объединяя servers и credentials.
- [x] Убрать runtime-зависимость launcher от checkout: установленные Hotels,
  Banking, broker и phone-login команды разрешаются из `PATH`, абсолютные
  overrides валидируются, repository layout остаётся development fallback.
- [x] Включить phone login в Banking wheel как `tbank-banking-login` и
  проверить entry point вне checkout.
- [x] Ограничить npm artifact toolkit allowlist-набором runtime, manifests и
  публичного README; tests и внутренние review-материалы не публикуются.
- [ ] Перенести mobile session из обычного файла в storage adapter: OS
  Keychain по умолчанию, file fallback только с явным предупреждением.
- [ ] Добавить pinned dependencies/lockfiles, release checksums, SBOM,
  provenance и проверку MIT-derived компонентов.
- [ ] Автоматизировать upgrade/rollback с сохранением совместимого config и
  без вывода credentials.

**Gate:** артефакты собираются в чистой CI-среде, устанавливаются без checkout
репозитория и проходят offline conformance suite.

### P3. Compatibility и release quality gate

**Статус:** `In progress`.

- [ ] Матрица OS/runtime/client: поддерживаемые Node.js, Python, macOS, Linux,
  затем Windows.
- [ ] Сценарии: install, first login вне LLM, restart, refresh rotation,
  logout, broker unavailable, stale handle, server upgrade и rollback.
- [x] Свести локальные login/logout и полный offline release gate к коротким
  командам общего toolkit; реальная login-сеть не входит в offline gate.
- [ ] Проверить standalone и combined least-privilege modes.
- [x] Проверить, что tests герметичны даже при credentials в parent
  environment и что ни один release check не вызывает production API.
- [ ] Выполнить независимый security/contract review и закрыть findings
  выбранного release tier.

**Gate:** публикуется только capability tier, прошедший всю матрицу. Наличие
read-only релиза не активирует mutations.

### P4. Опциональный Docker/sidecar

**Статус:** `Planned after P3`.

- [ ] Подготовить отдельные минимальные images и compose profile; не помещать
  ключи или session в image layers.
- [ ] Добавить health/readiness endpoints без provider-вызова.
- [ ] Задать read-only filesystem, non-root user, resource limits и egress
  policy.
- [ ] Для combined profile сохранить отдельные processes и scopes; broker не
  публиковать наружу.
- [ ] Проверить localhost deployment для desktop/service integration.

**Gate:** контейнер даёт воспроизводимость, но не становится обязательным для
телефонной авторизации или обычного CLI.

### P5. Streamable HTTP для приложений и сервисов

**Статус:** `Future / отдельная architecture task`.

- [ ] Реализовать transport adapters поверх той же domain/tool логики, без
  копирования Hotels или Banking business rules.
- [ ] Поддержать актуальный MCP protocol и явную backward-compatibility policy.
- [ ] Реализовать MCP OAuth resource boundary, discovery, scopes и token
  validation отдельно от upstream T-Bank credentials.
- [ ] Ввести tenant-isolated encrypted state/secret stores, lifecycle TTL,
  logout/revoke semantics и безопасный concurrent refresh.
- [ ] Реализовать `Origin` validation, localhost-only binding для local mode,
  rate limiting, quotas, circuit breaker, audit и telemetry без PII.
- [ ] Провести load, failure, timeout, rolling-upgrade и multi-instance tests.
- [ ] Запретить remote customer/banking profiles до отдельного подтверждения
  auth, legal, antifraud и data-processing модели.

**Gate:** multi-user service не использует локальный session-файл/Unix socket,
не передаёт входящий MCP token в upstream и выдерживает tenant isolation review.

### P6. Controlled execution tiers

**Статус:** `Blocked` внешними API-контрактами.

- [ ] Активировать booking/payment/cancel только после sandbox evidence,
  idempotency, reconciliation, trusted human confirmation и approval владельца
  API.
- [ ] Выпускать execution capability отдельным opt-in profile и отдельным
  release gate.
- [ ] Не считать Docker, remote transport или успешный login разрешением на
  денежные операции.

**Gate:** каждый write-flow имеет подтверждённый contract, безопасный timeout
recovery, audit trail и kill switch; production activation разрешается отдельно.

## 5. Порядок ближайших задач

Ближайший порядок для локального read-only/preview-only release:

1. [x] Завершить безопасный voucher PDF flow только на fixture/fake transport.
2. [x] Зафиксировать статическую цепочку hotel order → Hotels payment state →
   Banking payment preview без provider-вызовов.
3. [x] Добавить typed profile-to-search contract, мягкий `best_value`,
   presentation facts и локальные artifact candidates.
4. [x] Провести повторный независимый review текущих Hotels/Banking/broker
   изменений и закрыть release-blocking findings.
5. [ ] Current-worktree Codex smoke пройден для обычного/breakfast поиска,
   preview/handoff, customer summary и Banking-personalized flow. После
   публикации повторить короткий fresh-install smoke в Codex и OpenCode;
   Claude Code не входит в текущую acceptance matrix.
6. [ ] Завершить P1–P3 до объявления portable release: fresh-machine install,
   macOS/Linux/client matrix, secure storage, checksums, SBOM и provenance.
7. [ ] P4–P5 начинать отдельными задачами; P6 не следует автоматически ни из
   одного предыдущего этапа.

## 6. Решения, которые не нужно принимать сейчас

- Не переписывать Python MCP на Node.js только ради единого стека.
- Не объединять Banking и Hotels в один привилегированный MCP.
- Не делать Docker обязательным способом локальной установки.
- Не переносить mobile token в MCP config, prompt, tool arguments или remote
  environment без отдельной модели управления секретами.
- Не поддерживать legacy HTTP+SSE в новом remote adapter, если client matrix не
  докажет необходимость.
- Не публиковать mutating tools как доступные только потому, что их schemas уже
  существуют.

## 7. Definition of done portable read-only release

- [ ] Hotels и Banking подключаются по одному и вместе минимум к четырём
  проверенным MCP-клиентам.
- [ ] Установка не требует checkout репозитория и ручного редактирования кода.
- [ ] Секреты вводятся только доверенным локальным auth/setup flow.
- [ ] `doctor` и `connection_status` объясняют готовность без сетевого probe.
- [ ] Offline conformance, security, packaging и OS/client matrix зелёные.
- [ ] Tool contract versioned; breaking changes и migration path документированы.
- [ ] Release artifacts имеют checksums, SBOM и provenance.
- [ ] Read-only release не содержит доступных execution capabilities.
- [ ] Remote release, если он существует, прошёл отдельные OAuth, tenant
  isolation, load и operations gates.
