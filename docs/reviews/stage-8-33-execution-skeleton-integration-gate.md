# Stage 8.33 — Execution skeleton integration gate

## Цель Stage 8.33

Оценить, можно ли безопасно перейти от `ConfirmedSearchExecutionResult.PreparedButNotExecuted` к actual execution через `CreateHotelSearchUseCase`, или перед этим нужен отдельный idempotency / pending-state execution guard.

Stage 8.33 — review/design-only gate. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI, frontend, generated clients или roadmap/root status files.

## Текущая точка входа

После Stage 8.32 есть:

- `ConfirmedSearchExecutionPolicy`;
- `ConfirmedSearchExecutionResult`;
- `PlanConfirmedSearchExecutionUseCase`;
- `ConfirmedSearchCreationCommandPlan.CommandReady`;
- `CreateHotelSearchCommand`;
- существующий `CreateHotelSearchUseCase`;
- process-local `PendingConfirmationStore`;
- Stage 8.24 consuming confirmation reply route wiring без search creation.

`PlanConfirmedSearchExecutionUseCase` сейчас принимает `CommandReady` и возвращает `PreparedButNotExecuted` с reason `IDEMPOTENCY_GUARD_REQUIRED`. Actual execution не выполняется.

## Что уже есть после Stage 8.32

Internal confirmed-search chain может подготовить данные до execution boundary:

1. `PostConfirmationDecision.Confirmed(criteria)`.
2. `PlanConfirmedSearchCreationUseCase`.
3. `ConfirmedSearchCreationPlan.ReadyToCreateSearch`.
4. `BuildConfirmedSearchCreationCommandUseCase`.
5. `ConfirmedSearchCreationCommandPlan.CommandReady`.
6. `PlanConfirmedSearchExecutionUseCase`.
7. `ConfirmedSearchExecutionResult.PreparedButNotExecuted`.

Execution result skeleton уже фиксирует:

- pending state можно consume только после будущего successful search creation;
- failure response не должен включать search id;
- duplicate confirmation требует idempotency guard до actual execution;
- future route execution требует active session-bound pending confirmation.

## Execution integration readiness assessment

Вердикт: actual `CreateHotelSearchUseCase` call пока не готов.

Текущий execution skeleton полезен как typed stop point, но недостаточен для actual execution. Причины:

- `PreparedButNotExecuted` intentionally blocks execution;
- idempotency guard не реализован;
- нет typed execution boundary, который владеет вызовом `CreateHotelSearchUseCase`;
- нет typed mapping для `CreateHotelSearchUseCase` success/failure;
- нет связи pending confirmation -> created hotel search;
- route сейчас consumes pending state на `Confirmed` для non-search acknowledgement, а actual execution потребует другой ordering.

Будущий execution use case должен принимать `ConfirmedSearchCreationCommandPlan.CommandReady`, но не должен доверять одному command-ready input как достаточному условию. Перед execution нужны route/runtime preconditions или отдельный guard result.

Разделение responsibilities:

| Responsibility | Где должно жить |
|---|---|
| Проверить active pending confirmation | Pending-state/idempotency guard или route composition до execution. |
| Построить `CreateHotelSearchCommand` | Уже есть в `BuildConfirmedSearchCreationCommandUseCase`. |
| Выполнить actual search | Future execution boundary/use case. |
| Маппить success/failure в public response | Future route mapping после typed execution result. |

## Pending-state guard assessment

Вердикт: нужен отдельный pending-state execution guard до actual execution.

Guard должен проверять:

- active pending confirmation exists for current `AssistantSessionId`;
- pending state не expired;
- pending state не consumed;
- pending criteria соответствует command criteria;
- command session id совпадает с current route session id;
- execution не запускается из generic “yes” без active pending state;
- execution не запускается для ambiguous, negative, correction, unknown или no-active-pending outcomes.

Pending state не должен consume до successful search creation. Если search creation fails, pending state должен оставаться active, пока не expired, чтобы пользователь мог retry без повторного ввода criteria.

Process-local `PendingConfirmationStore` достаточно только для bounded Stage 8 experimental scope. Он не дает recovery after restart и не решает multi-instance behavior.

## Idempotency assessment

Вердикт: idempotency не готова для actual execution.

Риски:

- repeated “да” создает duplicate hotel searches;
- user retry after lost response не получает original `hotelSearchId`;
- search создан, но route response потерян;
- pending state consumed, но client не увидел result;
- process restart теряет mapping between pending confirmation and created search;
- parallel instances не разделяют process-local state.

