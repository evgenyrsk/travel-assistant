# Task

## Goal

Добавить полноценный journey-level checkout flow для выбранного тарифа Hotels
MCP: актуальная инспекция provider checkout, безопасная проверка промокода,
просмотр дополнительных услуг и upgrade без PII, бронирования или оплаты.

## Acceptance criteria

- `tbank_hotels_inspect_checkout` скрывает `bookHash`, `checkOutId`, provider
  extra-service IDs и cashback account identifiers.
- Инспекция использует выбранный journey rate и не требует provider DTO.
- Опциональный промокод валидируется, но не применяется.
- Доступные ранний заезд/поздний выезд возвращаются через opaque refs.
- Опциональный upgrade собирается из journey и не применяется.
- Локальный preview checkout-изменений не выполняет provider write.
- Существующие provider apply endpoints остаются за `prepare -> execute` и
  выключенным mutation gate; booking/payment не активируются.
- Tool schemas, runtime handlers, manifest, README и journey plan согласованы.
- Все automated tests герметичны и выполняют только fake provider calls.

## Constraints

- Не выполнять live provider requests, booking, payment, cancel или checkout
  mutations.
- Не принимать guest/card PII и не раскрывать provider identifiers.
- Не менять Kotlin backend, public application API или основной product roadmap.
- Соблюдать ADR-0003/0004/0005 и hosted checkout boundary.

## Definition of Done

- Targeted Hotels tests, toolkit contracts/conformance и repository offline
  gate проходят.
- Финальный diff проверен на scope, secrets и unrelated changes.
- Независимый Qwen review выполнен пользователем; все пять P3 findings закрыты
  до публикации.

## Escalation triggers

- Требование активировать production/provider mutations.
- Необходимость guest/card credentials или неподтверждённого auth profile.
- Конфликт с accepted ADR, который нельзя закрыть safe read-only дизайном.
