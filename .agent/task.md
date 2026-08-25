# Active task

**Статус:** completed

## Goal

Устранить зависимость combined Hotels/Banking MCP от порядка lazy-start: общий
mobile auth broker должен автоматически запускаться при первом обращении к
любому из двух MCP и корректно останавливаться через lifecycle/logout.

## Acceptance criteria

- [x] Сгенерированная combined-конфигурация обеспечивает broker для обоих MCP.
- [x] Hotels customer read работает, даже если Banking MCP ещё не запускался.
- [x] Banking read работает, даже если Hotels MCP ещё не запускался.
- [x] Одновременно запущенные MCP переиспользуют один owner-only broker.
- [x] Завершение одного MCP не обрывает auth второго.
- [x] Broker корректно завершается lifecycle-командой и перед logout.
- [x] Существующий `--with-broker` остаётся обратно совместимым.
- [x] Offline tests, manifests, conformance и diff hygiene проходят без provider calls.

## Constraints

- Сохранить отдельные MCP и scoped broker allowlist по ADR-0003/ADR-0004.
- Не передавать mobile credentials через MCP arguments или client config.
- Не выполнять provider calls, booking/payment mutations или live retries при реализации.

## Out of scope

- Remote transport, Keychain, server-side revoke и production daemon manager.
- Расширение broker allowlist и real booking/payment execution.
- Изменение core Kotlin backend или публичного OpenAPI.

## Definition of Done

- Lazy-start order покрыт тестами для Hotels-first и Banking-first.
- Broker lifecycle не оставляет stale socket после явной остановки.
- Документация и версии синхронизированы.
- Полный MCP offline gate проходит; live smoke повторяется только после restart клиента.

## Task-specific escalation triggers

- Требуется изменить ADR boundary или передавать credentials модели.
- Нужен production/system daemon, privileged install или external cost.
- Надёжный lifecycle невозможен без несовместимого client contract change.
