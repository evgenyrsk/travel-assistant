# Stage 8.25 — Confirmed-to-search creation readiness gate

## Цель Stage 8.25

Проверить, можно ли безопасно превращать `PostConfirmationDecision.Confirmed(criteria)` в actual hotel search creation, и зафиксировать contract/runtime guardrails до любого будущего route wiring, который вернет `show_hotel_results` и `hotelSearchId`.

Stage 8.25 — review/design-only gate. Он не меняет production code, tests, runtime behavior, routes, OpenAPI, frontend или generated clients.

## Текущая точка входа

После Stage 8.24 assistant route уже умеет обрабатывать confirmation reply без search creation:

- `ConfirmationRequired` сохраняет process-local pending confirmation state;
- active pending confirmation reply классифицируется через internal decision flow;
- `PostConfirmationDecision.Confirmed(criteria)` возвращает safe acknowledgement через existing public response shape;
- pending state consumed для `Confirmed`, `Declined`, `NeedsReplanning`;
- `Confirmed(criteria)` не создает `hotelSearchId`, не возвращает `show_hotel_results` и не запускает hotel search.

Existing Stage 7 strict `hotel-search;` handoff остается единственным automatic search creation path.

## Что уже есть после Stage 8.24

- Typed `ProceedWithCandidateCriteria` содержит destination, check-in, check-out, guests и rooms.
- `ProceedWithCandidateCriteriaValidator` уже отсекает partial, unsafe, unsupported или conflicting candidate.
- `PendingConfirmationStore` возвращает только active, non-expired, non-consumed state.
- `PlanPostConfirmationDecisionUseCase` может вернуть `Confirmed(criteria)` только при active pending state и explicit positive reply.
- `CreateHotelSearchUseCase` уже создает `HotelSearch` из `CreateHotelSearchCommand(sessionId, HotelSearchCriteria)`.
- Public response shape уже содержит optional `hotelSearchId` и `nextAction=show_hotel_results` для existing Stage 7 handoff.

## Contract readiness assessment

Вердикт: existing public response shape технически можно переиспользовать, но direct confirmed-to-search wiring пока не готов как следующий runtime step.

Можно использовать existing public fields, если search creation будет разрешена отдельным stage:

- `nextAction=show_hotel_results`;
- `hotelSearchId`;
- existing `assistantMessage.content`.

Новый `nextAction` не обязателен для минимального backend response. Новый public confirmation id тоже не обязателен, если route опирается только на session-bound active pending state.

Но contract readiness не закрывает весь риск:

- frontend contract уже знает `show_hotel_results`, но не был отдельно проверен для natural-language confirmation-created search path;
- отсутствие public confirmation id требует строгой session-bound state проверки;
- route должен явно отличать confirmed pending reply от ordinary natural-language request;
- OpenAPI/frontend изменения не нужны для mapper skeleton, но перед user-visible search creation нужен отдельный runtime/contract checkpoint.

## Runtime readiness assessment

Вердикт: criteria shape близок к required hotel search model, но перед route search creation нужен отдельный internal mapper.

`ProceedWithCandidateCriteria` покрывает обязательные поля, которые нужны `HotelSearchCriteria`:

| `ProceedWithCandidateCriteria` | `HotelSearchCriteria` |
|---|---|
| `destination` | `destination` |
| `checkInDate` | `checkInDate` |
| `checkOutDate` | `checkOutDate` |
| `guests.adults` | `guests.adults` |
| `guests.children` | `guests.children` |
| `rooms` | `rooms` |

Перед route wiring нужно добавить explicit internal mapping boundary:

- не собирать `CreateHotelSearchCommand` прямо внутри route mapping branch;
- не использовать raw LLM candidate или validation internals;
- повторно опираться только на typed, accepted, active pending criteria;
- сохранить strict date/guest/room constraints from validator;
- зафиксировать failure behavior для `CreateHotelSearchUseCase`.

Не хватает runtime decisions для:

- search creation failure;
- duplicate confirm/retry;
- response failure after created search;
- expired/consumed state race;
- process-local store reset;
- multi-instance behavior.

## State lifecycle / idempotency assessment

Вердикт: lifecycle не готов для immediate confirmed-to-search route wiring.

Главный unresolved risk — порядок consume/search:

