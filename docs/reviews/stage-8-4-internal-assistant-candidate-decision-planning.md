# Stage 8.4 — Внутреннее планирование решения Assistant candidate

## 1. Цель Stage 8.4

Добавить минимальный backend-only internal decision layer, который преобразует `LlmCandidateValidationResult` в безопасное внутреннее решение ассистента для будущей orchestration layer.

Stage 8.4 не подключает decision layer к Assistant routes, не меняет runtime behavior и не означает production readiness.

## 2. Что добавлено

Добавлены:

- `AssistantCandidateDecision` — внутренняя typed decision model;
- `PlanAssistantCandidateDecisionUseCase` — deterministic planner поверх `LlmCandidateValidationResult`.

Decision model содержит:

- `ProceedWithCandidate` — валидный interpreted candidate может быть использован будущим internal orchestration layer;
- `AskClarification` — есть безопасный clarification question;
- `Fallback` — default safe outcome для invalid, empty, failure-like, unsupported или неполного результата.

## 3. Production files

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantCandidateDecision.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/PlanAssistantCandidateDecisionUseCase.kt`.

Изменен:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/llm/LlmCandidateValidationResult.kt` — `Rejected` получил optional `clarificationQuestion` для internal decision planning.

## 4. Добавленные тесты

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/assistant/PlanAssistantCandidateDecisionUseCaseTest.kt`.

Тесты проверяют:

- accepted interpreted candidate превращается в `ProceedWithCandidate`;
- accepted clarification candidate превращается в `AskClarification`;
- rejected result с clarification question превращается в `AskClarification`;
- rejected result без clarification question превращается в `Fallback`;
- failure-like result остается safe fallback и не выбрасывает исключение наружу;
- unsupported accepted candidate получает safe fallback;
- planner deterministic;
- planner создается без provider, network, credentials или external configuration.

## 5. Связь с LlmCandidateValidationResult

`PlanAssistantCandidateDecisionUseCase` принимает уже проверенный результат:

```text
LlmCandidateValidationResult -> PlanAssistantCandidateDecisionUseCase -> AssistantCandidateDecision
```

Он не вызывает `LlmClient`; это уже делает `GenerateLlmCandidateUseCase`. Он также не вызывает hotel provider, не создает search, не пишет session state и не выполняет route-level mapping.

## 6. Runtime behavior

Runtime behavior не изменен:

- `Application.kt` не создает `PlanAssistantCandidateDecisionUseCase`;
- Assistant routes не знают о новом decision layer;
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

- `AssistantCandidateDecision` является internal model, а не public API contract.
- `ProceedWithCandidate` не означает разрешение вызвать provider; future layer должен отдельно проверить hotel-search criteria и required fields.
- `AskClarification` пока не подключен к пользовательскому ответу.
- `Fallback` пока не мапится на public Assistant response.
- Stage 8.4 не подключает real LLM integration и не является production-readiness claim.

## 10. Рекомендуемый следующий шаг Stage 8.5

`Stage 8.5 — Internal Assistant LLM Pipeline Composition`.

Отдельная небольшая backend-only задача может добавить internal composition use case, который последовательно вызывает:

```text
GenerateLlmCandidateUseCase -> PlanAssistantCandidateDecisionUseCase
```

Stage 8.5 не должен подключаться к routes, менять public API, вызывать hotel provider, добавлять real LLM provider или менять runtime behavior.

## 11. Verdict

Passed — минимальный internal assistant candidate decision layer добавлен в пределах Stage 8.4.

Runtime behavior, routes, public API, OpenAPI, frontend, real provider, network и API keys не изменены. Stage 8 остается незавершенным и не означает production readiness.
