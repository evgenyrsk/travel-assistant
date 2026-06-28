# Stage 8.31 — Confirmed-search execution readiness gate

## Цель Stage 8.31

Проверить, достаточно ли текущих internal layers для безопасного будущего confirmed-search execution перед любым фактическим вызовом `CreateHotelSearchUseCase`.

Stage 8.31 фиксирует policy для:

- failure response;
- idempotency/retry;
- consume ordering;
- public response mapping;
- Stage 7 strict `hotel-search;` handoff compatibility.

Stage 8.31 — review/design-only gate. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI, frontend, generated clients или roadmap/root status files.

## Текущая точка входа

После Stage 8.30 есть:

- `PostConfirmationDecision.Confirmed(criteria)`;
- `ConfirmedSearchCreationPlan.ReadyToCreateSearch`;
- `BuildConfirmedSearchCreationCommandUseCase`;
- `ConfirmedSearchCreationCommandPlan.CommandReady`;
- существующий `CreateHotelSearchCommand`;
- существующий `CreateHotelSearchUseCase`;
- process-local `PendingConfirmationStore`;
- Stage 8.24 consuming confirmation reply route wiring без search creation.

При этом confirmed-search execution не подключен к routes. `Confirmed(criteria)` по-прежнему возвращает безопасное подтверждение и не создает search.

## Что уже есть после Stage 8.30

Internal confirmed-search chain уже может подготовить command-ready данные без execution:

1. `PostConfirmationDecision.Confirmed(criteria)`.
2. `PlanConfirmedSearchCreationUseCase`.
3. `ConfirmedSearchCreationPlan.ReadyToCreateSearch`.
4. `BuildConfirmedSearchCreationCommandUseCase`.
5. `ConfirmedSearchCreationCommandPlan.CommandReady(command, lifecyclePolicy)`.

`CommandReady` содержит:

- `CreateHotelSearchCommand(sessionId, criteria)`;
- `ConfirmedSearchCreationLifecyclePolicy`.

Ни один слой в этой цепочке:

- не вызывает `CreateHotelSearchUseCase`;
- не вызывает hotel provider;
- не создает `hotelSearchId`;
- не возвращает `show_hotel_results`;
- не вызывает `markConsumed`;
- не пишет в pending store.

## Execution readiness assessment

Вердикт: direct route execution пока не готов.

Текущих internal layers достаточно, чтобы собрать command-ready объект, но недостаточно, чтобы безопасно выполнять search из route.

Обязательные preconditions для будущего execution:

- active pending confirmation существует для того же `AssistantSessionId`;
- pending confirmation не expired и не consumed на момент decision;
- reply классифицирован как explicit positive;
- decision является `PostConfirmationDecision.Confirmed`;
- command построен из `ConfirmedSearchCreationCommandPlan.CommandReady`;
- command session id совпадает с current assistant session;
- execution запрещен для ambiguous, negative, correction, unknown и no-active-pending outcomes;
- failure и duplicate behavior выражены typed result, а не скрыты в неявных route branches.

Перед route wiring нужен отдельный execution layer. Он должен принимать `ConfirmedSearchCreationCommandPlan.CommandReady`, вызывать search execution boundary только при явном разрешении и возвращать typed success/failure result.

## Consume ordering assessment

Вердикт: consume после успешного search creation.

Политика:

| Момент | Решение |
|---|---|
| До command construction | Не consume. |
| Только после command construction | Не consume. |
| До вызова `CreateHotelSearchUseCase` | Не consume. |
| После успешного search creation | Consume pending state. |
| После search failure | Оставить pending active, если он еще не expired. |
| После duplicate/idempotent success | Consume только когда созданный или переиспользованный search id безопасно известен. |

Обоснование:

- consume before search рискует потерять confirmation при failure;
- consume after success matches `ConfirmedSearchCreationLifecyclePolicy`;
- сохранение pending active при failure позволяет retry без повторного ввода всех criteria;
- duplicate prevention нужно решить до включения фактического execution.

Открытый вопрос: если search успешно создан, но response потерян, немедленный consume может скрыть созданный `hotelSearchId` от клиента. До безопасного route execution нужен idempotency guard или mapping к уже созданному search.

## Failure response assessment

Вердикт: failure, вероятно, можно отразить через existing public response shape, но сначала нужны typed internal results для разных failure cases.

Будущий execution layer должен различать минимум:

| Failure | Public mapping candidate |
|---|---|
| no active pending confirmation | `ask_clarification`; попросить пользователя снова описать hotel request. |
| command/session mismatch | safe boundary/clarification; без `hotelSearchId`. |
| session missing in `CreateHotelSearchUseCase` | safe boundary/clarification; без provider details. |
| search/provider failure | `ask_clarification` или boundary text с retry wording; без `hotelSearchId`. |
| duplicate confirmation with known search | возможно вернуть existing `hotelSearchId`, но только после idempotency design. |
| duplicate confirmation without known search | safe clarification; без нового search. |

Для минимального failure handling пока не нужен новый `nextAction`. Existing `ask_clarification` или boundary response могут быть достаточны для backend-only flow, если формулировка ясная:

