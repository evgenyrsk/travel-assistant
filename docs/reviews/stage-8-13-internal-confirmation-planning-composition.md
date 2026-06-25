# Stage 8.13 — Internal confirmation planning composition

## Цель Stage 8.13

Добавить backend-only internal use case, который связывает:

```text
AssistantCandidateDecision.ProceedWithCandidate
  -> ProceedWithCandidateCriteriaValidator
  -> BuildProceedWithCandidateConfirmationProposalUseCase
  -> ProceedWithCandidateConfirmationPlan
```

Stage 8.13 не подключает planning use case к routes, не меняет runtime behavior и не создает hotel search.

## Что было добавлено

Добавлены internal application-layer типы:

- `ProceedWithCandidateConfirmationPlan`;
- `PlanProceedWithCandidateConfirmationUseCase`.

Use case принимает только `AssistantCandidateDecision.ProceedWithCandidate`, применяет existing criteria validator и строит confirmation proposal только для accepted validation result.

## Production files

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateConfirmationPlan.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanProceedWithCandidateConfirmationUseCase.kt`.

Существующие production files не изменялись.

## Tests

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/PlanProceedWithCandidateConfirmationUseCaseTest.kt`.

Тесты проверяют:

- valid complete candidate -> confirmation proposal plan;
- missing destination -> clarification plan без proposal;
- invalid dates -> clarification plan без proposal;
- candidate clarification hint -> safe clarification plan;
- non-hotel intent -> fallback plan;
- conflicts/warnings -> fallback plan;
- отсутствие raw/internal validation details в user-facing text;
- отсутствие hotel search creation и `hotelSearchId`;
- deterministic result;
- отсутствие provider, external call или credential dependency.

## Planning flow

Flow остается полностью internal:

1. `PlanProceedWithCandidateConfirmationUseCase` принимает `AssistantCandidateDecision.ProceedWithCandidate`.
2. `ProceedWithCandidateCriteriaValidator` проверяет candidate.
3. Если validation accepted, `BuildProceedWithCandidateConfirmationProposalUseCase` строит `ProceedWithCandidateConfirmationProposal`.
4. Если validation rejected, use case возвращает clarification/fallback plan.
5. Use case не создает search request, не вызывает hotel provider и не формирует public response.

## Planning result outcomes

`ProceedWithCandidateConfirmationPlan` содержит:

| Outcome | Назначение |
|---|---|
| `ConfirmationRequired(proposal)` | Complete safe criteria готовы только к будущему confirmation prompt. |
| `ClarificationRequired(question, reason)` | Candidate неполный или исправимый; нужен безопасный уточняющий вопрос. |
| `Fallback(reason)` | Candidate unsafe, unsupported или conflicting; route-level message не формируется на этом этапе. |

Эти outcomes являются internal application model, а не public DTO.

## Rejected validation handling

Rejected validation result обрабатывается так:

- missing/invalid criteria -> `ClarificationRequired`;
- existing safe clarification hint -> `ClarificationRequired`;
- unsupported intent -> `Fallback`;
- conflicts или blocking warnings -> `Fallback`;
- unsupported outcome без safe clarification hint -> `Fallback`.

Typed validation issues не превращаются напрямую в user-facing message.

## Raw/internal details leakage boundary

Use case не раскрывает:

- raw candidate payload;
- extracted constraints map;
- validation issue enum names;
- conflicts/warnings/internal reasons as public text;
- provider/model/source metadata;
- `hotelSearchId`.

Единственные text values внутри planning result:

- confirmation proposal summary/question, построенные из accepted typed criteria;
- safe clarification question.

## Scope confirmations

- Route wiring не менялся.
- Runtime behavior не менялся.
- `Application.kt` не менялся.
- `AssistantLlmRouteWiringUseCase` не менялся.
- Hotel search не создается.
- `hotelSearchId` не создается.
- Stage 7 strict `hotel-search;` handoff сохранен.
- Public API shape, OpenAPI, frontend и generated clients не менялись.
- Внешний LLM-провайдер, внешние вызовы и ключи доступа не добавлены.
- Bounded hotel-only MVP не расширен.

## Риски и ограничения

- Confirmation planning use case пока не подключен к runtime composition.
- Clarification/fallback plan пока не имеет public route mapping.
- Clarification question является safe generic text или existing safe hint; финальный UX copy остается будущим шагом.
- Search creation остается отложенным до отдельного confirmation route wiring и user-confirmation handling step.
- `ProceedWithCandidateConfirmationPlan` не должен становиться OpenAPI shape без отдельной contract review.

## Рекомендуемый Stage 8.14

Stage 8.14 — review-only route wiring readiness gate для confirmation prompt.

Минимальная цель:

- проверить, можно ли отразить `ConfirmationRequired` через existing `nextAction=ask_clarification` и `assistantMessage.content`;
- подтвердить, что public contract не требует нового action/value/field;
- определить route tests до любого wiring;
- оставить search creation и `hotelSearchId` creation отложенными до отдельного post-confirmation step.

Immediate search creation из `ProceedWithCandidate` не рекомендуется.

## Verdict

Stage 8.13 выполнен в composition-only границах. Internal confirmation planning use case добавлен и покрыт targeted unit tests. Routes, runtime behavior, public API, OpenAPI, frontend, real provider work, search creation и Stage 7 strict handoff не менялись.
