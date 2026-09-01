# Safe hotel payment handoff preview

## Scope

Реализован статический офлайн-участок композиции Hotels MCP → Banking MCP.
Provider API, booking/payment setup, `/v1/pay`, списание и реальные мутации не
вызывались и не активировались.

## Результат

- Hotels MCP `0.19.0` создаёт через общий broker короткоживущий
  `paymentHandoffRef` для process-local `bookingRef`.
- Banking MCP `0.10.0` предоставляет
  `tbank_banking_prepare_hotel_payment_handoff_preview`.
- Меж-MCP идентификатором служит только broker-issued `paymentHandoffRef`.
- Сырой Hotels `orderId` и `paymentToken` новым tool не принимаются.
- Broker подтверждает binding capability с собственной бронью; сумма и payment
  state остаются неподтверждёнными.
- Provider requests, payment setup и payment execution не выполняются.
- Legacy tool с сырым `booking_order_id` удалён из публичного MCP-контракта,
  поэтому модель не может выбрать его в обычном flow.

## Проверяемые границы

- [x] Banking принимает только `paymentHandoffRef`; `bookingRef` остаётся внутри
  Hotels MCP, а provider order ID — внутри broker boundary.
- [x] Несуществующий или протухший capability отклоняется broker.
- [x] Source account скрыт в preview.
- [x] Ответ не содержит `orderId` или `paymentToken`.
- [x] `executionAvailable=false`.
- [x] `providerRequestsPerformed=false`.
- [x] `paymentSetupPerformed=false`.
- [x] `paymentExecutionPerformed=false`.

## Следующий gate

Подтвердить read-only источник Hotels payment facts и безопасно связать его с
broker capability, не передавая provider identifiers. До этого сумма и payment state
в Banking preview остаются входными неподтверждёнными данными. Реальный payment
setup/execution остаётся `NO-GO` до официального контракта, idempotency,
reconciliation, antifraud и trusted human confirmation.

## Проверки

- Banking tests: `42`, passed, включая owner-only Unix-socket lifecycle.
- Hotels tests: `48`, passed.
- Local toolkit: `5` checks, passed.
- Contract manifests и offline conformance: passed.
- Codex CLI smoke подтверждён после перезапуска общего broker: Hotels `0.19.0`
  и Banking `0.10.0` видят payment handoff; booking binding доступен, amount
  binding и Hotels payment state остаются неподтверждёнными.
- Во время перечисленных проверок provider requests не выполнялись.
