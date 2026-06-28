# Stage 8.29 — Command construction readiness gate

## Цель Stage 8.29

Проверить, нужен ли отдельный internal step между `ConfirmedSearchCreationPlan.ReadyToCreateSearch` и будущим вызовом `CreateHotelSearchUseCase`.

Stage 8.29 — review/design-only gate. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI, frontend, generated clients или roadmap/root status files.

## Текущая точка входа

После Stage 8.28 есть:

- `PostConfirmationDecision.Confirmed(criteria)` как internal decision только после active pending confirmation;
- `ProceedWithCandidateCriteriaToHotelSearchCriteriaMapper`;
- `PlanConfirmedSearchCreationUseCase`;
- `ConfirmedSearchCreationPlan.ReadyToCreateSearch(criteria, lifecyclePolicy)`;
- existing `CreateHotelSearchCommand(sessionId, criteria)`;
- existing `CreateHotelSearchUseCase`, который создает process-local hotel search через provider boundary;
- existing public success shape для Stage 7 strict handoff: `nextAction=show_hotel_results` и `hotelSearchId`.

При этом confirmed-search plan не подключен к route/runtime composition, не создает `CreateHotelSearchCommand`, не вызывает `CreateHotelSearchUseCase`, не создает `hotelSearchId`, не возвращает `show_hotel_results` и не вызывает `markConsumed`.

## Что уже есть после Stage 8.28

`ConfirmedSearchCreationPlan.ReadyToCreateSearch` содержит:

- planned `HotelSearchCriteria`;
- `ConfirmedSearchCreationLifecyclePolicy`.

Policy фиксирует future guardrails:

- consume pending state только после будущего успешного search creation;
- не consume pending state при search failure;
- duplicate confirmation требует idempotency guard.

Plan не содержит `AssistantSessionId`. Это осознанно: session принадлежит route/runtime boundary и active pending confirmation context.

## Command construction assessment

Вердикт: отдельный command construction step нужен.

Existing `CreateHotelSearchCommand` уже подходит как target command для minimal backend flow:

| Field | Источник |
|---|---|
| `sessionId` | current `AssistantSessionId` из route/session-bound context |
| `criteria` | `ConfirmedSearchCreationPlan.ReadyToCreateSearch.criteria` |

Но собирать `CreateHotelSearchCommand` прямо внутри route branch преждевременно. Нужен отдельный internal builder/mapper, потому что он:

- удерживает route thin;
- явно связывает `ConfirmedSearchCreationPlan` с current `AssistantSessionId`;
- сохраняет boundary: command можно построить только из confirmed plan;
- позволяет добавить typed policy metadata без вызова search;
- создает место для future idempotency/failure guardrails;
- не смешивает confirmed natural-language flow со Stage 7 strict `hotel-search;` handoff.

Дополнительные поля вроде source marker, idempotency key или correlation id отсутствуют в current `CreateHotelSearchCommand`. Добавлять их прямо сейчас не нужно без отдельного implementation stage. Для Stage 8.30 достаточно internal command-builder skeleton, который возвращает command-ready plan или `CreateHotelSearchCommand` без execution.

## Session boundary assessment

Вердикт: `AssistantSessionId` должен добавляться только на command construction boundary.

Условия будущего builder:

- принимает current `AssistantSessionId` из route/runtime context;
- принимает только `ConfirmedSearchCreationPlan.ReadyToCreateSearch`;
- не берет session id из public request body;
- не берет session id из free-form text;
- не использует stale или foreign pending state;
- не создает command без session-bound active pending confirmation, если composition layer отвечает за эту проверку.

Связь pending confirmation -> created hotel search сейчас не хранится. Перед actual execution нужно решить, где будет храниться created search id для retry/idempotency. Process-local pending store может быть достаточен только для bounded backend-only experiments, но не должен описываться как durable lifecycle.

## Failure handling assessment

Вердикт: command construction можно отделить безопасно, но execution failure policy еще не готова для route search creation.

Сам command construction должен быть deterministic и почти non-failing, если inputs уже typed:

- `ReadyToCreateSearch.criteria` уже является `HotelSearchCriteria`;
- `AssistantSessionId` уже известен в route context;
- provider call не происходит.

Но после command construction остаются unresolved runtime cases:

| Scenario | Required future decision |
|---|---|
| command построен, search creation fails | Не consume pending state; вернуть safe retry/clarification response. |
| search creation succeeds, response lost | Нужен idempotency guard или возможность вернуть same `hotelSearchId`. |
| session disappears before execution | Typed failure branch, без provider call и без consume. |
| provider/search boundary returns no offers | Success может вернуть `show_hotel_results` with created search status, если existing search flow это поддерживает. |

