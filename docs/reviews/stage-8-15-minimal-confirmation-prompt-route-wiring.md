# Stage 8.15 — Minimal confirmation prompt route wiring

## Цель Stage 8.15

Подключить existing internal confirmation planning к assistant runtime только для text-only confirmation prompt:

- `ConfirmationRequired` отображается через `nextAction=ask_clarification`;
- текст попадает в `assistantMessage.content`;
- `hotelSearchId` не создается;
- hotel search не запускается.

Stage 8.15 не меняет public response shape, OpenAPI, frontend или generated clients.

## Что было изменено

- `AssistantLlmRouteWiringUseCase` теперь обрабатывает `AssistantCandidateDecision.ProceedWithCandidate` через `PlanProceedWithCandidateConfirmationUseCase`.
- `ConfirmationRequired` превращается в existing clarification response.
- `ClarificationRequired` превращается в existing clarification response.
- `Fallback` остается existing safe boundary response.
- Explicit `hotel-search;` handoff остается приоритетным и не проходит через LLM confirmation mapping.

## Production files

Изменен:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantLlmRouteWiringUseCase.kt`.

`Application.kt` не менялся: use case подключен через existing default dependency внутри application-layer wiring use case.

## Tests

Изменен:

- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`.

Покрытие добавлено или обновлено для:

- complete `ProceedWithCandidate` -> confirmation prompt;
- partial `ProceedWithCandidate` -> clarification prompt;
- unsafe `ProceedWithCandidate` -> safe fallback;
- отсутствия `hotelSearchId` в confirmation/clarification/fallback LLM path;
- отсутствия raw/internal fields в public response;
- сохранения explicit `hotel-search;` handoff как search creation path.

Stage 8.8 expectation, где `ProceedWithCandidate` всегда возвращал boundary message, обновлен в рамках нового Stage 8.15 поведения: complete safe candidate теперь дает confirmation prompt, но не search.

## Mapping из confirmation planning outcomes в public route outcomes

| Internal plan | Public outcome |
|---|---|
| `ConfirmationRequired` | `assistantMessage.content = proposal.summary + confirmationQuestion`, `nextAction=ask_clarification`, без `hotelSearchId`. |
| `ClarificationRequired` | `assistantMessage.content = question`, `nextAction=ask_clarification`, без `hotelSearchId`. |
| `Fallback` | Existing safe boundary message, `nextAction=show_boundary_message`, без `hotelSearchId`. |

`displayFields` и typed validation details не становятся public response fields.

## Safe confirmation prompt content rules

Confirmation prompt может содержать только:

- destination;
- check-in / check-out dates;
- adults;
- children;
- rooms;
- короткий human-readable confirmation question.

Prompt не должен раскрывать:

- raw `LlmCandidate`;
- validation issues;
- internal warnings или conflicts;
- provider metadata;
- model metadata;
- confidence;
- safety marker;
- internal `displayFields` as structured payload.

## Scope confirmations

- Public API shape не изменен.
- Новые public fields не добавлены.
- Новые `nextAction` values не добавлены.
- OpenAPI, frontend и generated clients не менялись.
- Внешний LLM-провайдер, network calls и API keys не добавлены.
- Hotel search из confirmation prompt не создается.
- `hotelSearchId` из confirmation prompt не создается.
- `show_hotel_results` не используется для confirmation prompt.
- Stage 7 strict `hotel-search;` handoff сохранен как единственный automatic search creation trigger.
- Bounded hotel-only MVP не расширен.

## Риски и ограничения

- Frontend пока не отличает ordinary clarification от confirmation prompt как structured state.
- Confirmation prompt является text-only и не является отдельным public contract.
- User confirmation handling после prompt не реализован.
- Search creation после confirmation остается будущим отдельным step.
- Runtime все еще использует deterministic fake LLM path, а не внешний provider.

## Рекомендуемый Stage 8.16

Stage 8.16 — review/design-only или backend-only internal step для post-confirmation handling boundary.

Безопасная следующая цель:

- определить, как пользовательское подтверждение будет распознаваться без изменения public contract;
- подтвердить, что search creation после confirmation не обойдет Stage 7 strict handoff;
- не создавать hotel search до отдельного criteria-confirmation runtime gate.

Immediate `ProceedWithCandidate -> hotel search` по-прежнему не рекомендуется.

## Verdict

Stage 8.15 выполнен в minimal backend-only границах. Confirmation prompt route wiring подключен через existing response shape, public contract не изменен, hotel search и `hotelSearchId` не создаются, raw/internal details не раскрываются, Stage 7 strict `hotel-search;` handoff сохранен.