- “Не удалось безопасно запустить поиск. Подтвердите еще раз или уточните параметры.”
- “Подтверждение найдено, но запуск поиска не завершился. Я не создал новый поиск.”

Будущий failure mapping не должен раскрывать exception details, provider internals, stack traces, validation issue internals или raw model data.

## Idempotency / retry assessment

Вердикт: idempotency остается главным blocker для route execution.

Риски:

- повторное “да” может создать duplicate hotel searches;
- пользователь делает retry после потерянного response;
- search успешно создан, но pending state consumed до того, как клиент увидел `hotelSearchId`;
- process restart теряет pending confirmation;
- parallel instances не разделяют process-local state.

Минимальные future guardrails:

- execution path должен иметь один session-bound pending confirmation source;
- execution должен быть idempotent для того же pending confirmation, где это возможно;
- duplicate confirmation after success не должен создавать второй search;
- failure before search creation не должен consume pending confirmation;
- failure after search creation должен либо вернуть известный search id, либо избежать silent duplicate при retry;
- process-local limitation должен оставаться явным.

Для bounded backend-only scope process-local idempotency metadata может быть достаточно для skeleton. Для production-like behavior потребуется durable storage, но это вне Stage 8.31.

## Public response mapping assessment

Вердикт: success может использовать existing public shape; failure должен оставаться в safe existing response shape, пока не доказана необходимость нового contract.

Candidate mapping для success:

- `nextAction=show_hotel_results`;
- `hotelSearchId`;
- safe `assistantMessage.content`;
- без новых public fields.

Candidate mapping для failure:

- `nextAction=ask_clarification` или existing boundary action;
- без `hotelSearchId`;
- без `show_hotel_results`;
- safe retry/clarification text;
- без raw/internal/provider details.

OpenAPI/frontend changes не требуются для internal execution skeleton. Перед тем как route execution начнет возвращать `show_hotel_results` из confirmation path, route tests должны доказать совместимость current public response shape.

## Stage 7 strict handoff compatibility

Вердикт: совместимо только как explicit-confirmation exception.

Confirmed-search execution станет вторым automatic search creation path. Это допустимо только если tests и docs закрепляют:

- Stage 7 explicit `hotel-search;` path остается неизменным и priority-safe;
- confirmed-search execution происходит только после active pending confirmation;
- execution не запускается из generic natural language;
- execution не запускается из generic “yes” без pending state;
- execution не запускается из ambiguous, negative, correction, unknown или missing pending state;
- command/response не содержат raw LLM candidate, validation details или provider metadata;
- `hotelSearchId` и `show_hotel_results` появляются только после успешного search creation.

Без этих guardrails execution ослабит Stage 7 strict handoff boundary.

## Что не входит в Stage 8.31

- Изменения production code.
- Tests.
- Route wiring.
- Изменения `Application.kt`.
- Изменения `AssistantLlmRouteWiringUseCase`.
- Изменения `PlanPostConfirmationDecisionUseCase`.
- Изменения `PlanConfirmedSearchCreationUseCase`.
- Изменения `BuildConfirmedSearchCreationCommandUseCase`.
- Реализация confirmed-search execution layer.
- Вызов `CreateHotelSearchUseCase`.
- Вызов hotel provider.
- Создание `hotelSearchId`.
- Response `show_hotel_results`.
- Вызов `markConsumed`.
- Изменения Public API/OpenAPI/frontend/generated clients.
- Durable storage, auth или booking flow.
- Изменения roadmap/root status.

## Риски преждевременного route wiring / search execution

- Duplicate search при repeated confirmation.
- Потерянный `hotelSearchId` после успешного search и failed response.
- Pending confirmation consumed до failed search.
- Pending confirmation остается active после success и переиспользуется.
- Session mismatch между pending confirmation и search command.
- Generic “yes” превращается в search execution вне active pending context.
- Stage 7 strict handoff теряет строгий смысл.
- Failure branch раскрывает internal exception/provider details.
- Frontend получает `show_hotel_results` из неотревьюенного confirmation-created path.

## Рекомендуемый Stage 8.32

Safe Stage 8.32: backend-only internal confirmed-search execution result/use case skeleton, без route wiring.

Предпочтительная форма:

- input: `ConfirmedSearchCreationCommandPlan.CommandReady`;
- output: typed result, например:
  - `ReadyForExecution` / `ExecutionSucceeded` candidate с created search reference только если используется fake/no-op executor;
  - `ExecutionFailed` typed reason;
  - `IdempotencyRequired`;
- execution dependency должна быть fake/no-op или injected boundary в tests;
- без direct route wiring;
- без real provider;
- без durable storage;
- без public response changes.

Alternative safe Stage 8.32: review-only idempotency-key/storage gate, если execution skeleton иначе будет подразумевать преждевременное search creation.

## Verdict

Stage 8.31 verdict: direct route execution пока не готов.

Current layers готовы для command construction, но фактическому confirmed-search execution нужен отдельный internal execution result/use case skeleton с typed success/failure/idempotency outcomes до любого route wiring. `CreateHotelSearchUseCase`, provider, `hotelSearchId`, `show_hotel_results` и `markConsumed` должны оставаться вне confirmed-search route path, пока execution policy не будет явно описана и протестирована.
