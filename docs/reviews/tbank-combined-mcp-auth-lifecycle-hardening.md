# Hardening жизненного цикла общей mobile session

**Статус:** completed local implementation

**Scope:** Banking/broker `0.14.0`, local toolkit `0.6.0`, combined launcher

**Out of scope:** provider smoke, refresh/revoke semantics, Keychain, remote transport, booking/payment mutations

## Причина

Live smoke подтвердил privacy-safe `tbank_hotels_summarize_bookings` и
`tbank_banking_build_portfolio_travel_profile`, но только после ручного запуска
auth broker. Сгенерированная combined-конфигурация поднимала broker лишь вместе
с Banking MCP, а ленивый запуск Hotels первым оставлял customer reads без mobile
session.

## Реализация

- launcher обоих MCP в combined-профиле выполняет идемпотентный `ensure-broker`;
- broker отделён от срока жизни отдельного stdio MCP и переиспользует один
  owner-only Unix socket;
- одновременный старт допускает гонку процессов, но оба launcher-а ждут общий
  готовый socket, даже если их собственный broker-кандидат проиграл гонку;
- protocol lifecycle добавляет локальные `status` и `shutdown` без credentials
  и provider requests;
- toolkit `logout` сначала завершает broker, затем удаляет local session;
- отдельная `stop-broker` оставлена как диагностическая команда;
- прежний `--with-broker` сохранён как совместимый alias.
- существующий combined config без lifecycle-флага распознаётся по сохранённой
  общей mobile-session конфигурации, поэтому достаточно перезапустить MCP-клиент.

## Проверяемые критерии

- [x] Hotels-first запускает broker без обращения к Banking MCP.
- [x] Banking-first использует тот же lifecycle.
- [x] Одновременный lazy start переиспользует один socket.
- [x] EOF одного MCP не завершает broker.
- [x] Lifecycle shutdown отвечает до остановки socket server.
- [x] Старый combined launcher contract не требует ручной миграции config.
- [x] Проверки не обращаются к Hotels или Banking API.
- [x] Токены, session contents и private key не читаются и не выводятся.

## Решение по безопасности

Broker остаётся локальной инфраструктурой в owner-only trust boundary из
ADR-0004, а не MCP-tool. Persistent означает только срок локальной mobile
session между ленивыми MCP-процессами; это не системный production daemon.
Server-side revoke по-прежнему не заявляется без официального контракта.

## Live acceptance после restart клиента

После обычного перезапуска OpenCode, без ручного запуска broker, последовательно
прошли два естественных read-only сценария:

1. Hotels-first: `tbank_hotels_summarize_bookings` вернул только количества
   активных, завершённых и отменённых бронирований без PII и order identifiers.
2. Banking-second: `tbank_banking_build_portfolio_travel_profile(days=90)`
   вернул только обезличенный профиль, диапазон цены и стратегию ранжирования;
   абсолютные суммы, счета, операции и category breakdown не раскрывались.

Каждый сценарий потребовал ровно один высокоуровневый MCP tool. Booking, payment
и другие write-операции не выполнялись. Это подтверждает устранение исходной
Hotels-first зависимости в реальном lazy-start клиенте.
