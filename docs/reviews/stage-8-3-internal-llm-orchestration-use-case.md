# Stage 8.3 — Внутренний use case LLM-оркестрации

## 1. Цель Stage 8.3

Добавить минимальный backend-only application use case для будущей AI/LLM-оркестрации: вызвать `LlmClient`, применить `LlmCandidateValidator` и вернуть безопасный typed result.

Stage 8.3 не подключает этот use case к Assistant routes, не меняет runtime behavior и не означает production readiness.

## 2. Что добавлено

Добавлен `GenerateLlmCandidateUseCase` в `application/llm`.

Use case:

- принимает существующий безопасный `LlmCandidateRequest`;
- вызывает application-owned `LlmClient`;
- передает ответ в `LlmCandidateValidator`;
- возвращает `LlmCandidateValidationResult.Accepted` для валидного кандидата;
- возвращает `LlmCandidateValidationResult.Rejected` с безопасным `ASK_CLARIFICATION` fallback для empty, failure, invalid result или unexpected runtime exception;
- не меняет состояние и не обращается к hotel provider.

## 3. Production files

Создан:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/llm/GenerateLlmCandidateUseCase.kt`.

Существующие production files не изменялись.

## 4. Добавленные тесты

Создан:

- `services/backend/src/test/kotlin/com/travelassistant/backend/application/llm/GenerateLlmCandidateUseCaseTest.kt`.

Тесты проверяют:

- валидный LLM candidate проходит через use case;
- invalid candidate превращается в safe fallback;
- empty response превращается в safe fallback;
- ambiguous candidate с clarification question принимается как валидный кандидат;
- failure response от fake LLM не выбрасывает исключение наружу;
- unexpected runtime exception от `LlmClient` превращается в safe fallback;
- результат детерминирован при использовании `FakeLlmClient`.

Тесты не запускают Ktor application, routes, сеть или внешние сервисы.

## 5. Связь с LlmClient и validator

`GenerateLlmCandidateUseCase` остается тонкой orchestration boundary:

```text
LlmCandidateRequest -> LlmClient -> LlmCandidateValidator -> LlmCandidateValidationResult
```

Use case не интерпретирует provider facts, не создает hotel search, не выбирает следующий public Assistant action и не выполняет route-level mapping.

## 6. Runtime behavior

Runtime behavior не изменен:

- `Application.kt` не создает `GenerateLlmCandidateUseCase`;
- Assistant routes не знают о новом use case;
- strict Stage 7.50 hotel-search handoff остается прежним;
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
- retry, timeout или cost/model configuration.

Новые зависимости и build/package changes не потребовались.

## 9. Риски и ограничения

- Use case только валидирует структурный результат `LlmClient`; он не выполняет полноценную domain validation hotel-search значений.
- Fallback пока возвращается как typed internal result и не подключен к пользовательскому ответу.
- `FakeLlmClient` остается test double и не моделирует качество, latency или failure modes реальной LLM.
- Accepted ambiguous candidate не означает разрешение вызвать provider; следующий application слой должен отдельно выбрать безопасное действие.
- Stage 8.3 не является готовностью real LLM integration или production readiness.

## 10. Рекомендуемый следующий шаг Stage 8.4

`Stage 8.4 — Internal Assistant Candidate Decision Planning`.

Отдельная небольшая backend-only задача может добавить internal decision boundary, который преобразует validated `LlmCandidateValidationResult` в безопасное application decision:

- ask clarification;
- unsupported hotel-only boundary message;
- candidate ready for future hotel-search criteria validation;
- fallback for invalid/empty/failure result.

Stage 8.4 не должен подключаться к routes, менять public API, вызывать hotel provider или добавлять real LLM provider.

## 11. Verdict

Passed — минимальный internal `GenerateLlmCandidateUseCase` добавлен в пределах Stage 8.3.

Runtime behavior, routes, public API, OpenAPI, frontend, real provider, network и API keys не изменены. Stage 8 остается незавершенным и не означает production readiness.
