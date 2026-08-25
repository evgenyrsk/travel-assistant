# Hardening payment handoff preview

## Scope

Этап закрывает подтверждённые P2/P3 внешнего review для локальной цепочки
Hotels → auth broker → Banking. Изменения ограничены `preview_only`, выполняются
без provider-вызовов и не активируют payment setup, `/v1/pay`, бронирование,
отмену или иные mutations.

## Результат

- [x] Документация честно учитывает один booking v1 read при выпуске handoff.
- [x] Readiness доступен только при reachable broker и активной mobile session.
- [x] Readiness использует `bookingBindingSupported`; verified относится только
  к конкретному выпущенному capability.
- [x] Raw provider status представлен как `paymentStatusObservation` с
  `interpretation=not_interpreted`.
- [x] `paymentHandoffRef` одноразовый и атомарно поглощается первым Banking
  preview; повторное разрешение отклоняется локально.
- [x] Store остаётся bounded и удаляет старейший capability при переполнении.
- [x] Structure-only inspector маскирует также короткие смешанные dynamic keys
  и честно помечает эвристическую природу masking.
- [x] Добавлены отрицательные тесты readiness, interpretation, replay и
  eviction.

## Границы безопасности

Payment price остаётся наблюдаемым provider fact и не называется задолженностью
или разрешённой суммой списания. Payment status не интерпретируется. Перед
будущим денежным действием всё ещё нужны официальные setup/gateway contracts,
decimal-safe money format, свежая сверка брони, idempotency, reconciliation,
antifraud-контекст и trusted human confirmation.

## Проверки

- Hotels MCP: `49` tests.
- Banking MCP и broker: `44` tests; Unix-socket test требует среду с socket.
- Local toolkit: `10` tests; launcher socket test может быть пропущен в sandbox.
- Contract manifests и offline conformance: обязательный release gate.
- Provider requests: `0`.

## Out of scope

Реальные Hotels/Banking API, capture собственной брони, чтение secrets/evidence,
payment setup, `/v1/pay`, mutations, remote transport и production rollout.
