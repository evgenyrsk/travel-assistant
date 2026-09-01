# Progress

## Current focus

Checkout-inspection release `0.30.0/0.17.0/0.16.0` опубликован и проверен
fresh-install initialize без mutations.

## Completed

- Сверены четыре checkout/promocode/extra-services/upgrade endpoints и DTO.
- Подтверждено разделение safe inspection и stateful quote updates.
- Сопоставлены текущие low-level tools, journey context и mutation gate.
- Добавлены `tbank_hotels_inspect_checkout` и
  `tbank_hotels_preview_checkout_changes`.
- Provider identifiers и cashback account numbers исключены из публичных
  ответов; IDs допуслуг не сохраняются даже во внутренней checkout inspection.
- Добавлены hermetic protocol tests для happy path, rejected promocode,
  неизвестных opaque refs, отсутствия apply calls и invalidation при смене
  тарифа.
- Синхронизированы Hotels `0.30.0`, Banking `0.17.0`, toolkit `0.16.0`,
  contract manifest, README, roadmap и review prompt.
- Полный локальный MCP gate прошёл с 74 Hotels, 52 Banking и 21 toolkit test;
  Unix-socket test отдельно прошёл вне sandbox. Provider requests: 0.
- Bounded live smoke выявил и закрыл v3 checkout response-shape mismatch;
  повторный запуск прошёл checkout/promo validation/upgrade/local preview без
  provider identifiers, booking, payment или quote write.
- Qwen release-review дал `CONDITIONAL READY` без P0-P2; закрыты все пять P3:
  promo remove убран до подтверждения read-контракта, обе формы и mismatch
  покрыты тестами, checkout failure стал terminal/no-retry, wrapper bookHash
  сверяется fail-closed, inspection TTL ограничен пятью минутами.
- Release commit `b3a8395` отправлен в `codex/tbank-hotels-mcp`.
- `tbank-hotels-mcp@0.30.0` и `tbank-mcp-local@0.16.0` опубликованы с dist-tag
  `latest`; публичный Banking wheel остаётся `0.17.0`.
- Fresh registry install подтвердил версии и Hotels MCP `initialize` `0.30.0`.

## Blocker

None.

## Next action

Зафиксировать publication checkpoint отдельным docs-коммитом; дальнейшие
license/SBOM/remote transport этапы активировать отдельной задачей.
