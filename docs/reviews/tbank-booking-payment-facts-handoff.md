# Booking payment facts handoff

## Scope

Один явно разрешённый bounded read собственной активной брони использован для
structure-only contract evidence. После capture реализация и тесты выполнялись
только локально с fake transport. Payment setup, `/v1/pay`, бронирование,
отмена и денежные операции не выполнялись.

## Evidence

Structure-only отчёт подтвердил в booking v1 наблюдаемые пути:

- `rateData.paymentData.paymentPrice.amount` — number;
- `rateData.paymentData.paymentPrice.currency` — string;
- `rateData.paymentData.paymentStatus` — string;
- дополнительно присутствуют `shownPrice`, `paymentPlace` и `paymentType`.

Значения, provider identifiers и raw payload в evidence не сохранены. Отчёт
находится вне репозитория с правами `0600`.

## Реализация

- Hotels `0.20.0` при выпуске `paymentHandoffRef` выполняет один booking v1 read
  внутри broker boundary.
- Broker fail-closed извлекает `paymentPrice` и raw `paymentStatus` и связывает
  их с capability.
- Banking `0.11.0` больше не принимает сумму или валюту от модели.
- Banking preview получает amount/currency только из broker capability.
- `paymentStatus` возвращается как `rawStatus` с
  `interpretation=not_interpreted`.
- Provider `orderId` и `paymentToken` не пересекают меж-MCP границу.

## Ограничения

`paymentPrice` является подтверждённым provider fact конкретной booking v1
карточки, но не считается автоматически задолженностью, доступным остатком или
разрешённой суммой списания. Raw `paymentStatus` не сопоставлен с payable/paid
state machine. Payment setup и execution остаются `NO-GO`.

## Проверки

- Hotels MCP: `48` tests, passed.
- Banking MCP и broker: `42` tests, passed.
- Local toolkit: `10` tests, passed.
- Contract manifests и offline conformance: passed.
- После единственного разрешённого capture release gate не выполнял provider
  requests.

## Следующий gate

Получить официальный или capture-backed контракт значений `paymentStatus`,
связь payment setup token с payment gateway, idempotency и reconciliation.
