# Stage 8.27 — Mapper integration readiness gate

## Цель Stage 8.27

Проверить, можно ли безопасно использовать `ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper` в будущем `PostConfirmationDecision.Confirmed(criteria) -> hotel search creation` route wiring, или перед этим нужны отдельные failure/idempotency/lifecycle guardrails.

Stage 8.27 — review/design-only gate. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI, frontend, generated clients или roadmap/root status files.

## Текущая точка входа

После Stage 8.26 есть:

- active pending confirmation state для `ConfirmationRequired`;
- consuming confirmation reply handling без search creation;
- `PostConfirmationDecision.Confirmed(criteria)` как internal decision;
- `ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper`;
- existing Stage 7 `CreateHotelSearchUseCase` и `CreateHotelSearchCommand`;
- public response shape, который уже умеет возвращать `nextAction=show_hotel_results` и `hotelSearchId` для strict `hotel-search;` handoff.

При этом `Confirmed(criteria)` по-прежнему возвращает safe acknowledgement и consumes pending state without search creation.

## Что уже есть после Stage 8.26

`ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper`:

- принимает только typed `ProceedWithCandidateCriteria`;
- возвращает `HotelSearchCriteria`;
- маппит destination, dates, adults, children и rooms;
- не создает `CreateHotelSearchCommand`, потому что command требует `AssistantSessionId`;
- не вызывает `CreateHotelSearchUseCase`;
- не вызывает hotel provider;
- не создает `hotelSearchId`;
- не возвращает `show_hotel_results`;
- не подключен к route/runtime composition.

Targeted mapper tests покрывают field mapping, dates, guests/rooms, `children = 0`, destination, deterministic behavior и отсутствие search-side-effect markers.

## Mapper readiness assessment

Вердикт: mapper готов как internal conversion building block, но не является полной readiness для route search creation.

Mapper покрывает поля, нужные existing `HotelSearchCriteria`:

| Нужно для hotel search | Покрыто mapper |
|---|---|
| destination | yes |
| check-in date | yes |
| check-out date | yes |
| adults | yes |
| children | yes |
| rooms | yes |

Mismatch минимален:

- `ProceedWithCandidateCriteria.rooms` non-null;
- `HotelSearchCriteria.rooms` nullable because direct Stage 7 hotel-search API supports explicit transport behavior around optional rooms;
- mapper передает rooms as present value, без hidden default.

Дополнительные normalized/default fields не нужны на Stage 8.27. Defensive validation wrapper также не нужен внутри mapper, потому что validation responsibility уже находится в `ProceedWithCandidateCriteriaValidator` and confirmation planning flow.

Оставшийся gap: mapper не создает `CreateHotelSearchCommand`; session-bound command construction должен быть отдельным future composition step.

## Failure handling assessment

Вердикт: not ready for direct route search creation.

Будущий search creation path должен определить:

- что route возвращает, если `CreateHotelSearchUseCase` fails;
- должен ли pending state consumed before или after search creation;
- как не потерять active confirmation при transient failure;
- как не создать duplicate search при retry;
- как отразить failure через existing public response shape без leaking internals.

Основной риск:

| Порядок | Риск |
|---|---|
| consume before search | failure теряет pending confirmation, пользователь не может повторить confirm. |
| consume after search | retry или lost response может создать duplicate search. |
| keep active on failure | безопаснее для retry, но нужен explicit failure classification. |

До route wiring нужен internal plan/use case skeleton, который делает search-creation decision и failure branch typed, но еще не вызывает route.

## Idempotency / duplicate confirmation assessment

Вердикт: одного process-local pending store недостаточно для robust duplicate-confirm behavior.

Сценарии, которые должны быть решены до search creation route wiring:

- repeated “да” after successful search;
- response lost after successful search creation;
- user retries same confirmation request;
- pending state consumed but created search id lost to client;
- process restart between confirmation and retry;
- parallel runtime instances with different process-local stores.

Для backend-only MVP можно начать с internal non-route composition model, но route search creation не должен включаться, пока duplicate/retry behavior не покрыт explicit tests.

Durable lifecycle не требуется для Stage 8.27, но process-local state нельзя описывать как надежный across restarts or parallel instances.

## Public contract / OpenAPI / frontend assessment

