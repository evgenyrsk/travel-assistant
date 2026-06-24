# Stage 8.7 — Assistant LLM route wiring readiness gate

## 1. Цель Stage 8.7

Stage 8.7 проверяет, можно ли безопасно подключать internal LLM pipeline к Assistant routes без изменения публичного контракта.

Это review-only gate. Он не добавляет production code, тесты, route wiring, изменения runtime behavior, OpenAPI или frontend.

## 2. Текущая точка входа

- Stage 8.0-8.1 определили границы AI/LLM orchestration и `LlmClient`.
- Stage 8.2-8.5 добавили internal backend pipeline до `AssistantCandidateDecision`.
- Stage 8.6 определил design-level mapping из `AssistantCandidateDecision` в future assistant actions.
- `Application.kt` не создает `PlanAssistantLlmDecisionUseCase`.
- `AssistantPlaceholderRoutes` не знает о `LlmClient`, `GenerateLlmCandidateUseCase` или `AssistantCandidateDecision`.
- Stage 7 strict `hotel-search;` handoff остается единственным путем, который создает hotel search из Assistant message.

## 3. Что уже есть после Stage 8.6

Internal LLM pipeline:

```text
LlmCandidateRequest
  -> GenerateLlmCandidateUseCase
  -> PlanAssistantCandidateDecisionUseCase
  -> AssistantCandidateDecision
```

Decision outcomes:

- `AskClarification` — один безопасный уточняющий вопрос;
- `Fallback` — safe outcome для empty, invalid, failure-like, unsupported или неполного результата;
- `ProceedWithCandidate` — валидный candidate для future review, но не команда search.

## 4. Наблюдения по текущему Assistant route / public contract

Текущие Assistant routes:

- `POST /api/v1/assistant/sessions`;
- `POST /api/v1/assistant/sessions/{sessionId}/messages`.

Текущий request shape:

- `message`;
- optional `clientContext.locale`;
- optional `clientContext.timezone`.

Текущий runtime response shape:

- `session.sessionId`;
- `session.status`;
- `session.createdAt`;
- `session.updatedAt`;
- `assistantMessage.role`;
- `assistantMessage.content`;
- `nextAction`;
- optional `hotelSearchId`.

Текущие runtime `nextAction` values:

- `ask_clarification`;
- `show_hotel_results`;
- `show_boundary_message`.

OpenAPI draft уже содержит `nextAction` как required field и не требует раскрывать internal candidate fields. Runtime tests также проверяют, что response не содержит internal requirements state, slot coverage, `clientContext`, `hotelSearchRequest` или `searchIntentSummary`.

## 5. Readiness assessment — AskClarification

Вердикт: условно готово для первого узкого route wiring.

Почему это может быть безопасно:

- `ask_clarification` уже существует в runtime enum и OpenAPI enum;
- `assistantMessage.content` уже является публичным местом для текста уточнения;
- `hotelSearchId` не нужен;
- raw `LlmCandidate` не нужно раскрывать наружу.

Условия перед implementation:

- route tests должны зафиксировать, что response shape не меняется;
- ordinary non-LLM fallback должен оставаться безопасным при empty/failure/invalid result;
- Stage 7 strict explicit handoff не должен быть заменен;
- LLM clarification question должен проходить через internal validation и не должен менять session/search state сверх текущего message intake.

## 6. Readiness assessment — Fallback

Вердикт: условно готово для первого узкого route wiring после одного mapping decision.

Почему это может быть безопасно:

- `show_boundary_message` уже существует как runtime `nextAction`;
- fallback text можно вернуть через existing `assistantMessage.content`;
- public response не обязан раскрывать internal fallback reason;
- provider boundary и hotel search не нужны.

Что нужно решить до implementation:

- единообразно выбрать public `nextAction` для fallback: `show_boundary_message` или `ask_clarification`;
- оставить `FallbackReason` внутренним;
- добавить route tests для empty, invalid, failure-like и unsupported cases;
- убедиться, что fallback не выглядит как provider fact, search result или confirmed user constraint.

## 7. Readiness assessment — ProceedWithCandidate

Вердикт: не готово для route wiring.

Причины:

- `ProceedWithCandidate` содержит internal `LlmCandidate`, а не `HotelSearchCriteria`;
- нет отдельной route-level проверки required fields, assumptions, unknowns и date/guest/domain constraints;
- автоматический `show_hotel_results` потребовал бы создать hotel search и `hotelSearchId`;
- `ready_for_hotel_search` присутствует в OpenAPI enum, но не реализован в текущем runtime enum;
- публикация raw candidate расширила бы public API;
- автоматический search мог бы подменить Stage 7 strict explicit handoff.

`ProceedWithCandidate` должен оставаться на internal review boundary до отдельного contract/runtime шага.

## 8. Условия для будущего route wiring

Перед любым implementation step нужны:

- route contract review по текущей response shape;
- targeted tests для existing Assistant endpoint behavior;
- тесты, что `AskClarification` и `Fallback` не добавляют новые public fields;
- тесты, что strict `hotel-search;` handoff продолжает работать как раньше;
- явное подтверждение, что OpenAPI и frontend не меняются;
- deterministic fake path без внешних вызовов и без ключей доступа;
- запрет на `ProceedWithCandidate -> hotel search` до отдельного criteria-validation step.

## 9. Что не входит в Stage 8.7

- production code;
- backend tests;
- route wiring;
- runtime composition;
- изменение Assistant endpoint behavior;
- изменение hotel search handoff;
- изменение public request/response shape;
- OpenAPI, generated clients, frontend или CI gate;
- real LLM provider, внешние вызовы или ключи доступа;
- durable storage, auth, booking flow или расширение hotel-only MVP.

## 10. Риски преждевременного wiring

- Незаметное изменение public Assistant behavior.
- Расхождение runtime response с OpenAPI.
- Смешивание deterministic existing handoff и fake LLM path.
- Появление неявного LLM runtime path для всех обычных сообщений.
- Изменение frontend assumptions вокруг `nextAction`.
- Раскрытие internal candidate/reason fields через public response.
- Создание hotel search из непроверенного LLM candidate.
- Расширение за пределы bounded hotel-only MVP.

## 11. Рекомендуемый Stage 8.8

`Stage 8.8 — Minimal Assistant LLM route wiring for clarification and fallback`.

Допустимый следующий implementation step должен быть минимальным:

- backend-only;
- только `AskClarification` и `Fallback`;
- только deterministic fake LLM;
- без `ProceedWithCandidate` search creation;
- без OpenAPI/frontend/generated-client changes;
- targeted route tests на existing response shape;
- сохранение Stage 7 strict explicit handoff.

Если во время Stage 8.8 окажется, что route-level mapping требует нового public action или новых fields, задачу нужно остановить и заменить на internal action model skeleton без route wiring.

## 12. Verdict

Passed with constraints — public contract уже позволяет узкий future wiring для `AskClarification` и `Fallback` без изменения response shape.

`ProceedWithCandidate` не готов к route wiring и должен быть отложен. Stage 8.7 не меняет runtime behavior, routes, public API, OpenAPI, frontend, provider boundary или hotel-only MVP scope.
