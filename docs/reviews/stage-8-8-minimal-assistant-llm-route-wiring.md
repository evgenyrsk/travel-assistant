# Stage 8.8 — Minimal assistant LLM route wiring

## Цель

Подключить internal LLM pipeline к assistant runtime только для безопасных public outcomes:

- `AskClarification`;
- `Fallback`.

Stage 8.8 не подключает внешний LLM-сервис, не меняет public response shape и не превращает `ProceedWithCandidate` в hotel search.

## Что было изменено

- Добавлен `AssistantLlmRouteWiringUseCase` как тонкая application-level обертка над существующим `AssistantSessionBoundary`.
- Runtime composition теперь использует deterministic `FakeLlmClient` для narrow LLM path.
- Explicit `hotel-search;` handoff остается приоритетным: если сообщение распознано как explicit hotel search request, результат существующего handoff возвращается без LLM remapping.
- Для обычных сообщений internal decision переводится только в уже существующие public outcomes.

## Production files

- `services/backend/src/main/kotlin/com/travelassistant/backend/Application.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantLlmRouteWiringUseCase.kt`

## Tests

Изменен:

- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`

Покрытие добавлено для:

- clarification path с сохранением существующей response shape;
- fallback path без раскрытия internal reason;
- `ProceedWithCandidate` без создания hotel search;
- сохранения explicit `hotel-search;` handoff;
- отсутствия internal candidate fields в public response.

## Mapping в public route outcomes

| Internal decision | Public outcome |
|---|---|
| `AskClarification` | `assistantMessage.content` получает clarification question, `nextAction=ask_clarification`, `hotelSearchId` отсутствует. |
| `Fallback` | Возвращается safe boundary message, `nextAction=show_boundary_message`, internal reason не раскрывается, `hotelSearchId` отсутствует. |
| `ProceedWithCandidate` | Возвращается safe boundary message, `nextAction=show_boundary_message`, hotel search не создается, internal candidate не раскрывается. |

## Public contract

Public response shape не изменен. Route по-прежнему возвращает существующие поля:

- `session`;
- `assistantMessage`;
- `nextAction`;
- optional `hotelSearchId`.

Новые public fields не добавлены.

## Scope confirmations

- OpenAPI, frontend и generated clients не менялись.
- Внешний LLM-сервис, внешние вызовы и ключи доступа не добавлены.
- `ProceedWithCandidate` не запускает hotel search и не создает `hotelSearchId`.
- Stage 7 strict `hotel-search;` handoff сохранен как единственный путь к automatic hotel search creation.
- Bounded hotel-only MVP не расширен: flights, booking flow, durable storage и auth не добавлены.

## Риски и ограничения

- Safe LLM route wiring работает только с deterministic fake path и не является готовностью к внешнему провайдеру.
- `ProceedWithCandidate` пока намеренно не используется для hotel search: перед этим нужен отдельный criteria-validation и contract/runtime step.
- Fallback message пока общий и не раскрывает internal details; это безопаснее, но менее информативно для пользователя.
- Runtime теперь включает fake LLM path для обычных assistant messages, поэтому дальнейшие изменения должны особенно беречь Stage 7 explicit handoff.

## Рекомендуемый Stage 8.9

Stage 8.9 — отдельный criteria-validation и contract review для возможного будущего `ProceedWithCandidate` handoff.

Минимальный безопасный следующий шаг: определить, какие validated constraints могут стать hotel search request без изменения public contract, и какие route tests должны защитить существующий `hotel-search;` guardrail.

## Verdict

Stage 8.8 выполнен в минимальных backend-only границах. Safe outcomes подключены к assistant route через deterministic fake path, public API shape сохранен, explicit hotel handoff не сломан, `ProceedWithCandidate` не запускает search.
