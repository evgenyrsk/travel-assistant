# T-Bank MCP — publication readiness review

**Статус:** completed internal review; developer-preview follow-up prepared

**Дата:** 2026-08-27

**Scope:** Hotels `0.28.0`, Banking `0.17.0`, local toolkit `0.12.0`, локальные
release artifacts и подключение через `stdio`.

**Out of scope:** production booking/payment execution, remote HTTP transport,
registry upload и изменение внешних API.

## Executive verdict

| Tier | Вердикт |
| --- | --- |
| Локальный Hotels read/preview | GO |
| Локальный Banking/customer read | GO с mobile login и file-session warning |
| Локальные npm/wheel candidates | GO |
| Публичная установка без checkout | Технический GO; registry upload ждёт login |
| Booking/payment execution | NO-GO |

## Findings

### P1-1 — toolkit зависит от layout репозитория — закрыто

`tools/tbank-mcp-local/src/cli.mjs` вычисляет Hotels server, Banking `.venv`,
broker и login script через `repositoryRoot`. Опубликованный toolkit не сможет
запустить отдельно установленные npm/wheel packages.

**Resolution:** toolkit `0.11.0` разрешает команды через проверенные абсолютные
overrides, development fallback и установленные bin-команды из `PATH`.
Install-outside-checkout regression test проверяет опубликованную форму.

### P1-2 — Banking phone login не входит в wheel как console script — закрыто

Toolkit запускает `login_cli.py` из checkout, а `pyproject.toml` не объявляет
отдельную login-команду. Fresh install через `pipx`/`uvx` не сможет выполнить
mobile login штатным способом.

**Resolution:** Banking `0.17.0` включает packaged module `src.login_cli` и
console script `tbank-banking-login`; wheel test проверяет entry point и logout
из установленного вне checkout артефакта.

### P1-3 — публикационные имена и лицензия не утверждены — закрыто для preview

Для developer preview выбраны свободные на момент проверки registry names:
`tbank-hotels-mcp`, `tbank-mcp-local` и
`travel-assistant-tbank-banking-mcp`. Npm packages больше не `private`, имеют
public registry metadata и `UNLICENSED`. Это позволяет распространить preview,
но не предоставляет open-source лицензию. `LICENSE.upstream` продолжает
описывать происхождение заимствованного Banking-кода.

**Resolution:** namespace и preview audience выбраны владельцем; metadata
добавлена. Выбор MIT/Apache/другой публичной лицензии остаётся отдельным stable-
release решением.

### P1-4 — публичная credential story не завершена — закрыто для search

Hotels search использует `anonymous` по умолчанию и не отправляет auth header.
Service JWT/static profiles являются опциональными overrides. Mobile phone auth
подтверждён только для ограниченных customer reads и не расширяется на
непроверенные endpoints.

**Resolution:** публичный preview рассчитан на anonymous search и локальную
mobile session для allowlisted customer reads. Общий ключ в package не
встраивается; реальные `401/403` считаются terminal результатом без перебора.

### P2-1 — toolkit npm artifact не имеет publish allowlist — закрыто

Dry-run package содержит tests, internal review prompt и manifests. Секретов
нет, но это лишняя поверхность и размер.

**Resolution:** `files` allowlist оставляет только runtime, manifests и README;
npm pack/install test фиксирует точный состав и запуск вне checkout.

### P2-2 — portable release matrix не завершена

Artifact install/initialize проверены локально, но нет fresh-machine macOS/Linux
matrix, независимого третьего MCP client, upgrade/rollback, checksums, SBOM и
provenance.

**Fix:** сначала developer preview, затем CI release matrix и signed metadata.

### P2-3 — mobile session хранится в owner-only файле

Текущая защита корректна для локального preview, но OS Keychain adapter ещё не
реализован. Для публичного developer preview нужен явный warning; для stable
release — secure storage по умолчанию.

## Checks

- Full offline verify до anonymous follow-up: toolkit `16/16`, Hotels `59/59`, Banking `52/52`.
- Targeted artifact checks после hardening: toolkit `16/16` вне restricted
  sandbox, Banking wheel/login `1/1`.
- Финальный offline verify после publication follow-up: toolkit `17/17`, Hotels `60/60`,
  Banking `52/52`; manifests и conformance passed, provider requests `0`.
- Manifest и MCP conformance: passed.
- Provider requests: `0`.
- Hotels npm dry-run: allowlisted runtime из 8 файлов.
- Toolkit npm dry-run: allowlisted runtime/manifests/README из 8 файлов.
- Secret scan добавленных строк: совпадения только в redaction regression
  fixtures и удалённом legacy PEM wrapper; реальных credentials не найдено.
- Documentation gate и `git diff --check`: passed.

## Рекомендуемая последовательность

1. Выполнить registry login и загрузить versioned developer-preview packages.
2. Опубликовать GitHub preview artifacts с checksums.
3. Проверить fresh install в Codex и OpenCode на macOS, затем Linux.
4. Перед stable release выбрать публичную лицензию и добавить SBOM/provenance.
