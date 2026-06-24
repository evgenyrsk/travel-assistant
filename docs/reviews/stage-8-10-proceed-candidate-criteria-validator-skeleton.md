# Stage 8.10 — Proceed candidate criteria validator skeleton

## Цель Stage 8.10

Добавить backend-only internal validator для `ProceedWithCandidate`, который проверяет, достаточно ли candidate полон и безопасен для будущего hotel-search handoff.

Stage 8.10 не подключает validator к routes, не меняет runtime behavior и не создает hotel search.

## Что было добавлено

Добавлены internal application-layer типы:

- `ProceedWithCandidateCriteriaValidator`;
- `ProceedWithCandidateCriteria`;
- `ProceedWithCandidateValidationResult`;
- `ProceedWithCandidateValidationIssue`.

Validator принимает `AssistantCandidateDecision.ProceedWithCandidate`, читает только internal `LlmCandidate` и возвращает typed accepted/rejected result. Он не знает про Ktor routes, OpenAPI, frontend, provider boundary или runtime composition.

## Production files

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateCriteria.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateCriteriaValidator.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateValidationIssue.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateValidationResult.kt`.

Существующие production files не изменялись.

## Tests

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateCriteriaValidatorTest.kt`.

Тесты проверяют:

- complete safe candidate accepted;
- missing destination rejected;
- invalid date range rejected;
- adults < 1 rejected;
- children < 0 rejected;
- missing или invalid rooms rejected;
- missing required fields rejected;
- conflicts rejected;
- clarification question rejected with clarification hint;
- warnings treated as blocking;
- unsupported/non-hotel intent rejected;
- non-interpreted outcome rejected;
- partial current candidate rejected when required fields are absent;
- deterministic result;
- no provider, external call или credential dependency.

## Validation rules

Accepted result возможен только если:

- intent — `HOTEL_SEARCH`;
- outcome — `INTERPRETED`;
- destination присутствует и не пустой;
- `check-in` и `check-out` parseable as ISO dates;
- `check-out` позже `check-in`;
- adults >= 1;
- children >= 0;
- rooms присутствует явно и >= 1;
- `missingRequiredFields` пуст;
- `conflicts` пуст;
- clarification question отсутствует;
- warnings отсутствуют.

Если текущая candidate model не содержит нужные fields, validator возвращает rejected result, а не делает assumptions.

## Accepted/rejected result model

`ProceedWithCandidateValidationResult.Accepted` содержит internal `ProceedWithCandidateCriteria`. Это не public DTO и не search request.

`ProceedWithCandidateValidationResult.Rejected` содержит:

- typed `ProceedWithCandidateValidationIssue`;
- optional `clarificationHint`, если candidate уже содержит безопасный clarification question.

## Scope confirmations

- Route wiring не менялся.
- Runtime behavior не менялся.
- `AssistantLlmRouteWiringUseCase` не менялся.
- Hotel search не создается.
- Public API shape, OpenAPI, frontend и generated clients не менялись.
- Внешний LLM-провайдер, внешние вызовы и ключи доступа не добавлены.
- Stage 7 strict `hotel-search;` handoff сохранен.
- Bounded hotel-only MVP не расширен.

## Риски и ограничения

- Validator использует текущую string-map candidate model; formal typed LLM extraction contract еще не выделен.
- Date parsing пока принимает только ISO date values.
- Warnings считаются blocking, потому что severity model еще не определена.
- `ProceedWithCandidateCriteria` является internal bridge, а не разрешением на route wiring.
- Search creation остается отложенным до отдельного contract/runtime readiness step.

## Рекомендуемый Stage 8.11

Stage 8.11 должен остаться без automatic search creation.

Безопасный следующий шаг: review/design или backend-only skeleton для explicit confirmation boundary между accepted criteria и future handoff. Этот шаг должен проверить, как пользователь подтверждает extracted criteria до создания search и как сохранить текущий public contract.

## Verdict

Stage 8.10 выполнен в validator-only границах. Internal criteria validation skeleton добавлен и покрыт targeted unit tests. Routes, runtime behavior, public API, OpenAPI, frontend, real provider work и Stage 7 strict handoff не менялись.
