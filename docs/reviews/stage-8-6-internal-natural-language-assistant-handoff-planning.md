# Stage 8.6 — Планирование internal natural-language assistant handoff

## 1. Цель Stage 8.6

Stage 8.6 определяет, как текущий internal `AssistantCandidateDecision` может быть сопоставлен с будущими assistant actions для natural-language handoff.

Это review/design-only шаг. Он не добавляет код, тесты, route wiring, public contract changes или изменения runtime behavior.

## 2. Текущая точка входа

- Stage 7 завершил ограниченную hotel-only MVP foundation.
- Stage 8.0-8.1 зафиксировали границы AI/LLM orchestration и `LlmClient`.
- Stage 8.2-8.5 добавили internal backend building blocks без подключения к routes.
- `PlanAssistantLlmDecisionUseCase` уже соединяет `GenerateLlmCandidateUseCase` и `PlanAssistantCandidateDecisionUseCase`.
- Assistant routes, public API, OpenAPI, frontend и hotel-search handoff остаются прежними.
- Real LLM provider, внешние вызовы и ключи доступа не добавлены.

## 3. Что уже есть после Stage 8.5

Internal pipeline сейчас выглядит так:

```text
LlmCandidateRequest
  -> GenerateLlmCandidateUseCase
  -> PlanAssistantCandidateDecisionUseCase
  -> AssistantCandidateDecision
```

Доступные decision outcomes:

- `ProceedWithCandidate` — валидный candidate может быть рассмотрен будущим orchestration layer;
- `AskClarification` — есть безопасный уточняющий вопрос;
- `Fallback` — безопасный default для empty, invalid, failure-like, unsupported или неполного результата.

Эти outcomes остаются внутренними. Они не являются public API contract и не должны напрямую менять поведение endpoint без отдельного шага.

## 4. Proposed internal assistant actions

Для будущего handoff достаточно рассматривать небольшие internal actions:

| Future action | Назначение |
|---|---|
| `CandidateReadyForReview` | Принять валидный candidate как вход для будущей проверки hotel-search criteria. |
| `ClarificationToUser` | Подготовить один уточняющий вопрос пользователю. |
| `SafeAssistantFallback` | Вернуть безопасный ответ без применения candidate и без вызова provider boundary. |
| `HotelOnlyBoundaryMessage` | Сообщить, что запрос выходит за hotel-only MVP, если это нужно для user-facing fallback. |

Названия являются design labels, а не требованием создать такие классы в Stage 8.6.

## 5. Mapping из AssistantCandidateDecision в future actions

| Current decision | Future action | Безопасная интерпретация | Что нельзя делать автоматически |
|---|---|---|---|
| `ProceedWithCandidate` | `CandidateReadyForReview` | Передать candidate в будущую internal проверку hotel-only intent, required fields, assumptions и unknowns. | Нельзя сразу вызывать hotel search, менять session state, раскрывать raw candidate в public response или подменять Stage 7 strict handoff. |
| `AskClarification` | `ClarificationToUser` | Подготовить один короткий уточняющий вопрос, если будущий endpoint contract позволяет существующую safe response shape. | Нельзя расширять public response shape или начинать dynamic clarification flow без отдельного contract/runtime шага. |
| `Fallback` | `SafeAssistantFallback` или `HotelOnlyBoundaryMessage` | Вернуть безопасный fallback без применения LLM candidate. Для unsupported intent сохранить hotel-only boundary. | Нельзя маскировать failure как найденные hotel facts, запускать provider boundary или выдавать assumptions за подтвержденные ограничения. |

## 6. Что безопасно для будущего route wiring

Будущий первый route wiring может быть безопасным только после отдельной readiness gate. Минимально безопасными выглядят такие правила:

- `AskClarification` можно сопоставить с существующим безопасным assistant clarification outcome, если это не меняет public request/response shape.
- `Fallback` можно сопоставить с уже существующим safe assistant fallback, если endpoint contract не расширяется.
- `ProceedWithCandidate` должен остановиться на internal review boundary и не должен сам запускать hotel search.
- Stage 7 strict explicit hotel-search handoff должен сохраниться как отдельный детерминированный путь до отдельного решения о его замене или объединении.
- Любое отображение в route должно проходить contract check: public API, OpenAPI и frontend не должны расходиться с runtime.

## 7. Что не безопасно и должно быть отложено

Отложено за пределы Stage 8.6 и будущего первого wiring без отдельного решения:

- автоматический вызов hotel search из `ProceedWithCandidate`;
- обновление session/search state на основе непроверенного candidate;
- изменение public API request/response shape;
- публикация raw LLM candidate, confidence, warnings или internal reasons в API;
- замена Stage 7 strict handoff LLM-потоком;
- подключение real LLM provider или provider-specific configuration;
- frontend changes и новый chat UI;
- flights, booking, payment, combined itinerary или general travel assistant behavior.

## 8. Как сохранить bounded hotel-only MVP

Будущий handoff должен соблюдать текущие границы:

- все действия остаются hotel-only;
- unsupported non-hotel intent уходит в safe fallback или hotel-only boundary message;
- provider facts продолжают приходить только через provider boundary;
- LLM candidate не становится hotel fact;
- assumptions и unknowns не смешиваются с user-provided constraints;
- public API меняется только через отдельный contract step;
- booking, payment, flights и combined itinerary не возвращаются в MVP v1.

## 9. Что не входит в Stage 8.6

- production code;
- backend tests;
- route wiring;
- runtime composition;
- изменения Assistant endpoint behavior;
- изменения hotel search handoff;
- OpenAPI, generated clients, frontend или CI gate;
- real LLM provider, внешние вызовы, ключи доступа или provider-specific настройки;
- durable storage, auth, booking flow или расширение MVP.

## 10. Риски преждевременного route wiring

- Незаметное изменение поведения Assistant endpoint.
- Расхождение runtime behavior с OpenAPI и contract notes.
- Неявное расширение public API через internal candidate fields.
- Смешивание deterministic fake LLM с пользовательским runtime flow.
- Подмена Stage 7 strict hotel-search handoff LLM candidate.
- Вызов provider boundary до проверки required fields, assumptions и unknowns.
- Размывание hotel-only MVP в general travel assistant.
- Ложное впечатление готовности к промышленному использованию.

## 11. Рекомендуемый Stage 8.7

`Stage 8.7 — Assistant LLM Route Wiring Readiness Gate`.

Рекомендуемый следующий шаг должен остаться review-only и проверить:

- какую existing Assistant response shape можно использовать без OpenAPI change;
- где проходит граница между clarification, fallback и candidate review;
- сохраняется ли Stage 7 strict handoff как отдельный deterministic path;
- какие runtime tests потребуются перед любой проводкой pipeline к routes;
- нужен ли отдельный internal action model skeleton до route wiring.

Если readiness gate покажет, что mapping недостаточно явный, следующим шагом должен быть минимальный internal action model skeleton без route wiring.

## 12. Verdict

Passed — Stage 8.6 фиксирует безопасное design-level сопоставление `AssistantCandidateDecision` с future assistant actions.

Следующий безопасный шаг — review-only readiness gate перед любым route wiring. Stage 8.6 не меняет runtime behavior, public API, OpenAPI, frontend, provider boundary или hotel-only MVP scope.
