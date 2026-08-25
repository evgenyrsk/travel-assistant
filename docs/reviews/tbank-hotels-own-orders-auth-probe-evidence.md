# Read-only auth probe на собственных Hotels-заказах

**Роль документа:** completed live-evidence report для experimental MCP
toolstream. Документ не разрешает booking/payment mutations и не меняет core
product roadmap.

## Scope

- fixed origin `https://hotels.t-bank-app.ru`;
- только собственные booking list/details;
- GET booking, voucher, EVO и task status candidate;
- no-auth control и bounded mobile auth profiles;
- без вывода credentials, identifiers, PII и response bodies.

## Live evidence 2026-08-21

| Route | Control | Mobile profile | Результат | Вывод |
| --- | --- | --- | --- | --- |
| `customerdata` | `401` | Bearer-only | `200` | auth effect confirmed |
| `booking_list` | `401` | Bearer-only | `200` | auth effect confirmed |
| booking v1 | `401` | Bearer-only | `200` | auth effect confirmed |
| voucher | `401` | Bearer-only | `200 application/pdf` | auth effect confirmed; PDF не читался |
| EVO booking | `401` | Bearer-only | `400 rate_not_found` | auth boundary passed, успешный endpoint contract не подтверждён |
| task status | — | — | не тестировался | собственный `taskId` не найден |

Own-order discovery прочитал booking list и не более пяти booking details.
Идентификаторы использовались только внутри локального процесса. Отчёт probe
`1.2` содержал только статусы, content type, безопасный provider code и
структурные флаги.

## Safety gates

- [x] `readOnly=true`.
- [x] `mutationsAttempted=false`.
- [x] Credentials и identifiers не включены в отчёт.
- [x] Voucher PDF не читался и не сохранялся.
- [x] EVO error body не включён; извлечён только allowlisted code.
- [x] Отсутствующий `taskId` не подбирался.

## Решение

Mobile Bearer можно считать подтверждённым для voucher на указанном origin и
route. Включать его в broker/MCP следует только после выбора безопасной модели
доставки PDF: файл содержит PII и не должен автоматически попадать в LLM
context. EVO и task status в broker allowlist не добавляются.

## Следующий bounded этап

1. Спроектировать explicit-user-action tool для voucher с owner-only local
   storage либо binary MCP content без автоматического логирования содержимого.
2. Добавить broker operation только вместе с size/content-type limits и
   privacy tests.
3. Не повторять EVO на случайных заказах; уточнить семантику `rate_not_found` у
   владельца API или использовать специально подходящий собственный заказ.
4. Task status проверять только при появлении настоящего собственного `taskId`.
