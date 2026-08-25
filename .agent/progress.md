# Progress

## Current focus

Lifecycle общего auth broker реализован и проверен офлайн.

## Completed

- Кейсы search, breakfast и booking preview прошли read-only/preview-only smoke.
- Customer summary и portfolio profile сначала не прошли из-за недоступного broker.
- Ручной диагностический запуск broker подтвердил, что сами оба flows исправны.
- Root cause локализован: generated combined config назначает `--with-broker`
  только Banking MCP, тогда как OpenCode запускает MCP лениво.
- Временный диагностический broker остановлен; socket больше не работает.
- Combined launcher обоих MCP автоматически обеспечивает один persistent broker.
- Hotels-first, Banking-first и одновременный запуск прошли Unix-socket тест.
- Graceful lifecycle shutdown и остановка через logout/stop-broker покрыты тестом.
- Версии обновлены: Banking/broker `0.14.0`, toolkit `0.6.0`.
- Полный release gate прошёл: toolkit 11, Hotels 49, Banking 48; provider calls 0.
- После restart OpenCode Hotels-first summary и Banking-second portfolio profile
  прошли без ручного broker; по одному высокоуровневому tool на сценарий.

## Blocker

None.

## Next action

Подготовить commit/push только по отдельному явному запросу пользователя.
