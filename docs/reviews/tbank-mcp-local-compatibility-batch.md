# T-Bank MCP local compatibility batch

**Роль:** completed implementation report для Hotels MCP `0.18.1`, Banking MCP
`0.8.1` и local toolkit `0.2.1`.

## Scope

Выполнен крупный offline batch: постоянная локальная конфигурация без PEM в
client config, общий launcher/doctor, secret-free client registration,
versioned tool manifests и protocol conformance. Provider API, mobile login,
customer data и mutations не вызывались.

## Результат

- Hotels принимает `TBANK_HOTELS_JWT_PRIVATE_KEY_FILE`; inline key сохранён для
  совместимости, одновременно задавать два источника запрещено.
- `tools/tbank-mcp-local` создаёт owner-only config, запускает Hotels, Banking
  или broker и генерирует подключение OpenCode, Codex CLI и Claude Code.
- Toolkit даёт короткие interactive `login/logout` вне LLM и единую команду
  `verify` для всего offline release gate.
- Combined client config назначает Banking launcher владельцем broker lifecycle,
  поэтому после перезапуска клиента customer reads не требуют отдельного
  ручного запуска broker.
- Codex CLI зарегистрировал `tbank-hotels` и `tbank-banking` как два enabled
  `stdio` MCP без env-значений в client config; Banking использует launcher
  `--with-broker`. Постоянный Banking setup и offline doctor прошли, Hotels
  setup ожидает путь к owner-only JWT key.
- Hotels booking summary возвращает только counts, а Banking portfolio profile
  скрывает абсолютные суммы, сведения о счетах, разбивку категорий и booking
  history; агрегированные category signals используются только внутри MCP.
- Doctor проверяет runtime, executables, transport, key/session permissions и
  broker socket без provider probe и без вывода значений секретов.
- Hotels и Banking manifests фиксируют server version, protocol, tool names,
  descriptions, input schemas, output-schema presence и annotations.
- Offline conformance дважды запускает оба MCP и проверяет initialize,
  tools/list, ping, newline framing, EOF shutdown, clean restart и отсутствие
  неожиданного stderr.

## Capability boundary

Read-only и preview-only tiers остаются локальными experimental capabilities.
`booking_execute` и `payment_execute` остаются `NO-GO`: toolkit, Docker или
успешный doctor не активируют их.

## Checks

| Проверка | Результат |
| --- | --- |
| Hotels `npm test` | 48 passed |
| Banking `unittest` | 38 passed, включая Unix-socket test вне sandbox |
| Local toolkit `npm run verify` | 5 toolkit + 48 Hotels + 38 Banking tests; manifests match; conformance passed |
| Provider requests | 0 |
| `git diff --check` | Выполняется общим финальным gate |

## Remaining gates

- standalone client-profile smoke при необходимости и cross-OS проверки;
  combined OpenCode/Codex CLI и privacy-кейсы подтверждены, Claude Code
  исключён из acceptance matrix решением владельца;
- фактическая клиентская матрица вне OpenCode и cross-OS проверки;
- packaging/publishing и OS Keychain;
- все внешние booking/payment auth, header, idempotency и recovery contracts.

## Release review follow-up

Независимый Qwen 3.8 Max review дал `CONDITIONAL READY` без P0–P2 и семь P3.
Все семь закрыты одним patch checkpoint:

- Banking session path больше не передаётся Hotels-процессу;
- версии, test counts и review index синхронизированы;
- editable-install обновлён до Banking `0.8.1`;
- локальные `opencode.json`, `request.txt` и `message*.txt` исключены из Git;
- Hotels runtime требует owner-only key file;
- broker booking responses рекурсивно очищаются от расширенного класса provider
  identifiers и token-полей.

Повторный offline gate: 5 toolkit + 48 Hotels + 38 Banking tests, manifests и
conformance прошли, provider requests не выполнялись.

Codex CLI follow-up зарегистрировал оба MCP без env-секретов, combined doctor
вернул `ready=true`, а ephemeral read-only session через встроенный tool
discovery вызвал только два `connection_status`. Hotels `0.18.1` и Banking
`0.8.1` доступны; provider search/customer/payment/booking calls не выполнялись.
