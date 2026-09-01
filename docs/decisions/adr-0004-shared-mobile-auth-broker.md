# ADR-0004 — общий локальный broker мобильной авторизации

- **Статус:** Accepted
- **Дата:** 2026-08-21
- **Связанный этап:** experimental Banking/Hotels MCP toolstream

## Контекст

Banking MCP и Hotels MCP должны подключаться к любому MCP-клиенту как вместе,
так и по отдельности. При этом повторный вход по телефону для каждого MCP
ухудшает UX, а передача mobile token через prompt, tool arguments или общую
конфигурацию MCP увеличивает риск утечки. Refresh mobile session вращает
`refresh_token`, поэтому два независимых процесса не должны одновременно
обновлять одну копию сессии.

## Решение

1. Banking MCP и Hotels MCP остаются отдельными MCP-серверами и не требуют друг
   друга для запуска.
2. Mobile session принадлежит локальному `tbank-auth-broker`, который работает
   вне LLM через Unix socket с правами владельца.
3. Broker не возвращает access token, refresh token, cookies, OTP, пароль или
   PIN. Он предоставляет только версионированный allowlist высокоуровневых
   операций.
4. Banking MCP сохраняет автономный direct-file режим, если broker не настроен.
5. Hotels MCP сохраняет автономные service-JWT/static-token профили. Mobile auth
   является отдельным опциональным профилем для endpoint-ов, совместимость
   которых подтверждена capture, контрактом или read-only проверкой.
6. Первый allowlist включает read-only операции Banking и подтверждённый
   upstream-вызов `GET https://hotels.t-bank-app.ru/api/v1/hotels/bookings/{id}`.
   Неизвестные Hotels routes и любые денежные операции не проксируются.
7. Один запущенный broker может одновременно обслуживать оба MCP. Если к
   интегратору подключён только один MCP, он использует тот же broker независимо
   от второго MCP.
8. Broker protocol v2 принимает явный `client` scope и применяет разные
   allowlist: Banking не вызывает Hotels operations, Hotels не получает Banking
   operations. Это защищает контракт от случайного расширения полномочий, но не
   является аутентификацией процесса.
9. Owner-only Unix socket, session-файл и процессы одного OS-пользователя
   образуют одну локальную границу доверия. Защита от вредоносного процесса того
   же пользователя требует отдельного OS-user/sandbox/Keychain решения.
10. В combined-режиме broker является единственным владельцем refresh. Direct-file
    MCP для того же session-файла параллельно не запускается; advisory file lock
    остаётся дополнительной защитой от случайной гонки.
11. Сгенерированный combined launcher каждого MCP проверяет broker и при
    необходимости запускает один общий process-local service. Broker не
    принадлежит жизненному циклу отдельного MCP: он переживает закрытие одного
    stdio-процесса, явно останавливается при local logout или lifecycle shutdown.

## Последствия

- Пользователь входит по телефону один раз локально.
- Обновление mobile session имеет одного владельца и не создаёт гонку между MCP.
- Отключение Banking MCP не ломает Hotels service-auth search; отключение Hotels
  MCP не ломает Banking MCP.
- Mobile auth не считается доказанным для всех защищённых Hotels endpoints.
  Поддержка расширяется только по endpoint matrix с безопасными проверками.
- Broker является локальной инфраструктурой авторизации, а не третьим MCP и не
  выставляет tools модели.
- Lazy-start порядок MCP-клиента не влияет на доступность mobile reads;
  одновременный запуск двух MCP переиспользует один owner-only socket.
- Локальный `--logout` удаляет session-файл, но не считается server-side revoke,
  пока официальный logout endpoint не подтверждён.

## Не разрешено этим ADR

- Передавать токены через LLM или MCP arguments.
- Автоматически пробовать mutating endpoints.
- Считать принадлежность сервисов одному супераппу доказательством одинаковой
  auth-схемы.
- Выполнять бронирование, оплату, отмену или подтверждение без отдельного gate.
