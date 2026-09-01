# Безопасная локальная выдача Hotels voucher

**Статус:** завершённый implementation report для Hotels MCP `0.14.0` и
Banking/auth broker `0.7.0`.

## Scope

- fixture/fake-transport реализация выдачи PDF собственной брони;
- непрозрачный `bookingRef` на agent-facing границе;
- broker-side проверка и owner-only локальное хранение PDF с TTL;
- запрет inline PDF в MCP JSON и booking overview;
- герметичные protocol/unit tests и синхронизация tool-local документации.

## Out of scope

- новые production API calls или повторный mobile auth probe;
- чтение, распознавание, вложение или суммаризация PDF;
- EVO/task status, booking/payment/cancel mutations;
- remote transport, multi-user storage, OS Keychain и production rollout.

## Результат

- `tbank_hotels_save_voucher` принимает только process-local `bookingRef` и
  вызывает allowlisted `hotels.save_voucher_v1` локального auth broker.
- Provider `orderId` остаётся внутри Hotels MCP/broker boundary.
- Broker принимает binary response, допускает только `application/pdf`,
  сигнатуру `%PDF-` и размер не более 5 MiB.
- Документ записывается в случайный файл с правами `0600` внутри каталога
  `0700`; default TTL — 15 минут, допустимый диапазон — 60–3600 секунд.
- MCP возвращает путь и безопасные метаданные, но не PDF/base64, PII документа,
  credentials или provider identifier.
- Legacy `tbank_hotels_get_voucher` отклоняет inline delivery;
  `tbank_hotels_get_booking_overview` не загружает binary voucher.

## Проверки

- [x] `node --check tools/tbank-hotels-mcp/src/server.mjs`.
- [x] Hotels MCP: 40/40 tests, fake transport, без внешней сети.
- [x] Banking/broker: 37 tests total — 36 passed, один Unix-socket test
  пропущен sandbox.
- [x] Проверены права `0600`/`0700`, PDF type/signature и отсутствие content в
  broker/MCP response.
- [x] Production API и пользовательская session не использовались.

## Решения

- Сохранение ваучера считается явной локальной write-операцией, но не provider
  mutation: annotations — `readOnlyHint=false`, `destructiveHint=false`,
  `idempotentHint=false`.
- Путь к локальному файлу можно показать пользователю; модель не должна читать,
  парсить, прикреплять или загружать PDF без отдельного явного запроса.
- Local filesystem boundary подходит только для desktop/self-hosted профиля и
  не переносится автоматически в remote/multi-tenant deployment.

## Следующий шаг

Статически описать и проверить цепочку hotel order → Hotels payment state →
Banking payment preview только на fixtures/fake transport. Реальные payment
calls остаются заблокированы contract/security gate.
