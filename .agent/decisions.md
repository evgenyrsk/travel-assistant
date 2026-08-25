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
