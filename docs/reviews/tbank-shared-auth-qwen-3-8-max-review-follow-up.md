# Follow-up аудита Banking MCP, Hotels MCP и auth broker

**Роль:** completed implementation follow-up внешнего review Qwen 3.8 Max.

## Scope

- локальные security/reliability findings общей mobile auth;
- hermetic tests, protocol compatibility и документация deployment boundary;
- без чтения секретов, live provider calls, бронирования, отмены или платежей.

## Результат

| Finding | Результат |
| --- | --- |
| Per-client broker allowlist | Закрыт protocol v2 с `banking`/`hotels` scopes; same-UID trust boundary задокументирована |
| Error redaction | Переиспользован upstream `redact_text`, добавлен scrub составного session token |
| Broker timeouts | Default 45 секунд, диапазон 1–120 секунд; status probe Hotels ограничен 1,5 секунды |
| Негерметичный Hotels test | Все `TBANK_HOTELS_*` очищаются и восстанавливаются |
| Logout | Добавлен локальный `login_cli.py --logout`; server-side revoke не заявлен |
| Concurrent refresh | Добавлены advisory file lock и запрет смешивать direct/broker ownership |
| Socket hardening | `umask 0177`, `0600`, read timeout, backlog и лимит 16 соединений |
| Booking identifier | Raw ID передаётся один раз и проверяется по allowlist charset |
| Readiness semantics | Hotels status проверяет broker и session без provider request |
| Bounded state | Account references и payment intents ограничены 100 элементами |
| Wheel provenance | `README.md` и `UPSTREAM.md` включены в package metadata/artifact |

## Решения и ограничения

- `client` scope предотвращает случайное нарушение границы MCP, но не
  аутентифицирует процесс того же OS-пользователя.
- Mobile auth расширен только на capture-driven `get_booking_v1`.
- Удаление `sessionid` из Hotels request отложено до сравнительного read-only
  smoke, чтобы не менять capture-derived контракт предположением.
- Hotel-specific `Pg-Api-System`, `flow.type`, token mapping, idempotency и
  authoritative payment status не подтверждены. Payment execution остаётся
  `NO-GO`.

## Проверки

- Banking unit/protocol suite: 17/17, включая реальный Unix socket test.
- Hotels suite: 36/36, включая ответ broker позже прежнего трёхсекундного timeout.
- Python compile check и Node syntax check прошли.
- Wheel `0.3.0` содержит `UPSTREAM.md`, license и CA artifact.
- `git diff --check` прошёл.

## Следующий шаг

Локальный phone login и только read-only smoke: broker status, Banking
aggregates и `get_booking_v1` на собственной тестовой брони. Любой `401`, `403`
или `404` фиксируется как результат endpoint matrix; подбор credentials/routes и
mutating вызовы запрещены.
