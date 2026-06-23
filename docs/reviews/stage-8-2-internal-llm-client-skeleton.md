# Stage 8.2 — Внутренний skeleton LlmClient

## 1. Цель Stage 8.2

Добавить минимальную backend-only основу внутренней границы `LlmClient`: application-owned контракт, provider-independent модели, validation boundary, детерминированный fake и targeted unit tests.

Stage 8.2 не подключает LLM orchestration к runtime и не меняет статус Stage 8.

## 2. Что добавлено

В `application/llm` добавлены:

- `LlmClient` — внутренний контракт генерации кандидата;
- `LlmCandidateRequest` — минимальный вход с текущим сообщением, подтвержденными constraints и missing fields;
- `LlmCandidate` — provider-independent кандидат с outcome, intent, constraints, conflicts, clarification и warnings;
- `LlmClientResponse` — candidate, empty или failure result;
- `LlmCandidateValidator` — проверка целостности и согласованности кандидата;
- `LlmCandidateValidationResult` — accepted/rejected result с безопасным `ASK_CLARIFICATION` fallback.

В `infrastructure/llm` добавлен `FakeLlmClient`. Он возвращает заранее заданный `LlmClientResponse`, не использует сеть и не содержит provider-specific логики.

## 3. Production code files

Созданы:

- `services/backend/src/main/kotlin/com/travelassistant/backend/application/llm/LlmClient.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/llm/LlmCandidateRequest.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/llm/LlmCandidate.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/llm/LlmClientResponse.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/llm/LlmCandidateValidationResult.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/llm/LlmCandidateValidator.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/infrastructure/llm/FakeLlmClient.kt`.

Существующие production files не изменялись.

## 4. Добавленные тесты

- `LlmCandidateValidatorTest` проверяет happy path, ambiguous candidate, rejection invalid/empty/failure responses и безопасный fallback.
- `FakeLlmClientTest` проверяет детерминированный valid, empty и ambiguous response.

Тесты не запускают Ktor application, routes, сеть или внешние сервисы.

## 5. Runtime behavior

Runtime behavior не изменен:

- `Application.kt` не создает `LlmClient`;
- Assistant routes и `ApiRoutes.kt` не знают о новой границе;
- strict hotel-search handoff, session state, ranking и fake hotel provider работают как раньше;
- public request/response shape не менялся.

Новый код доступен только как внутренняя compile-time основа и напрямую не участвует в обработке запросов.

## 6. Внешние интеграции и соседние области

Не добавлены:

- real LLM provider, SDK, network calls, API keys, secrets или environment variables;
- provider-specific request/response parsing, retry или timeout behavior;
- OpenAPI changes, generated clients, manifest или CI gate;
- frontend changes;
- durable storage, auth, booking, payment, flights или combined itinerary.

Новые зависимости и build/package changes не потребовались.

## 7. Риски и ограничения

- Модели являются минимальным internal skeleton, а не финальным prompt или LLM provider contract.
- Validator проверяет структурную согласованность кандидата, но не выполняет полноценную domain validation hotel-search значений.
- `FakeLlmClient` является test double и не моделирует качество, latency или failures реальной LLM.
- Fallback пока представлен типизированным решением, но не подключен к Assistant behavior.
- Skeleton не является production-readiness claim.

## 8. Рекомендуемый следующий шаг Stage 8.3

`Stage 8.3 — Internal LLM Candidate Orchestration Use Case`.

Отдельная небольшая backend-only задача может добавить application use case, который:

- вызывает `LlmClient`;
- передает ответ в `LlmCandidateValidator`;
- возвращает типизированный accepted/fallback result;
- проверяется через `FakeLlmClient` и targeted unit tests.

Stage 8.3 не должен подключаться к Assistant routes, менять runtime behavior, добавлять real LLM provider, OpenAPI или frontend.

## 9. Verdict

Passed — минимальный internal `LlmClient` skeleton, validator, deterministic fake и targeted tests добавлены в согласованных границах.

Stage 8.2 не меняет runtime behavior, не подключает real provider и не означает завершение Stage 8 или production readiness.
