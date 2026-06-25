# Stage 8.14 — Confirmation prompt route wiring readiness gate

## Цель Stage 8.14

Проверить, можно ли безопасно отразить internal `ConfirmationRequired` через существующий public response shape:

- `nextAction=ask_clarification`;
- `assistantMessage.content`;
- без новых public fields;
- без `hotelSearchId`;
- без создания hotel search.

Stage 8.14 является review-only шагом. Он не меняет production code, tests, runtime behavior, routes, public API, OpenAPI или frontend.

## Текущая точка входа

- Stage 8.8 подключил internal LLM path к runtime только для `AskClarification` и `Fallback`.
- `ProceedWithCandidate` в runtime по-прежнему возвращает safe boundary message и не создает search.
- Stage 8.13 добавил internal `PlanProceedWithCandidateConfirmationUseCase`.
- `PlanProceedWithCandidateConfirmationUseCase` не подключен к routes или runtime composition.
- Stage 7 strict `hotel-search;` handoff остается единственным текущим automatic search creation path.

## Что уже есть после Stage 8.13

Internal planning path:

```text
AssistantCandidateDecision.ProceedWithCandidate
  -> PlanProceedWithCandidateConfirmationUseCase
  -> ProceedWithCandidateConfirmationPlan
```

Planning outcomes:

| Outcome | Текущее состояние |
|---|---|
| `ConfirmationRequired` | Есть internal proposal summary/question, но route mapping отсутствует. |
| `ClarificationRequired` | Есть safe question/reason, но route mapping отсутствует. |
| `Fallback` | Есть internal fallback reason, но route mapping отсутствует. |

## Readiness assessment: ConfirmationRequired

Verdict: conditionally ready for minimal route wiring.

Условия:

- map to `nextAction=ask_clarification`;
- put only safe text into `assistantMessage.content`;
- do not add public fields;
- do not return `hotelSearchId`;
- do not create hotel search;
- do not expose `displayFields` as public structured payload;
- keep explicit `hotel-search;` handoff priority unchanged.

Текущий response shape уже поддерживает text-only confirmation prompt. Новое public action value не требуется.

## Readiness assessment: ClarificationRequired

Verdict: ready only for safe text mapping.

Допустимый mapping:

- `nextAction=ask_clarification`;
- `assistantMessage.content` contains a safe clarification question;
- no `hotelSearchId`;
- no raw issues or internal reasons.

Если question пришел из candidate clarification hint, он должен оставаться bounded hotel-only и не должен раскрывать internal extraction details.

## Readiness assessment: Fallback

Verdict: ready for existing safe fallback outcome.

Допустимый mapping:

- `nextAction=show_boundary_message`;
- existing safe boundary message;
- no `hotelSearchId`;
- no raw fallback reason;
- no typed issue names in public response.

Fallback не должен становиться способом объяснять internal validation failures пользователю.

## Safe message content rules

Для `ConfirmationRequired` в `assistantMessage.content` можно включать только:

- destination;
- check-in / check-out dates;
- adults;
- children, если значение явно безопасно;
- rooms;
- короткий human-readable confirmation question.

Запрещено включать:

- raw LLM candidate;
- validation issue enum names;
- internal warnings/conflicts;
- provider/model/source metadata;
- confidence или safety markers;
- internal `displayFields` structure as JSON-like payload;
- sensitive free-form text beyond approved criteria fields.

Message должен быть text-only и не должен становиться public criteria DTO.

## Public contract / OpenAPI / frontend assessment

Текущий public response shape:

- `session`;
- `assistantMessage`;
- `nextAction`;
- optional `hotelSearchId`.

Assessment:

- `nextAction=ask_clarification` уже существует.
- `assistantMessage.content` уже является public text channel.
- `hotelSearchId` can remain absent.
- Новые public fields не нужны.
- Новое OpenAPI action value не нужно.
- Frontend не должен получать новые поля.

Ограничение: frontend не сможет машинно отличить ordinary clarification от confirmation prompt. Это допустимо для minimal Stage 8.15 только если цель — text-only confirmation prompt, а не structured confirmation UX.

## Stage 7 strict handoff compatibility

Совместимо при условиях:

- `hotel-search;` остается единственным automatic search creation trigger;
- confirmation prompt не создает search;
- `ProceedWithCandidate` не превращается в `show_hotel_results`;
- `hotelSearchId` не появляется из LLM candidate path;
- explicit handoff path остается приоритетным перед LLM remapping.

## Что не входит в Stage 8.14

- production code;
- tests;
- route wiring;
- изменение `Application.kt`;
- изменение `AssistantLlmRouteWiringUseCase`;
- изменение runtime behavior;
- создание hotel search или `hotelSearchId`;
- изменение public request/response shape;
- OpenAPI, frontend, generated clients или CI gate;
- внешний LLM-сервис, внешние вызовы, ключи доступа или provider-specific настройки;
- durable storage, auth, booking flow или расширение hotel-only MVP.

## Риски преждевременного wiring

- Confirmation prompt может быть ошибочно воспринят как ordinary clarification.
- Frontend не имеет structured confirmation state.
- Raw/internal planning details могут случайно попасть в `assistantMessage.content`.
- `ProceedWithCandidate` может преждевременно начать создавать search.
- Strict `hotel-search;` handoff может быть обойден LLM path.
- Public contract может незаметно расшириться через новые fields или action values.

## Рекомендуемый Stage 8.15

Stage 8.15 — minimal backend-only route wiring for confirmation prompt, without search creation.

Минимальная цель:

- wire `PlanProceedWithCandidateConfirmationUseCase` only inside existing `ProceedWithCandidate` branch;
- map `ConfirmationRequired` to `nextAction=ask_clarification` and text-only `assistantMessage.content`;
- map `ClarificationRequired` to `nextAction=ask_clarification`;
- map `Fallback` to existing safe boundary message;
- keep `hotelSearchId` absent;
- preserve explicit `hotel-search;` handoff priority;
- add route tests for response shape, no raw fields, no search creation and no OpenAPI/frontend changes.

Search creation after user confirmation must remain a separate future step.

## Verdict

Passed with constraints.

`ConfirmationRequired` can be safely represented through existing `ask_clarification` + `assistantMessage.content` only as a text-only prompt. `ClarificationRequired` can use the same public shape when the question is safe. `Fallback` should continue using the existing safe boundary outcome. Stage 8.14 does not permit hotel search creation, new public fields, new OpenAPI values, frontend changes or production-readiness claims.
