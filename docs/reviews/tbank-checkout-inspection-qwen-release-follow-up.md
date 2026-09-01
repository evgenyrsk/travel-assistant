# Qwen release-review follow-up: checkout inspection

**Статус:** completed review follow-up / Hotels `0.30.0`, Banking `0.17.0`,
toolkit `0.16.0`.

## Scope

Независимый review проверил checkout inspection, promo validation, opaque
extra-service refs, upgrade lookup, local preview, privacy boundary, journey
invalidation, manifests и release-gates. Booking/payment и quote mutations не
активировались.

## Вердикт review

- `0 P0`, `0 P1`, `0 P2`;
- `5 P3` hardening/test-coverage findings;
- исходный verdict: `CONDITIONAL READY`.

## Закрытые findings

| Finding | Follow-up |
| --- | --- |
| Public `promocodeAction=remove` не подтверждён read-контрактом | `remove` исключён из journey-схемы и runtime validation до появления contract evidence |
| Не покрыты wrapper и v3 no-match формы | Добавлены отдельные regression tests |
| Checkout transport failure возвращался как generic error | Добавлен `checkout_temporarily_unavailable` с `retryAllowed=false` и запретом low-level fallback |
| Wrapper rate не сверял `bookHash` | При наличии hash используется строгая сверка с выбранным journey rate |
| Инспекция могла устареть в пределах journey TTL | Добавлен пятиминутный TTL; preview требует повторную инспекцию после expiry |

## Проверки

- Hotels protocol/unit suite: `74/74`.
- Banking suite: `52/52`.
- Toolkit suite: `21/21` вне sandbox.
- Contract manifest, conformance и clean restart: passed.
- Provider requests в automated gates: `0`.

## Capability boundary

Search/read, customer read, banking read, checkout inspection, preview-only и
hosted checkout handoff остаются `GO`. Booking execution, payment execution и
promo/extra-services quote mutations остаются `NO-GO`.
