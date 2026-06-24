# Stage 8.5 — Внутренняя композиция Assistant LLM pipeline

## 1. Цель Stage 8.5

Добавить минимальный backend-only internal pipeline use case, который последовательно соединяет `GenerateLlmCandidateUseCase` и `PlanAssistantCandidateDecisionUseCase`.

Stage 8.5 возвращает безопасный `AssistantCandidateDecision`, но не подключает pipeline к routes, runtime composition или public API.

## 2. Что добавлено

Добавлен `PlanAssistantLlmDecisionUseCase` в `application/assistant`.

Pipeline:

- принимает существующий безопасный `LlmCandidateRequest`;
- вызывает `GenerateLlmCandidateUseCase`;
- передает `LlmCandidateValidationResult` в `PlanAssistantCandidateDecisionUseCase`;
- возвращает `AssistantCandidateDecision`;
- возвращает safe fallback, если внутренний шаг неожиданно выбрасывает `RuntimeException`;
- не меняет состояние и не обращается к hotel provider.

## 3. Production files

Создан:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanAssistantLlmDecisionUseCase.kt`.

Существующие production files не изменялись.

## 4. Добавленные тесты

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/PlanAssistantLlmDecisionUseCaseTest.kt`.

Тесты проверяют:

- valid candidate проходит через pipeline и дает `ProceedWithCandidate`;
- clarification candidate дает `AskClarification`;
- invalid candidate дает safe fallback;
- empty response дает safe fallback;
- fake LLM failure дает safe fallback;
- unexpected internal exception не выходит наружу;
- pipeline deterministic;
- pipeline создается без provider, network, credentials или external configuration.

## 5. Связь с существующими use cases

Stage 8.5 добавляет только композицию уже созданных internal шагов:

```text
LlmCandidateRequest
  -> GenerateLlmCandidateUseCase
  -> PlanAssistantCandidateDecisionUseCase
  -> AssistantCandidateDecision
```

`PlanAssistantLlmDecisionUseCase` не вызывает `LlmClient` напрямую, не создает hotel search, не вызывает hotel provider и не выполняет route-level mapping.

## 6. Runtime behavior

Runtime behavior не изменен:

- `Application.kt` не создает `PlanAssistantLlmDecisionUseCase`;
- Assistant routes не знают о новом pipeline;
- Stage 7.50 strict hotel-search handoff остается прежним;
- search/offers endpoints продолжают использовать существующий fake-provider flow.

Новый код доступен только как internal application-layer building block и не участвует в обработке запросов.

## 7. Routes, public API, OpenAPI и frontend

Не изменялись:

- Assistant routes;
- hotel search routes;
- public request/response shape;
- OpenAPI artifacts;
- generated clients;
- frontend code.

## 8. Real provider, network и API keys

Не добавлены:

- real LLM provider;
- network calls;
- API keys или environment variables;
- provider-specific request/response parsing;
- retry, timeout или model configuration.

Новые зависимости и build/package changes не потребовались.

## 9. Риски и ограничения

- Pipeline является internal composition, а не public Assistant behavior.
- `ProceedWithCandidate` не означает разрешение вызвать provider; future layer должен отдельно проверить hotel-search criteria и required fields.
- `AskClarification` пока не подключен к пользовательскому ответу.
- `Fallback` пока не мапится на public Assistant response.
- Stage 8.5 не подключает real LLM integration и не является production-readiness claim.

## 10. Рекомендуемый следующий шаг Stage 8.6

`Stage 8.6 — Internal Natural-Language Assistant Handoff Planning`.

Отдельная небольшая backend-only задача может определить, как internal `AssistantCandidateDecision` будет безопасно сопоставляться с будущими assistant actions без изменения public routes. Такой шаг должен оставаться без route wiring, real provider, OpenAPI changes и real LLM integration.

## 11. Verdict

Passed — минимальный internal Assistant LLM pipeline composition добавлен в пределах Stage 8.5.

Runtime behavior, routes, public API, OpenAPI, frontend, real provider, network и API keys не изменены. Stage 8 остается незавершенным и не означает production readiness.