Минимальный future guard должен решить:

- как идентифицировать одну confirmation execution attempt;
- где хранить created search id после successful creation;
- что возвращать при repeated confirm after success;
- что делать при failure before search creation;
- что делать при failure after search creation but before response;
- когда и где можно вызывать `markConsumed`.

Без этого actual execution не стоит подключать к routes.

## Failure / public response assessment

Вердикт: existing public response shape, вероятно, достаточен для minimal backend-only flow, но failure semantics должны появиться как typed internal results до route wiring.

Potential mapping:

| Execution result | Public response |
|---|---|
| Successful search creation | `nextAction=show_hotel_results` + `hotelSearchId` + safe assistant message. |
| Pending guard failed | `ask_clarification` или boundary message, без `hotelSearchId`. |
| Idempotency required / duplicate unresolved | `ask_clarification` или boundary message, без нового search. |
| Search creation failed | safe retry/clarification text, без `hotelSearchId`. |
| Provider/search internal failure | safe boundary text, без raw internal details. |

Failure response не должен раскрывать:

- provider internals;
- exception details;
- stack traces;
- raw LLM candidate;
- validation internals;
- command/internal policy details.

OpenAPI/frontend changes не нужны для review-only Stage 8.33. Перед backend route wiring нужно доказать тестами, что current public shape handles success/failure without new public fields.

## Stage 7 strict handoff compatibility

Вердикт: совместимо только как explicit-confirmation exception и только после guardrails.

Actual confirmed-search execution станет вторым automatic search creation path. Это допустимо только если:

- Stage 7 explicit `hotel-search;` handoff остается unchanged;
- Stage 7 path сохраняет priority для explicit format;
- confirmed-search execution requires active pending confirmation;
- generic natural-language request does not create search silently;
- generic “yes” without pending state does not create search;
- stale, expired or consumed pending state blocks execution;
- route tests доказывают отсутствие `show_hotel_results` для non-confirmed replies;
- route tests доказывают, что `hotelSearchId` появляется только после successful actual search creation.

Без этих guardrails confirmed-search execution ослабит strict handoff boundary.

## Что не входит в Stage 8.33

- Production code changes.
- Tests.
- Route wiring.
- `Application.kt` changes.
- `AssistantLlmRouteWiringUseCase` changes.
- `PlanPostConfirmationDecisionUseCase` changes.
- `PlanConfirmedSearchCreationUseCase` changes.
- `BuildConfirmedSearchCreationCommandUseCase` changes.
- `PlanConfirmedSearchExecutionUseCase` changes.
- Actual `CreateHotelSearchUseCase` call.
- Hotel provider call.
- Actual `hotelSearchId` creation.
- `show_hotel_results` response.
- `markConsumed` call.
- Public API/OpenAPI/frontend/generated clients changes.
- Durable storage, auth or booking flow.
- Roadmap/root status changes.

## Риски преждевременного actual execution / route wiring

- Duplicate search on repeated confirmation.
- Потерянный `hotelSearchId` после successful search и lost response.
- Pending confirmation consumed before failed search.
- Stale or consumed pending state reused.
- Session mismatch between pending state and command.
- Generic “yes” starts search outside active pending context.
- Internal provider/search failure leaks to public response.
- Stage 7 strict `hotel-search;` handoff becomes ambiguous.

## Рекомендуемый Stage 8.34

Safe Stage 8.34: backend-only internal pending-state/idempotency guard skeleton, no route wiring and no search execution.

Минимальная цель:

- input: `AssistantSessionId`, active pending confirmation context, `ConfirmedSearchCreationCommandPlan.CommandReady`;
- output: typed guard result, for example `AllowedToExecute`, `BlockedExpired`, `BlockedConsumed`, `BlockedSessionMismatch`, `IdempotencyRequired`;
- verify command session and pending state alignment;
- preserve lifecycle policy;
- не вызывать `CreateHotelSearchUseCase`;
- не создавать `hotelSearchId`;
- не вызывать `markConsumed`;
- no public API changes.

Alternative Stage 8.34: review-only idempotency/failure mapping gate, если guard skeleton would imply premature execution.

## Verdict

Stage 8.33 verdict: actual execution через `CreateHotelSearchUseCase` пока не готов.

Текущий `PreparedButNotExecuted` skeleton правильно останавливает flow до execution. Следующий безопасный шаг — отдельный internal pending-state/idempotency guard skeleton без route wiring, без search execution, без `hotelSearchId`, без `show_hotel_results`, без provider call и без `markConsumed`.
