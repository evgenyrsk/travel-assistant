# Decisions

## Checkout inspection is a journey-level read boundary

- **Decision:** agent-facing checkout принимает `journeyId`, сам использует
  выбранный `bookHash` и возвращает нормализованные facts.
- **Reason:** модель не должна знать provider DTO/identifiers.

## Promo validation is not promo application

- **Decision:** optional `promocode` вызывает только documented validation
  endpoint. Apply/remove endpoint не вызывается инспектором.
- **Reason:** provider error taxonomy указывает на stateful promo operation.

## Applied promo removal is not exposed without read evidence

- **Decision:** public checkout preview поддерживает только `unchanged` и
  `apply_validated`; `remove` исключён из схемы.
- **Reason:** подтверждённый GET checkout v3 не содержит надёжного источника
  текущего применённого промокода.

## Checkout inspection is fresh for five minutes

- **Decision:** локальный preview отклоняет inspection старше пяти минут и
  требует повторного provider read.
- **Reason:** цена и отмена могут измениться в течение часового journey TTL.

## Extra services use opaque references

- **Decision:** provider IDs не сохраняются в checkout inspection; пользователю
  и следующему локальному preview выдаётся только process-local
  `extraServiceOptionRef` и безопасное описание услуги.
- **Reason:** предотвращает утечку идентификаторов и случайные low-level writes.

## Public flow remains booking/payment free

- **Decision:** checkout inspection и local change preview возвращают явные
  `bookingCreated=false`, `paymentStarted=false`, `checkoutModified=false`.
- **Reason:** ADR-0005 направляет фактическое оформление в hosted checkout.