Вердикт: existing public shape условно достаточно для backend-only response, но route search creation still needs a separate runtime gate.

Existing fields могут представить successful search creation:

- `nextAction=show_hotel_results`;
- `hotelSearchId`;
- `assistantMessage.content`.

Новый `nextAction` не обязателен для minimal backend-only route response. Новый public confirmation id также не обязателен, если route создает search только from active session-bound pending confirmation.

Но current public contract не различает:

- strict `hotel-search;` search;
- confirmed natural-language search;
- search-created-after-confirmation failure/retry state.

OpenAPI/frontend changes не нужны для internal use case skeleton. Перед user-visible route search creation tests and review должны подтвердить current frontend behavior для `show_hotel_results` from confirmation path.

## Stage 7 strict handoff compatibility

Вердикт: compatible only как explicit-confirmation exception, а не как silent AI handoff.

Confirmed natural-language search creation станет second automatic search creation path. Это совместимо со Stage 7 strict handoff только если future wiring guarantees:

- active pending confirmation exists for same session;
- reply is `PostConfirmationDecision.Confirmed`;
- criteria are accepted typed criteria saved from confirmation prompt;
- mapper is used only after confirmation decision;
- no search from ambiguous, negative, correction, unknown or no-active-pending outcomes;
- strict `hotel-search;` handoff remains unchanged and priority-safe;
- docs/tests explicitly describe this as explicit-confirmation flow.

## State lifecycle guardrails

Future confirmed-to-search wiring должен enforcing:

- search creation only from `PostConfirmationDecision.Confirmed(criteria)`;
- active pending state required;
- pending state must be non-expired and non-consumed;
- no search from `NeedsClarification`, `Declined`, `NeedsReplanning`, `NoActivePendingConfirmation` or `Unknown`;
- no generic “yes” without active pending state;
- no stale criteria reuse;
- no raw `LlmCandidate` or raw validation details;
- no provider metadata in public response;
- `hotelSearchId` only after successful search creation;
- `show_hotel_results` only after successful search creation;
- failure branch must not expose internal exception details;
- consume policy must be explicit for success, failure and retry.

## Что не входит в Stage 8.27

- Production code changes.
- Tests.
- Route wiring.
- `Application.kt` changes.
- `AssistantLlmRouteWiringUseCase` changes.
- `PlanPostConfirmationDecisionUseCase` changes.
- Mapper integration into runtime composition.
- Hotel search creation.
- `hotelSearchId` creation.
- `show_hotel_results`.
- `CreateHotelSearchUseCase` call.
- Hotel provider call.
- Public API/OpenAPI/frontend/generated clients changes.
- Durable storage, auth or booking flow.
- Roadmap/root status changes.

## Риски преждевременного route wiring / search creation

- Duplicate search on repeated confirmation.
- Lost `hotelSearchId` if response fails after search creation.
- Pending confirmation consumed before failed search.
- Generic “yes” routed into search creation by mistake.
- Expired/consumed pending state reused.
- Frontend receives `show_hotel_results` from an unreviewed confirmation-created path.
- Stage 7 strict handoff meaning becomes ambiguous.
- Internal criteria, validation details or provider metadata leaks into public response.

## Рекомендуемый Stage 8.28

Safe Stage 8.28: backend-only internal confirmed-search creation plan/use case skeleton, no route wiring.

Минимальная цель:

- compose `PostConfirmationDecision.Confirmed(criteria)`;
- use `ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper`;
- attach current session id into `CreateHotelSearchCommand` or a typed internal plan;
- return typed outcomes for ready-to-create, failure candidate, no-active-state and duplicate/retry cases;
- do not call `CreateHotelSearchUseCase`;
- do not create search;
- do not create `hotelSearchId`;
- do not return `show_hotel_results`.

Direct confirmed-to-search route wiring должен оставаться deferred until Stage 8.28 or a later gate resolves failure/idempotency lifecycle.

## Verdict

Stage 8.27 verdict: conditionally ready for internal composition, not ready for route search creation.

`ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper` is ready to be used by a future internal planning/use-case skeleton. It is not yet safe to wire `PostConfirmationDecision.Confirmed(criteria)` directly to `CreateHotelSearchUseCase` from the route.
