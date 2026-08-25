# Payment contract readiness review follow-up

**Статус:** completed offline P3 follow-up.
**Версии:** Hotels MCP `0.22.0`, Banking/broker `0.13.1`, local toolkit `0.5.0`.

## Scope

Независимый review не выявил P0–P2 и признал локальный read-only/preview-only
release готовым. Этот follow-up закрывает два найденных P3 без расширения
payment полномочий.

## Закрытые findings

- `payloadHash` теперь вычисляется через HMAC-SHA256 со случайным per-process
  pepper. Сырой provider account ID не раскрывается и не допускает проверку
  предполагаемых значений обычным SHA-256 перебором по известным полям preview.
- Если capability уже атомарно поглощён, а последующая validation отклоняет
  facts, ошибка требует создать новый payment handoff вместо повтора старого
  `paymentHandoffRef`.

## Scope control

- payment setup и execution не добавлены;
- provider API не вызывается;
- `/v1/pay` не переиспользуется;
- readiness остаётся `contract_evidence_required`;
- unknown-outcome policy остаётся `do_not_retry_automatically`.
