# Follow-up финального Qwen-аудита T-Bank MCP

**Роль:** completed review follow-up / quality gate.

**Проверенный checkpoint:** Hotels `0.26.0`, Banking `0.16.0`, local toolkit
`0.9.0`.

## Границы

- анализировался переданный review-only отчёт по текущему working tree;
- provider API, mobile session, бронирование и оплата не вызывались;
- production execution не активировался;
- Kotlin backend и публичный Travel Assistant API не изменялись.

## Вердикт исходного аудита

| Tier | Вердикт |
|---|---|
| `hotels_read` | `READY` |
| `customer_read` | `READY` |
| `banking_read` | `READY` |
| `preview_only` | `READY` |
| `booking_execute` | `NO-GO` |
| `payment_execute` | `NO-GO` |

Аудит не выявил P0–P2. Четыре P3 относились к защите от рассинхронизации
версий, декларации MCP annotations, полноте runtime dispatcher и устаревшему
editable metadata локального Banking virtual environment.

## Закрытие findings

- [x] Hotels-тесты сравнивают runtime version с `package.json`; Banking-тесты
  сравнивают runtime version с `pyproject.toml`; artifact tests используют тот
  же runtime version вместо независимых литералов.
- [x] Нестандартные annotations объявлены рядом с соответствующими tools;
  stdio adapter больше не содержит специальные имена tools.
- [x] Runtime dispatch переведён на единый registry; regression test требует,
  чтобы каждый объявленный tool имел обработчик.
- [x] Локальный Banking editable install обновлён с `0.14.0` до `0.16.0` без
  загрузки зависимостей.

## Проверки

- `Hotels`: 58/58 hermetic tests;
- `Banking/broker/probe/smoke/packaging`: 52/52;
- local toolkit: 14/14, включая Unix-socket lifecycle;
- manifests и conformance обоих MCP совпадают;
- provider requests: 0.

## Остаточные границы

Read-only/preview-only release candidate готов к bounded human smoke после
перезапуска клиентов. `booking_execute` и `payment_execute` остаются `NO-GO`:
этот follow-up не добавляет customer execution auth, trusted client IP,
provider idempotency или timeout reconciliation.