Future route wiring не должен вызывать `markConsumed` до successful search creation. Stage 8.24 behavior для non-search acknowledgement не переносится автоматически на confirmed-search execution path.

## Idempotency assessment

Вердикт: idempotency не готова для actual search creation route wiring.

Риски:

- repeated “да” может создать duplicate hotel searches;
- user retry после lost response может не получить original `hotelSearchId`;
- pending state может быть consumed, а client не увидит result;
- process restart теряет pending confirmation;
- parallel instances не разделяют process-local pending state.

Возможные future inputs для idempotency key:

- internal pending confirmation id, если он будет введен;
- session id + criteria hash + pending created timestamp;
- separate internal command id;
- persisted mapping pending confirmation -> created hotel search.

Для Stage 8.30 не нужно решать durable idempotency. Но command builder должен быть спроектирован так, чтобы future idempotency metadata можно было добавить без public API changes.

## Public contract / OpenAPI / frontend assessment

Вердикт: existing public success shape условно достаточен для successful search, но failure/retry semantics требуют отдельного runtime gate.

Successful future response может использовать existing fields:

- `nextAction=show_hotel_results`;
- `hotelSearchId`;
- `assistantMessage.content`.

Новый `nextAction` не обязателен для happy path. Новый public confirmation id не обязателен, если route строго использует active session-bound pending confirmation.

Но current public contract не выражает:

- transient search failure;
- retry-after-created-search;
- already-created confirmed search;
- confirmation-specific failure state.

Поэтому OpenAPI/frontend changes не нужны для Stage 8.30 command builder skeleton. Перед route search creation нужен отдельный gate или tests, которые докажут, что failure/retry можно безопасно выразить через existing response shape.

## Stage 7 strict handoff compatibility

Вердикт: совместимо только как explicit-confirmation exception.

Command construction не должен менять Stage 7 strict `hotel-search;` handoff:

- existing explicit `hotel-search;` path остается unchanged;
- confirmed natural-language path может build command только после active pending confirmation and `Confirmed`;
- command не строится из ambiguous, negative, correction, unknown или missing pending state;
- command не строится из generic “yes” без active pending context;
- raw LLM candidate или validation details не попадают в command;
- docs/tests должны называть это confirmed-search flow, а не silent AI handoff.

## Что не входит в Stage 8.29

- Production code changes.
- Tests.
- Route wiring.
- `Application.kt` changes.
- `AssistantLlmRouteWiringUseCase` changes.
- `PlanPostConfirmationDecisionUseCase` changes.
- `PlanConfirmedSearchCreationUseCase` changes.
- Confirmed-search planning connection to routes.
- Command builder implementation.
- `CreateHotelSearchCommand` creation at runtime.
- `CreateHotelSearchUseCase` call.
- Hotel provider call.
- `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` call.
- Public API/OpenAPI/frontend/generated clients changes.
- Durable storage, auth or booking flow.
- Roadmap/root status changes.

## Риски преждевременного route wiring / search creation

- Duplicate search on repeated confirmation.
- Lost `hotelSearchId` after successful search but failed response.
- Pending confirmation consumed before failed search.
- Session mismatch between pending confirmation and search command.
- Generic “yes” becomes search creation outside active pending context.
- Stage 7 strict handoff loses its strict meaning.
- Failure branch exposes internal exception/provider details.
- Frontend receives `show_hotel_results` from an unreviewed confirmation-created path.

## Рекомендуемый Stage 8.30

Safe Stage 8.30: backend-only internal command builder skeleton, no route wiring and no search creation.

Минимальная цель:

- input: `AssistantSessionId` + `ConfirmedSearchCreationPlan.ReadyToCreateSearch`;
- output: typed internal command-ready result, likely containing `CreateHotelSearchCommand`;
- сохраняет lifecycle policy from plan;
- без `CreateHotelSearchUseCase` call;
- без hotel provider call;
- без `hotelSearchId`;
- без `show_hotel_results`;
- без `markConsumed`;
- targeted tests для session/criteria mapping, отсутствия side effects и route/API dependency.

Route search creation должен оставаться deferred, пока command construction, failure handling и idempotency не будут покрыты explicit tests или отдельным readiness gate.

## Verdict

Stage 8.29 verdict: split path.

Existing `CreateHotelSearchCommand` подходит как target model, но direct route wiring from `ConfirmedSearchCreationPlan.ReadyToCreateSearch` to `CreateHotelSearchUseCase` не готов. Безопасный следующий шаг — separate internal command builder skeleton. Он должен добавить `AssistantSessionId` к confirmed-search plan, сохранить lifecycle policy и остановиться до execution.
