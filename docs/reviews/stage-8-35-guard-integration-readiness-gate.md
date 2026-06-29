# Stage 8.35 — Guard integration readiness gate

## Цель Stage 8.35

Оценить, достаточно ли `PlanConfirmedSearchExecutionGuardUseCase` для будущего actual confirmed-search execution, или перед любым `CreateHotelSearchUseCase` call нужен отдельный idempotency/attempt skeleton.

Stage 8.35 — review/design-only gate. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI, frontend, generated clients или roadmap/root status files.

## Текущая точка входа

После Stage 8.34 есть:

- `ConfirmedSearchExecutionGuardRequest`;
- `ConfirmedSearchExecutionGuardResult`;
- `PlanConfirmedSearchExecutionGuardUseCase`;
- `ConfirmedSearchExecutionPolicy`;
- `ConfirmedSearchExecutionResult`;
- `ConfirmedSearchCreationCommandPlan.CommandReady`;
- process-local `PendingConfirmationStore`;
- existing `CreateHotelSearchUseCase`;
- Stage 8.24 consuming confirmation reply route wiring без search creation.

`PlanConfirmedSearchExecutionGuardUseCase` принимает read-only pending snapshot, проверяет session/criteria/status alignment и возвращает typed guard result. Даже matching active pending state возвращает `AllowedButBlockedUntilIdempotencyGuard`, а не разрешение на actual execution.

## Что уже есть после Stage 8.34

Internal confirmed-search chain может дойти до guarded stop point:

1. `PostConfirmationDecision.Confirmed(criteria)`.
2. `PlanConfirmedSearchCreationUseCase`.
3. `ConfirmedSearchCreationPlan.ReadyToCreateSearch`.
4. `BuildConfirmedSearchCreationCommandUseCase`.
5. `ConfirmedSearchCreationCommandPlan.CommandReady`.
6. `PlanConfirmedSearchExecutionUseCase`.
7. `ConfirmedSearchExecutionResult.PreparedButNotExecuted`.
8. `PlanConfirmedSearchExecutionGuardUseCase`.
9. `ConfirmedSearchExecutionGuardResult.AllowedButBlockedUntilIdempotencyGuard` или `Rejected(reason)`.

Ни один из этих слоев не вызывает `CreateHotelSearchUseCase`, не создает `hotelSearchId`, не возвращает `show_hotel_results`, не вызывает provider и не вызывает `markConsumed`.

## Guard integration readiness assessment

Вердикт: guard готов как pre-execution validation boundary, но не готов как direct execution input.

`PlanConfirmedSearchExecutionGuardUseCase` покрывает важные preconditions:

- active pending confirmation snapshot существует;
- pending не expired;
- pending не consumed;
- pending session совпадает с current `AssistantSessionId`;
- command session совпадает с current `AssistantSessionId`;
- command criteria совпадают с pending criteria после internal mapping;
- idempotency required before execution сохраняется как blocker.

Этого достаточно, чтобы future orchestration понимала: command и pending state согласованы. Этого недостаточно, чтобы безопасно вызвать `CreateHotelSearchUseCase`, потому что guard не знает:

- был ли уже создан search для этой confirmation;
- есть ли execution attempt in progress;
- как вернуть тот же created search на duplicate confirmation;
- что делать после lost response;
- где хранится idempotency key или attempt id;
- как связать pending confirmation, execution attempt и created search id.

Future execution use case не должен принимать `AllowedButBlockedUntilIdempotencyGuard` как “execute now”. Это stop result. Безопаснее сначала создать отдельный attempt/idempotency layer, который превращает guarded state в typed attempt decision.

Snapshot-based guard остается правильной boundary для Stage 8.34/8.35: store lookup лучше держать вне guard, в future route composition или attempt orchestration. Так guard остается deterministic и read-only.

## Idempotency / attempt assessment

Вердикт: перед actual execution нужен отдельный `ConfirmedSearchExecutionAttempt` skeleton.

Минимально нужен internal model, который связывает:

- `AssistantSessionId`;
- pending confirmation identity или snapshot;
- `ConfirmedSearchCreationCommandPlan.CommandReady`;
- attempt id или idempotency key;
- attempt status;
- optional created `HotelSearchId` после future success;
- failure classification без provider/internal leakage.

Минимальные attempt states:

| State | Назначение |
|---|---|
| `Prepared` | Guard passed, но execution еще не начат. |
| `ExecutionInProgress` | Future execution attempt начался; duplicate reply не должен создавать второй search. |
| `Succeeded` | Search создан, created search id известен и может быть reused for duplicate confirmation. |
| `FailedBeforeSearchCreation` | Search не был создан; pending может оставаться active. |
| `FailedAfterUnknownCreationState` | Нельзя безопасно утверждать, был ли search создан; duplicate/retry должны быть blocked или resolved отдельно. |
| `DuplicateOfExistingAttempt` | Repeated confirmation относится к уже известному attempt. |

Stage 8.35 не рекомендует сразу добавлять attempt store. Безопасный следующий implementation step — model/use case skeleton, который фиксирует idempotency semantics без route wiring и без execution.

