# Совместный safe-smoke Banking MCP и Hotels MCP

**Роль документа:** completed implementation/smoke follow-up для отдельного
experimental MCP toolstream. Документ не меняет core product roadmap и не
разрешает booking/payment execution.

## Scope

- совместное подключение Banking MCP и Hotels MCP через owner-only auth broker;
- read-only account/customer/order routes;
- локальные `payment preview` и `booking preview` без внешнего write;
- минимизация identifiers в agent-facing mobile booking flow;
- автоматизированный privacy-safe smoke report.

## Live evidence 2026-08-21

| Проверка | Результат |
| --- | --- |
| Два MCP в OpenCode | Connected |
| Banking mobile session | Ready через auth broker |
| Accounts / spending summary / travel profile | Passed; raw transactions не возвращались |
| Hotels customer / booking list | Passed через mobile Bearer |
| Hotels booking v1 | Passed на собственной брони через mobile Bearer |
| Hotels search / rates | Passed с bounded partial-search metadata |
| Banking payment preview | `preview_only`, execution unavailable |
| Hotels booking preview | `preview_only`, PII не собирались, HTTP write не выполнялся |
| Booking/payment execution | Не выполнялись и оставались disabled |

Live-smoke не выводил значения PII в итоговом отчёте, однако MCP client trace
показывал raw `orderId` как tool argument. Это стало основанием для следующего
privacy hardening, а не для расширения полномочий.

## Реализованный follow-up

- Hotels MCP `0.12.0` публикует `hotels.get_booking_v1` в
  `verifiedOperations` вместе с `get_customer` и `list_bookings`.
- В mobile broker-режиме `list_bookings` заменяет provider `orderId` на
  process-local `bookingRef` с TTL и bounded storage.
- `get_booking` принимает `bookingRef`, разрешает raw identifier только внутри
  MCP и удаляет `orderId`/`bookingId` из результата.
- Direct static/service API-профиль сохраняет низкоуровневый `orderId` contract.
- Banking MCP `0.6.0` добавляет `tbank-mcp-safe-smoke` с обязательным
  `--acknowledge-read-own-data`.
- Runner передаёт обоим MCP один явно настроенный либо стандартный owner-only
  broker socket; Hotels service search остаётся опциональной отдельной
  readiness-проверкой.
- Smoke-runner использует фиксированный tool inventory и выводит только
  readiness/pass/fail; credentials, PII, provider/process-local identifiers,
  суммы и содержимое броней в отчёт не включаются.

## Сохранённые gates

- Voucher, EVO booking и task status не добавлены в broker allowlist без
  отдельного route-level evidence.
- Booking create, cancel/update, payment setup и `/v1/pay` не вызываются.
- `x-real-ip`, idempotency, trusted confirmation и reconciliation остаются
  внешними blockers для execution.
- Общий broker не превращает Banking и Hotels в один MCP: оба сервера можно
  подключать вместе или по отдельности.

## Следующий ограниченный шаг

Проверять voucher/EVO/task status только read-only probe на собственных
identifiers и добавлять каждый route в allowlist отдельно после evidence.
Payment linkage остаётся отдельным исследованием без денежных вызовов.
