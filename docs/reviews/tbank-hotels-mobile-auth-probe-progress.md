# Progress report: Hotels mobile auth probe

**Роль документа:** completed local implementation report. Live auth evidence
этот документ не содержит и разрешением на payment/booking mutations не
является.

## Scope

- fixed-origin read-only probe для `hotels.t-bank-app.ru`;
- bounded comparison `bearer_only`, `bearer_session`, `capture_compatible`;
- безопасный обезличенный отчёт;
- offline unit/protocol tests и пользовательская инструкция.

## Реализовано

- [x] Произвольные URL, path и HTTP methods не принимаются.
- [x] Route inventory ограничен customer data, booking list, собственными
  booking/voucher/EVO reads и booking task status.
- [x] Payment setup, booking create, cancel, update и другие mutations
  отсутствуют.
- [x] Response bodies не скачиваются и не анализируются.
- [x] Token, cookie, session, PII и identifiers не включаются в отчёт.
- [x] Невалидные identifiers отклоняются до первого transport-вызова.
- [x] `x-real-ip` не синтезируется и не передаётся.

## Checks

| Проверка | Результат |
| --- | --- |
| Banking/broker/probe unit tests | `29/29 passed` |
| Hotels tests с очищением унаследованных auth settings | `36/36 passed` |
| Python compile / Node syntax | Passed |
| Banking wheel | `0.5.0`, probe entry point присутствует |
| Live Hotels requests | Probe `1.1`: два auth-effect confirmed read-only routes |
| End-to-end broker smoke | `mobile_read_only_ready`; customer/list payload shape подтверждён без вывода значений |
| Пользовательская mobile session | Использована локально CLI; credentials и response bodies не раскрыты |

## Решение

Probe `1.1` подтвердил auth effect для `customerdata` и `booking_list`: control
без Authorization получил `401`, Bearer-only — `200`. Для этих двух routes не
потребовались `sessionid`, cookies, device ID или `x-real-ip`. Banking MCP
`0.5.0` и Hotels MCP `0.11.0` подключают только эти подтверждённые reads через
broker; остальные routes остаются неподтверждёнными.

## Open gates

- наличие собственных test `orderId`/`taskId`;
- отдельное contract evidence для hotel payment linkage;
- idempotency, reconciliation и trusted confirmation для любых будущих writes.