Process-local attempt store может быть рассмотрен позже. Durable storage не нужна для bounded skeleton, но потребуется для production-like behavior, restart recovery и multi-instance safety.

## Pending-state lifecycle assessment

Вердикт: current `markConsumed` недостаточен для future confirmed-search execution route.

Текущий `markConsumed` подходит для Stage 8.24 non-search acknowledgement, но actual execution требует более строгого ordering:

| Событие | Pending state |
|---|---|
| Guard rejected | Не consume. |
| Guard passed but attempt missing | Не consume. |
| Attempt prepared | Не consume. |
| Execution in progress | Обычно не consume, пока search id не известен. |
| Search creation failed before creation | Не consume, если pending еще active. |
| Search creation succeeded and search id recorded | Consume после записи success/created search mapping. |
| Duplicate confirmation after known success | Можно вернуть existing search id; consume already-safe state только если mapping сохранен. |
| Unknown creation state / lost response | Не consume без idempotency resolution. |

Нужна связь:

- pending confirmation -> execution attempt;
- execution attempt -> created search id;
- consumed pending -> known successful attempt.

Без этой связи premature `markConsumed` может скрыть созданный `hotelSearchId` от user/client или создать duplicate search на retry.

## Failure / public response assessment

Вердикт: existing public shape, вероятно, достаточно для minimal backend-only route wiring позже, но нужны internal failure categories до route wiring.

Success может использовать current shape:

- `nextAction=show_hotel_results`;
- `hotelSearchId`;
- safe `assistantMessage.content`.

Failure должен использовать safe existing shape:

- `nextAction=ask_clarification` или safe boundary response;
- no `hotelSearchId`;
- no `show_hotel_results`;
- no raw provider/internal error;
- no stack trace, validation internals или raw LLM data.

До route wiring нужны internal categories:

- guard rejected;
- attempt missing;
- idempotency required;
- duplicate attempt already succeeded;
- duplicate attempt in progress;
- execution failed before search creation;
- execution state unknown;
- search/provider failure sanitized.

OpenAPI/frontend changes не нужны для Stage 8.35. Если later route wiring uses only existing success/failure response shape, отдельный OpenAPI/frontend step может не понадобиться, но это должно быть подтверждено route tests.

## Stage 7 strict handoff compatibility

Вердикт: совместимо только как explicit-confirmation exception.

Actual confirmed-search execution станет вторым automatic search creation path. Это допустимо только если future tests докажут:

- Stage 7 explicit `hotel-search;` path остается unchanged and priority-safe;
- actual execution requires active session-bound pending confirmation;
- guard result должен быть allowed и still pass attempt/idempotency checks;
- generic natural-language request does not create search;
- generic “yes” without active pending state does not create search;
- ambiguous/negative/correction/unknown replies do not create search;
- stale/expired/consumed pending state blocks execution;
- duplicate confirmation does not create duplicate search;
- `hotelSearchId` and `show_hotel_results` appear only after successful search creation.

Без этих guardrails confirmed-search execution размывает Stage 7 strict handoff boundary.

## Что не входит в Stage 8.35

- Production code changes.
- Tests.
- Route wiring.
- `Application.kt` changes.
- `AssistantLlmRouteWiringUseCase` changes.
- `PlanPostConfirmationDecisionUseCase` changes.
- `PlanConfirmedSearchCreationUseCase` changes.
- `BuildConfirmedSearchCreationCommandUseCase` changes.
- `PlanConfirmedSearchExecutionUseCase` changes.
- `PlanConfirmedSearchExecutionGuardUseCase` changes.
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
- Search created but response lost, then retry creates another search.
- Pending consumed before created search id is safely known.
- Pending stays active after success and can be reused.
- Guard snapshot becomes stale between guard and execution.
- Session/criteria mismatch is checked, but attempt identity is missing.
- Failure branch leaks provider/internal details.
- Stage 7 strict `hotel-search;` handoff becomes ambiguous.

## Рекомендуемый Stage 8.36

Safe Stage 8.36: backend-only internal execution attempt/idempotency model skeleton, no route wiring and no search execution.

Минимальная цель:

- добавить internal `ConfirmedSearchExecutionAttempt` / attempt result model;
- смоделировать attempt id или idempotency key;
- смоделировать statuses: prepared, in progress, succeeded, failed before creation, unknown creation state, duplicate;
- carry `ConfirmedSearchCreationCommandPlan.CommandReady`;
- preserve lifecycle/execution policy;
- no `CreateHotelSearchUseCase` call;
- no provider call;
- no `hotelSearchId` actual creation;
- no `markConsumed`;
- no route/runtime composition.

Attempt store skeleton должен быть отдельным более поздним шагом, если Stage 8.36 design не докажет, что store нужен сразу.

## Verdict

Stage 8.35 verdict: `PlanConfirmedSearchExecutionGuardUseCase` necessary but not sufficient for actual confirmed-search execution.

Guard готов как deterministic read-only precondition boundary. Его нельзя использовать как direct permission to call `CreateHotelSearchUseCase`. Перед любым actual execution или route wiring нужен internal execution attempt/idempotency skeleton, который сможет представить duplicate handling, success mapping, failure states и pending lifecycle ordering без создания search.