| Вариант | Риск |
|---|---|
| `markConsumed` before search creation | Search failure может потерять pending confirmation. |
| `markConsumed` after search creation | Retry или lost response может создать duplicate search. |
| Search creation succeeds, response fails | Пользователь может не получить `hotelSearchId`, а pending уже consumed. |
| Pending remains active after failure | Повторный confirm может быть полезным, но требует idempotency rule. |

Для backend-only MVP можно продолжить с process-local store, но actual confirmed-to-search route wiring должен иметь tests на duplicate/retry behavior. Для эксплуатационно надежного поведения process-local store недостаточен; нужна отдельная storage/idempotency стратегия, но она не входит в Stage 8.25.

## Stage 7 strict handoff compatibility

Вердикт: compatible only with explicit new guardrails.

`Confirmed(criteria) -> hotel search` станет second automatic search creation path, но он может быть допустим как Stage 8 path только если:

- search запускается исключительно после active pending confirmation;
- reply классифицирован как explicit positive;
- criteria уже validated и сохранены в pending state;
- ambiguous, negative, correction, unknown и no-active-pending outcomes не создают search;
- existing strict `hotel-search;` handoff остается unchanged;
- docs/tests явно фиксируют, что это не generic natural-language auto-search.

Без этих guardrails confirmed-to-search wiring нарушит смысл Stage 7 strict handoff.

## Data leakage / safety assessment

Вердикт: safe только при typed criteria-only mapping.

Разрешено передавать в future search creation:

- destination;
- dates;
- guests;
- rooms;
- internal session id.

Запрещено:

- raw `LlmCandidate`;
- model/provider metadata;
- validation issue details;
- internal confidence или safety markers;
- sensitive free-form user text;
- hotel search request data built from unvalidated text;
- any search from ambiguous confirmation reply;
- any search without active pending state.

## Confirmed-to-search guardrails

Будущий confirmed-to-search route wiring должен выполнять все условия:

- `Confirmed(criteria)` возможен только from active pending state.
- Pending state должен быть non-expired и non-consumed на момент decision.
- `ExplicitPositive` without active pending state не должен создавать search.
- `NeedsClarification`, `Declined`, `NeedsReplanning`, `NoActivePendingConfirmation` и `Unknown` не должны создавать search.
- Search command должен собираться dedicated internal mapper.
- Mapper должен использовать только typed `ProceedWithCandidateCriteria`.
- Route не должен раскрывать raw candidate, validation issues или provider internals.
- `hotelSearchId` может появляться только после actual successful search creation.
- `show_hotel_results` может появляться только после actual successful search creation.
- Failure и duplicate retry behavior должны быть покрыты tests до включения route search creation.
- Existing Stage 7 strict `hotel-search;` path must remain unchanged.

## Что не входит в Stage 8.25

- Production code changes.
- Tests.
- Route wiring.
- `Application.kt` changes.
- `AssistantLlmRouteWiringUseCase` changes.
- Hotel search creation from confirmation reply.
- `hotelSearchId` creation from confirmation reply.
- `show_hotel_results` from confirmation reply.
- OpenAPI/frontend/generated clients changes.
- Durable storage, auth, booking flow или real provider integration.
- Roadmap/root status updates.

## Риски преждевременного search creation

- Accidental search from generic “yes” without active pending state.
- Duplicate search on retry.
- Consumed pending state with failed search.
- Lost `hotelSearchId` after response failure.
- Stale pending criteria reused after restart or process-local store reset.
- Frontend receives `show_hotel_results` from a path not covered by current UX expectations.
- Raw/internal details leak into search request or response logs.

## Рекомендуемый Stage 8.26

Safe next step: backend-only internal `ProceedWithCandidateCriteria -> HotelSearchCriteria/CreateHotelSearchCommand` mapper skeleton, с targeted tests и без route wiring.

Stage 8.26 should verify:

- full field mapping;
- no raw candidate input;
- no provider metadata;
- no public DTO dependency;
- no search creation;
- no `hotelSearchId`;
- no route/runtime behavior changes.

Confirmed-to-search route wiring should remain deferred until mapper and lifecycle/idempotency guardrails are explicit.

## Verdict

Stage 8.25 verdict: split path.

Existing public response shape is conditionally sufficient to represent a future successful confirmed search via `nextAction=show_hotel_results` and `hotelSearchId`, but immediate route search creation is not ready.

The safe next stage is an internal mapper skeleton, not direct `PostConfirmationDecision.Confirmed(criteria) -> CreateHotelSearchUseCase` route wiring.
