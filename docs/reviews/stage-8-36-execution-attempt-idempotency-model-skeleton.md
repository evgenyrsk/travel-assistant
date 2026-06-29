# Stage 8.36 — execution attempt/idempotency model skeleton

## Цель

Stage 8.36 добавляет backend-only internal skeleton для будущего
confirmed-search execution attempt/idempotency слоя.

Цель этапа — дать future execution path типизированный способ различать:

- first attempt;
- duplicate in-progress attempt;
- duplicate succeeded attempt;
- duplicate failed attempt;
- duplicate blocked attempt;
- blocked-until-future-policy состояние.

Этап не подключает этот слой к routes/runtime composition и не запускает hotel
search.

## Что добавлено

Добавлены internal application-layer модели:

- `ConfirmedSearchExecutionIdempotencyKey`;
- `ConfirmedSearchExecutionAttempt`;
- `ConfirmedSearchExecutionAttemptStatus`;
- `ConfirmedSearchExecutionAttemptResult`;
- `PlanConfirmedSearchExecutionAttemptUseCase`.

`PlanConfirmedSearchExecutionAttemptUseCase` принимает результат guard layer и
готовит internal attempt result, но не исполняет search и не пишет состояние.

## Production files

Добавлены:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionIdempotencyKey.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionAttempt.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionAttemptStatus.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ConfirmedSearchExecutionAttemptResult.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanConfirmedSearchExecutionAttemptUseCase.kt`.

Не менялись:

- `Application.kt`;
- assistant routes;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- `PlanConfirmedSearchCreationUseCase`;
- `BuildConfirmedSearchCreationCommandUseCase`;
- `PlanConfirmedSearchExecutionUseCase`;
- `PlanConfirmedSearchExecutionGuardUseCase`.

## Tests

Добавлен targeted test:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/PlanConfirmedSearchExecutionAttemptUseCaseTest.kt`.

Проверки покрывают:

- allowed guard result -> prepared-but-blocked attempt;
- deterministic internal idempotency basis;
- duplicate `IN_PROGRESS`, `SUCCEEDED`, `FAILED` outcomes;
- rejected guard result;
- mismatched existing attempt key;
- read-only pending-state boundary;
- отсутствие route/search/provider/`markConsumed` side effects.

## Attempt/idempotency input/output

Input:

- `ConfirmedSearchExecutionGuardResult`;
- `Instant now`;
- optional existing `ConfirmedSearchExecutionAttempt`.

Output:

- `AttemptPreparedButExecutionBlocked`;
- `DuplicateDetected`;
- `Rejected`.

Даже successful first attempt result не означает permission to execute. Он
остается blocked until attempt store / execution policy будет добавлен в
отдельном этапе.

## Attempt statuses

Internal statuses:

- `PREPARED`;
- `IN_PROGRESS`;
- `SUCCEEDED`;
- `FAILED`;
- `DUPLICATE_BLOCKED`.

На Stage 8.36 use case создает только `PREPARED` для first attempt и
`DUPLICATE_BLOCKED` для duplicate attempt. Остальные статусы моделируются как
existing attempt snapshot для будущего execution/storage path.

## Idempotency basis

`ConfirmedSearchExecutionIdempotencyKey` строится детерминированно из:

- internal scope prefix;
- `AssistantSessionId`;
- `HotelSearchCriteria.destination`;
- check-in/check-out dates;
- adults/children;
- rooms.

Key хранится как SHA-256 hex с internal prefix. Это не public id, не
confirmation id и не hotel search id. Raw session/destination не раскрываются в
значении key.

## Почему attempt store еще не добавлен

Stage 8.36 намеренно не добавляет attempt store.

Причины:

- текущий этап не исполняет search;
- без route wiring нет runtime consumer для store;
- store design должен отдельно решить lifecycle, duplicate success replay,
  failure retry и lost-response behavior;
- durable storage не входит в Stage 8.36.

Без store use case может только подготовить typed attempt или распознать
переданный existing attempt snapshot.

## No-route-wiring boundary

Attempt skeleton не подключен к:

- assistant routes;
- `AssistantLlmRouteWiringUseCase`;
- `PlanPostConfirmationDecisionUseCase`;
- `PlanConfirmedSearchCreationUseCase`;
- `BuildConfirmedSearchCreationCommandUseCase`;
- `PlanConfirmedSearchExecutionUseCase`;
- `PlanConfirmedSearchExecutionGuardUseCase`.

Backend runtime behavior не меняется.

## No-search-execution boundary

Stage 8.36 не:

- создает search;
- создает actual `hotelSearchId`;
- возвращает `show_hotel_results`;
- вызывает `CreateHotelSearchUseCase`;
- вызывает provider;
- вызывает `markConsumed`;
- пишет pending state;
- меняет assistant session.

`createdSearchId` в `ConfirmedSearchExecutionAttempt` — optional future-modeled
поле для duplicate succeeded branch, а не actual runtime creation.

## Raw/internal leakage boundary

Attempt skeleton не принимает и не хранит:

- raw `LlmCandidate`;
- provider metadata;
- model metadata;
- validation issue payload;
- raw candidate payload.

Input уже typed и прошел предыдущие internal boundaries.

## Public API / OpenAPI / frontend / generated clients

Verdict: unchanged.

Stage 8.36 не меняет:

- public API request/response shape;
- OpenAPI contracts;
- frontend;
- generated clients;
- CI/build/package files.

## Stage 7 strict handoff compatibility

Stage 7 strict `hotel-search;` handoff остается единственным current automatic
search creation path.

Stage 8.36 только моделирует будущие confirmed-search attempts и не создает
второй runtime search path.

## Durable storage / provider / network / API keys

Verdict: not added.

Stage 8.36 не добавляет:

- durable storage;
- database/cache/filesystem persistence;
- real provider integration;
- network calls;
- API keys, secrets или environment variables.

## Риски и ограничения

- Idempotency key пока не backed by store, поэтому duplicate prevention не
  работает как runtime гарантия.
- `SUCCEEDED`/`FAILED`/`IN_PROGRESS` branches моделируются только через
  existing attempt snapshot.
- Нет durable behavior для restart/multi-instance scenarios.
- Actual execution все еще требует отдельный policy/store/executor step.

## Рекомендуемый Stage 8.37

Рекомендуемый следующий шаг: Stage 8.37 как review/design-only attempt store
readiness gate.

Цель Stage 8.37:

- решить, нужен ли process-local attempt store skeleton перед actual execution;
- определить states и transitions для prepared/in-progress/succeeded/failed;
- определить duplicate success replay policy;
- определить failure/lost-response policy;
- подтвердить, что route wiring и `CreateHotelSearchUseCase` call все еще не
  должны добавляться без explicit execution stage.

## Verdict

Stage 8.36 выполнен как backend-only internal attempt/idempotency model skeleton.

Attempt planning теперь typed и session-bound, но остается blocked до будущего
attempt store / execution policy этапа. Route wiring, actual search execution,
`hotelSearchId`, `show_hotel_results`, provider calls, `markConsumed`, durable
storage, OpenAPI/frontend/generated-client changes не добавлены.
