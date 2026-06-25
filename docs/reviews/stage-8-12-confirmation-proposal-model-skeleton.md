# Stage 8.12 — Confirmation proposal model skeleton

## Цель Stage 8.12

Добавить backend-only internal model и builder, которые на основе `ProceedWithCandidateValidationResult.Accepted` готовят human-readable confirmation proposal для будущего confirmation step.

Stage 8.12 не подключает proposal builder к routes, не меняет runtime behavior и не создает hotel search.

## Что было добавлено

Добавлены internal application-layer типы:

- `ProceedWithCandidateConfirmationProposal`;
- `ProceedWithCandidateConfirmationField`;
- `BuildProceedWithCandidateConfirmationProposalUseCase`.

Builder принимает только `ProceedWithCandidateValidationResult.Accepted`, строит deterministic summary/question и возвращает internal proposal. Rejected validation result не может породить proposal через этот builder на уровне типа.

## Production files

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateConfirmationProposal.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/ProceedWithCandidateConfirmationField.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/BuildProceedWithCandidateConfirmationProposalUseCase.kt`.

Существующие production files не изменялись.

## Tests

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/BuildProceedWithCandidateConfirmationProposalUseCaseTest.kt`.

Тесты проверяют:

- happy path proposal from accepted criteria;
- deterministic summary и question;
- включение destination, dates, guests и rooms;
- безопасное отображение `children=0`;
- пропуск blank destination;
- отсутствие raw/internal metadata и `hotelSearchId`;
- отсутствие provider, external call или credential dependency.

## Proposal model fields

`ProceedWithCandidateConfirmationProposal` содержит:

- `summary` — human-readable summary для будущего confirmation message;
- `confirmationQuestion` — короткий вопрос подтверждения;
- `displayFields` — список safe display fields, построенных из `ProceedWithCandidateCriteria`.

`ProceedWithCandidateConfirmationField` содержит:

- `key` — internal stable key для allowed criteria field;
- `label` — человекочитаемый label;
- `value` — безопасное display value.

Эти типы не являются public DTO и не должны напрямую становиться OpenAPI shape.

## Builder input/output

Input:

- `ProceedWithCandidateValidationResult.Accepted`.

Output:

- `ProceedWithCandidateConfirmationProposal`.

Builder не принимает `Rejected`, не вызывает criteria validator сам и не принимает raw LLM candidate.

## Omission/safety rules

Реализованные правила:

- blank destination не включается в `displayFields` и `summary`;
- dates, adults, children и rooms берутся только из typed `ProceedWithCandidateCriteria`;
- `children=0` отображается как безопасное явное значение;
- raw candidate payload не используется;
- provider/model/source metadata не используется;
- warnings, conflicts, validation issues и internal reasons не включаются;
- `hotelSearchId` не создается и не включается;
- builder не создает hotel search request.

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
- Raw candidate не раскрывается через proposal.
- Bounded hotel-only MVP не расширен.

## Риски и ограничения

- Proposal пока не подключен к criteria validator composition.
- Proposal пока не подключен к routes и не виден пользователю.
- Summary text является internal skeleton wording, а не финальным UX copy.
- `displayFields` не являются public contract и не должны использоваться как OpenAPI shape без отдельного contract step.
- Search creation остается отложенным до отдельного confirmation/runtime readiness step.

## Рекомендуемый Stage 8.13

Stage 8.13 — backend-only internal confirmation planning composition без route wiring.

Минимальная цель:

- принять `AssistantCandidateDecision.ProceedWithCandidate`;
- применить `ProceedWithCandidateCriteriaValidator`;
- для accepted result построить `ProceedWithCandidateConfirmationProposal`;
- для rejected result вернуть typed clarification/fallback planning outcome;
- не создавать search;
- не менять public API, OpenAPI, frontend или runtime routes.

Route wiring для confirmation prompt и тем более search creation должны оставаться отдельными future steps.

## Verdict

Stage 8.12 выполнен в proposal-model-only границах. Internal confirmation proposal skeleton добавлен и покрыт targeted unit tests. Routes, runtime behavior, public API, OpenAPI, frontend, real provider work, search creation и Stage 7 strict handoff не менялись.
