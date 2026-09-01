# Payment contract readiness foundation

**Статус:** completed offline implementation report.
**Версии:** Hotels MCP `0.22.0`, Banking/broker `0.13.0`, local toolkit `0.5.0`.

## Scope

- усилить уже существующий preview-only payment handoff без provider writes;
- исключить дополнительную binary floating-point обработку суммы между MCP;
- проверять свежесть наблюдаемых booking facts и принадлежность выбранного
  счёта текущему Banking MCP-процессу;
- сделать remaining payment gates машиночитаемыми;
- закрепить unknown-outcome policy до появления reconciliation contract.

## Реализовано

- capability переносит `amountDecimal`, локальное время наблюдения и окно
  свежести;
- Banking проверяет неизвестный `accountRef` до поглощения handoff; сам handoff
  поглощается атомарно, после чего Banking проверяет сумму и свежесть фактов.
  При отказе capability намеренно не восстанавливается: для повторной попытки
  нужен новый handoff;
- payment intent возвращает fail-closed `executionReadiness`, запрещает
  автоматический retry и отмечает отсутствие reconciliation;
- `tbank-mcp-local payment-readiness` показывает закрытые gates и недостающие
  evidence полностью офлайн;
- статический аудит не признал банковский `/v1/pay` или известные marketplace
  gateway flows эквивалентом Hotels payment contract.

## Проверяемые границы

- [x] payment setup не выполняется;
- [x] payment execution не выполняется;
- [x] booking mutations не выполняются;
- [x] provider credentials и identifiers не добавлены в меж-MCP контракт;
- [x] неизвестный исход не приводит к автоматическому повтору;
- [x] readiness остаётся `contract_evidence_required`.

## Ограничения

`amountDecimal` предотвращает последующее использование float внутри
handoff/intent, но не восстанавливает исходное лексическое представление JSON,
уже разобранного upstream-клиентом. Официальный decimal scale, Hotels payment
status semantics, payment setup/gateway, antifraud, idempotency и reconciliation
по-прежнему не подтверждены.

## Следующий gate

Получить официальные или безопасно обезличенные contract evidence для Hotels
payment setup и gateway, status transitions, idempotency и authoritative status
lookup. Только после review разрешён отдельный bounded non-production этап с
доверенным human confirmation; production execution остаётся `NO-GO`.
