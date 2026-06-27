# Stage 8.24 — Consuming confirmation reply route wiring

## Цель Stage 8.24

Подключить `PlanPostConfirmationDecisionUseCase` к assistant runtime только для безопасной non-search обработки confirmation reply.

Stage 8.24 может читать active pending confirmation state, классифицировать reply и вызывать `markConsumed` по lifecycle rules. Stage 8.24 не создает hotel search, не создает `hotelSearchId` и не возвращает `show_hotel_results` для confirmation reply.

## Что было изменено

- `AssistantLlmRouteWiringUseCase` теперь проверяет active pending confirmation после Stage 7 explicit `hotel-search;` handoff guard.
- Если active pending confirmation есть, user reply обрабатывается через `PlanPostConfirmationDecisionUseCase`.
- `PostConfirmationDecision` маппится в existing public response shape через `assistantMessage.content` и `nextAction=ask_clarification`.
- `markConsumed` вызывается только для final lifecycle outcomes:
  - `Confirmed`;
  - `Declined`;
  - `NeedsReplanning`.
- Existing LLM route behavior сохраняется для generic user messages без active pending confirmation.
- Existing Stage 7 strict `hotel-search;` handoff остается приоритетным search creation path.

## Production files

Изменен:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantLlmRouteWiringUseCase.kt`.

Новые production files не создавались.

## Tests

Изменен:

- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`.

Покрытие добавлено или обновлено для:

- positive reply with active pending state -> safe acknowledgement, `ask_clarification`, no `hotelSearchId`, pending consumed;
- positive reply without active pending state -> existing LLM path, no confirmed path, no search;
- ambiguous reply with active pending state -> clarification, pending remains active;
- negative reply with active pending state -> neutral response, pending consumed;
- correction reply with active pending state -> ask for corrected criteria, pending consumed;
- unknown reply with active pending state -> safe clarification, pending remains active;
- confirmation prompt still saves pending state;
- explicit `hotel-search;` handoff still creates search as Stage 7 behavior;
- public response shape does not expose raw/internal fields.

## Route integration flow

Flow inside `AssistantLlmRouteWiringUseCase`:

1. Existing assistant boundary accepts the user message.
2. Existing Stage 7 explicit `hotel-search;` handoff can create search before LLM/post-confirmation logic.
3. If message is explicit `hotel-search;`, the accepted Stage 7 result is returned as before.
4. If message is not explicit `hotel-search;`, route wiring checks active pending confirmation by session and current time.
5. If active pending confirmation exists:
   - call `PlanPostConfirmationDecisionUseCase`;
   - map `PostConfirmationDecision` to safe public text outcome;
   - call `markConsumed` only for allowed final lifecycle outcomes.
6. If active pending confirmation does not exist, existing Stage 8.15 LLM decision flow runs unchanged.

## `PostConfirmationDecision` public mappings

| Internal decision | Public response | Pending state |
|---|---|---|
| `Confirmed(criteria)` | `nextAction=ask_clarification`; safe acknowledgement text; no `hotelSearchId`; no `show_hotel_results`. | `markConsumed`. |
| `NeedsClarification` | `nextAction=ask_clarification`; ask user to confirm, cancel, or correct criteria. | Keep active. |
| `Declined` | `nextAction=ask_clarification`; neutral message that search will not start. | `markConsumed`. |
| `NeedsReplanning` | `nextAction=ask_clarification`; ask for corrected destination, dates, guests, and rooms. | `markConsumed`. |
| `NoActivePendingConfirmation` | Defensive safe clarification. Route normally does not call this branch without active state. | No consume. |
| `Unknown` | `nextAction=ask_clarification`; safe clarification without internal reason. | Keep active. |

No new public fields or `nextAction` values were added.

## Lifecycle / `markConsumed` behavior

`markConsumed` is called only after final internal decision mapping for:

- `Confirmed`;
- `Declined`;
- `NeedsReplanning`.

`markConsumed` is not called for:

- `NeedsClarification`;
- `Unknown`;
- `NoActivePendingConfirmation`;
- ordinary user messages without active pending confirmation;
- explicit Stage 7 `hotel-search;` handoff.

This prevents stale confirmed/declined/replanned criteria reuse while preserving the pending confirmation for ambiguous or unknown replies until TTL expiry.

## Confirmed-without-search boundary

`Confirmed(criteria)` is only an acknowledgement in Stage 8.24.

It does not:

- transform criteria into hotel search request;
- create or persist hotel search;
- call hotel provider;
- create `hotelSearchId`;
- return `show_hotel_results`;
- expose raw criteria payload or internal metadata in public response.

## Hotel search creation boundary

The only current automatic search creation path remains Stage 7 strict explicit format:

```text
hotel-search; destination=Rome; check-in=2026-07-01; check-out=2026-07-04; adults=2; rooms=1
```

Confirmation reply handling never creates search in Stage 8.24.

## Public API / OpenAPI / frontend / generated clients verdict

- Public API request/response shape не изменен.
- Новые public fields не добавлены.
- Новые `nextAction` values не добавлены.
- OpenAPI contracts не менялись.
- Frontend не менялся.
- Generated clients не создавались и не обновлялись.

## Stage 7 strict handoff compatibility

Совместимо.

Stage 8.24 сохраняет Stage 7 behavior:

- explicit complete `hotel-search;` request creates search;
- confirmation reply does not create search;
- confirmation reply does not create `hotelSearchId`;
- confirmation reply does not return `show_hotel_results`;
- generic natural-language messages without active pending confirmation still use existing LLM path.

## Durable storage limitation

`PendingConfirmationStore` remains process-local/in-memory.

Stage 8.24 не добавляет:

- database;
- filesystem persistence;
- Redis/cache service;
- cross-instance synchronization;
- account-level ownership;
- recovery after restart.

TTL and consumed behavior are internal safety boundaries, not durable lifecycle guarantees.

## Real provider / network / access-key verdict

Stage 8.24 не добавляет:

- real LLM provider;
- real hotel provider;
- external calls;
- access keys or environment variables;
- provider-specific configuration.

## Риски и ограничения

- Pending confirmation can be lost on process restart.
- Parallel runtime instances do not share pending confirmation state.
- Public contract still does not expose structured confirmation lifecycle.
- `Confirmed(criteria)` acknowledges confirmation but does not start search.
- Search creation after confirmation still needs a separate future contract/runtime gate.
- Ambiguous/unknown replies keep pending state active until TTL, which is safe but still text-only.

## Рекомендуемый Stage 8.25

Stage 8.25 — review/design-only confirmed-to-search creation readiness gate.

Минимальная цель:

- определить, можно ли превращать `Confirmed(criteria)` в hotel search creation;
- зафиксировать required guardrails before any search creation;
- решить, нужен ли public contract/OpenAPI/frontend update перед search creation;
- подтвердить lifecycle ordering между `Confirmed`, `markConsumed`, search creation и error handling;
- сохранить Stage 7 strict handoff as existing automatic path until explicit new stage allows another path.

Immediate search creation без readiness gate не рекомендуется.

## Verdict

Stage 8.24 выполнен как backend-only consuming confirmation reply route wiring без search creation. `PlanPostConfirmationDecisionUseCase` подключен только в active pending confirmation context. `Confirmed`, `Declined` и `NeedsReplanning` consume pending state; `NeedsClarification` и `Unknown` keep pending active. Public API shape, OpenAPI, frontend, generated clients, durable storage, real provider work, hotel search creation, `hotelSearchId` creation и `show_hotel_results` для confirmation reply не добавлены.
