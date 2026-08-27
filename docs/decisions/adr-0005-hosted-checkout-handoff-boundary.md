# ADR-0005 — граница hosted checkout для публичных MCP

- **Статус:** Accepted
- **Дата:** 2026-08-27
- **Связанный этап:** experimental Banking/Hotels MCP toolstream

## Контекст

Hotels API описывает создание booking/payment tasks и hosted payment form, но
не подтверждает необходимые для безопасного публичного execution внешние
контракты: customer auth, доверенный `x-real-ip`, idempotency, recovery после
timeout и полный lifecycle payment task. Payment Swagger также содержит
raw-card и 3-D Secure endpoints, которые не должны попадать в LLM-контекст.

При этом пользователь должен иметь возможность пройти путь от естественного
запроса до оформления, даже если MCP не выполняет денежную мутацию сам.

## Решение

1. Публичный Hotels MCP покрывает search, comparison, rates и preview.
2. После выбора тарифа `tbank_hotels_create_checkout_handoff` возвращает
   безопасную HTTPS-ссылку на публичную страницу выбранного отеля и краткий
   контекст выбора.
   Для одной комнаты без детей MCP добавляет только подтверждённые публичной
   страницей параметры `dateFrom`, `dateTo` и `guests`, поэтому сохраняются
   даты и общее число взрослых. Для нескольких комнат или детей сохраняются
   только даты: неподтверждённый формат состава гостей не угадывается.
3. Handoff не содержит и не переносит token, `bookHash`, ФИО, контакты, данные
   карты, OTP, 3-D Secure или payment state; MCP не открывает URL сам.
4. Тариф не считается зарезервированным или перенесённым. Пользователь повторно
   подтверждает доступность и цену, вводит PII и оплачивает только во внешнем
   доверенном интерфейсе. `exactRateHandoffStatus` явно сообщает, что безопасный
   публичный контракт перехода на exact rate не подтверждён.
5. По умолчанию используется официальный public hotel-page route. Интегратор
   может заменить его через `TBANK_HOTELS_HOTEL_PAGE_URL_TEMPLATE` с одним `{hotelId}`;
   допускается только абсолютный HTTPS URL без credentials, query и fragment.
   Прежний generic `TBANK_HOTELS_HOSTED_CHECKOUT_URL` в новом контракте не используется,
   чтобы stale environment не возвращал environment-wide entry point вместо выбранного отеля.
6. Прямые booking/payment mutations не входят в публичный release и остаются
   fail-closed до отдельного non-production gate.
7. Banking `/v1/pay` не используется как оплата hotel order без отдельного
   linkage contract.

## Последствия

- Публичный продукт закрывает пользовательский путь без передачи платёжных
  секретов модели и без зависимости от browser agent.
- Выбор отеля сохраняется в public route (`selectionPreserved=true`). Для
  простой occupancy сохраняются даты и число взрослых; сложный состав гостей
  переносится частично и помечается соответствующими preservation-флагами.
  Exact rate и бронь не переносятся (`exactRatePreserved=false`).
- Прямой execution можно развивать отдельно, не меняя публичный безопасный
  контракт search/preview/handoff.
- Raw-card, saved-card, 3-D Secure и antifraud данные остаются вне MCP.

## Не разрешено этим ADR

- Считать handoff созданной бронью или начатой оплатой.
- Добавлять guest/card/OTP поля в MCP tool arguments.
- Кодировать `bookHash`, `rateOptionId` или неподтверждённые query-параметры
  ради перехода прямо на тариф.
- Подставлять произвольный client IP или использовать данные модели как
  доверенный antifraud context.
- Активировать production mutations только переменной окружения.
