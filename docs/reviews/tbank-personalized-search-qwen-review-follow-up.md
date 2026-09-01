# Follow-up независимого review T-Bank MCP release candidate

**Статус:** completed local follow-up.

**Версии:** Hotels MCP `0.23.0`, Banking/broker `0.14.0`, local toolkit
`0.6.1`.

## Scope

Закрыть неблокирующие findings независимого Qwen 3.8 Max review текущего
working tree без provider-вызовов, публикации артефактов и активации mutations.

## Результат review

Review дал `CONDITIONAL READY`: P0–P2 не найдены. Зафиксированы три P3 и одна
необязательная privacy-рекомендация:

- agent-facing order overview и cancellation preview не принимали
  process-local `bookingRef` в mobile broker-профиле;
- historical payment report неточно описывал порядок consume/validation;
- отсутствие `verifiedOperations` в legacy broker ошибочно компенсировалось
  значением `supportedOperations`;
- booking draft мог временно сохранить guest PII при недоступном execution.

## Закрытые изменения

- `tbank_hotels_get_booking_overview` и
  `tbank_hotels_preview_cancellation` принимают `bookingRef`, выполняют
  broker-side booking v1 read и не раскрывают provider identifiers;
- customer readiness теперь fail-closed: только явный массив
  `verifiedOperations` включает mobile customer reads;
- `tbank_hotels_create_booking_draft` проверяет execution readiness до разбора
  и сохранения `bookingData`; при отказе draft не создаётся, PII не сохраняется
  и клиент направляется в безопасный preview;
- historical payment report уточняет, что `accountRef` проверяется до consume,
  а amount/freshness — после одноразового consume с восстановлением через новый
  handoff.

## Проверки

- `node --check tools/tbank-hotels-mcp/src/server.mjs`;
- `npm test` в `tools/tbank-hotels-mcp`: `52/52`, только fake/local transport;
- полный `tools/tbank-mcp-local` offline verify вне sandbox: toolkit `13/13`,
  Hotels `52/52`, Banking `49/49`, manifests/conformance зелёные, provider
  requests `0`;
- `./scripts/verify.sh docs` и `git diff --check`;
- regression: legacy broker без `verifiedOperations` остаётся
  `partial_read_only_unverified`;
- regression: overview/cancellation работают через opaque `bookingRef` без raw
  identifiers;
- regression: недоступный execution не создаёт `bookingDraftId` и не возвращает
  guest PII.

## Scope control

Provider API не вызывался. Booking/payment setup, execute, cancel и другие
mutations не выполнялись и не активировались. Публикация npm/PyPI и remote
transport не входят в этот follow-up.

## Следующий gate

Синхронизировать generated manifests, выполнить полный offline gate и затем
запустить шесть естественных read-only/preview-only smoke-кейсов после полного
рестарта MCP-клиента.
