# Stage 7.40 — Assistant Endpoint Runtime Contract Test Cleanup

## 1. Назначение Stage 7.40

Stage 7.40 выполняет узкий backend test cleanup для `Assistant` endpoints: уточняет runtime contract tests по contract shape, зафиксированному в Stage 7.39, без изменения production backend behavior, OpenAPI contracts, generated clients, conformance tool, manifest или readiness state.

Этот этап не является generated-client readiness gate, OpenAPI finalization gate, full conformance gate или началом Stage 7.41.

## 2. Baseline после Stage 7.39

Baseline перед началом Stage 7.40:

- последний завершенный commit: `e30023e docs: clarify stage 7.39 assistant endpoint contract shape`;
- working tree был clean по `git status --short --untracked-files=all`;
- `git diff --stat` не показывал изменений;
- roadmap фиксировал Stage 7.39 как последний завершенный этап и Stage 7.40 как следующий явный шаг;
- Stage 8+ оставались Planned и не активированы.

## 3. Source-of-truth и прочитанные правила/шаблоны

Прочитаны и применены:

- `AGENTS.md`;
- `docs/prompts/codex-rules.md`;
- `docs/prompts/review-template.md`;
- `docs/prompts/codex-review-template.md`;
- `docs/guides/documentation-style-guide.md`;
- `README.md`;
- `docs/ROADMAP.md`;
- `docs/roadmap/roadmap.md`;
- `docs/reviews/README.md`;
- `docs/development/README.md`;
- `docs/development/testing-strategy.md`;
- `docs/development/quality-gates.md`;
- `docs/development/coding-standards.md`;
- `docs/development/kotlin-backend-style-guide.md`;
- `docs/development/definition-of-done.md`;
- `docs/development/documentation-guidelines.md`;
- `docs/architecture/backend-layering-rules.md`.

Примененные guardrails:

- `docs/roadmap/roadmap.md` остается primary roadmap/status source of truth;
- active project documentation пишется на русском языке;
- backend stack остается Kotlin + Ktor;
- production backend behavior не меняется;
- generated-client/OpenAPI readiness не заявляется;
- future-stage recommendations не выполняются без отдельной явной задачи.

## 4. Какие Stage 7 reports были прочитаны

Для Stage 7.40 были прочитаны:

- `docs/reviews/stage-7-24-openapi-conformance-manifest-validation-design.md`;
- `docs/reviews/stage-7-25-openapi-conformance-manifest-detection-validation.md`;
- `docs/reviews/stage-7-35-endpoint-candidate-review.md`;
- `docs/reviews/stage-7-36-assistant-endpoint-candidate-clarification.md`;
- `docs/reviews/stage-7-37-assistant-endpoint-contract-runtime-alignment-notes.md`;
- `docs/reviews/stage-7-38-assistant-endpoint-alignment-cleanup-decision.md`;
- `docs/reviews/stage-7-39-assistant-endpoint-contract-shape-cleanup.md`.

Stage 7.24-7.25 использовались как pre-audit technical baseline для manifest/conformance boundaries. Stage 7.37-7.39 использовались как прямой источник contract/runtime gaps, cleanup decision и уточненного Assistant contract shape.

## 5. Stage 7.40 documented или inferred

Stage 7.40 явно описан в active roadmap как следующий планируемый шаг: `Assistant Endpoint Runtime Contract Test Cleanup`.

Детальный scope взят из текущей явной задачи пользователя и согласован с Stage 7.38 sequencing:

1. contract shape cleanup first;
2. runtime tests after contract decision;
3. conformance/tooling after contract/test clarity;
4. manifest update only later.

## 6. Источник scope

Scope Stage 7.40:

- добавить или уточнить backend runtime contract tests для `POST /api/v1/assistant/sessions`;
- добавить или уточнить backend runtime contract tests для `POST /api/v1/assistant/sessions/{sessionId}/messages`;
- покрывать только поведение, которое уже существует в runtime;
- не менять production backend code;
- не менять OpenAPI contracts;
- не менять `tools/openapi-conformance/**`;
- не менять `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- не генерировать clients;
- не начинать Stage 7.41.

## 7. Прочитанные contract files

Прочитаны:

- `docs/architecture/stage-6/openapi-draft.yaml`;
- `docs/architecture/stage-6/openapi-contract-notes.md`;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml` как context-only документ.

Ключевой contract baseline после Stage 7.39:

- `POST /api/v1/assistant/sessions` допускает no-body session-only creation;
- если request body передан, `message` является contract-level required field;
- `POST /api/v1/assistant/sessions/{sessionId}/messages` требует request body и `message`;
- `message.maxLength` зафиксирован как contract-level limit без runtime enforcement claim;
- `clientContext` optional и behavior-neutral;
- `nextAction` является required response field;
- `hotelSearchRequest` и `searchIntentSummary` остаются optional/future-facing;
- malformed/unknown JSON behavior не зафиксирован как runtime-validated guarantee;
- generated-client readiness не заявлена.

## 8. Прочитанные runtime/backend files

Прочитаны read-only:

- `services/backend/README.md`;
- `services/backend/build.gradle.kts`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/Application.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ApiRoutes.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/Serialization.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorHandling.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorResponse.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionBoundary.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantResponseSemantics.kt`;
- существующие backend tests в `services/backend/src/test/kotlin/com/travelassistant/backend/**`.

Production files не изменялись.

## 9. Созданные/измененные test files

Созданные backend test files: отсутствуют.

Измененный backend test file:

- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`.

Изменения добавляют runtime contract coverage к уже существующим `Assistant` route tests вместо создания параллельной test infrastructure.

## 10. Резюме runtime contract tests

Добавлены узкие проверки:

- `createAssistantSessionAcceptsOptionalClientContextAsBehaviorNeutralInput`;
- `acceptAssistantMessageAcceptsOptionalClientContextAndKeepsNextActionRequired`;
- `missingInitialAssistantMessageReturnsValidationErrorWhenRequestBodyIsPresent`.

Существующие tests уже покрывали:

- no-body `POST /api/v1/assistant/sessions` возвращает `201 Created`;
- session-only response содержит `session`, `assistantMessage`, `nextAction`;
- initial `message` на `POST /api/v1/assistant/sessions` принимается как foundation intake;
- valid message на `POST /api/v1/assistant/sessions/{sessionId}/messages` возвращает `200 OK`;
- unknown `sessionId` возвращает structured `404` с `SESSION_NOT_FOUND`;
- blank/missing `message` возвращает structured `400` с `VALIDATION_ERROR`;
- optional/future-facing public fields не становятся runtime-required assertions.

## 11. Покрытие по endpoint

| Endpoint | Coverage после Stage 7.40 | Статус |
|---|---|---|
| `POST /api/v1/assistant/sessions` | no-body session-only creation, optional initial `message`, optional behavior-neutral `clientContext`, required response fields, blank/missing body-field validation при переданном body | Покрыто для текущего runtime |
| `POST /api/v1/assistant/sessions/{sessionId}/messages` | valid `message`, optional behavior-neutral `clientContext`, required `nextAction`, required response fields, unknown `sessionId` -> `404` `SESSION_NOT_FOUND`, blank/missing `message` -> `400` `VALIDATION_ERROR` | Покрыто для текущего runtime |

## 12. Покрытие по gap

| Gap | Stage 7.40 coverage | Статус |
|---|---|---|
| request validation semantics | Missing/blank `message` покрыт для message endpoint; missing `message` при переданном create-session body покрыт | Частично runtime-validated |
| malformed/unknown JSON behavior | Не добавлялось, потому что Stage 7.39 не делает это runtime guarantee | Not runtime-validated |
| `message.maxLength` | Не тестировалось, потому что runtime enforcement не заявлен и production behavior не менялся | Not runtime-validated |
| `clientContext` | Optional behavior-neutral acceptance покрыт для обоих Assistant endpoints | Runtime-covered для принятой current shape |
| `nextAction` | Required response field/value покрыт для session creation и message intake | Runtime-covered для текущей response shape |
| optional/required response fields | Required fields покрыты; optional/future-facing fields не превращены в required runtime assertions | Частично runtime-validated |
| error taxonomy | `VALIDATION_ERROR` и `SESSION_NOT_FOUND` покрыты там, где current runtime уже поддерживает их | Частично runtime-validated |
| lifecycle/security/session ownership assumptions | Не тестировалось; current runtime остается process-local foundation без auth/account ownership | Future-only |
| OpenAPI/runtime mismatch risk | Покрыта только выбранная current Assistant contract shape; full schema/runtime validation не выполнялась | Частично |
| generated-client readiness implications | Не является readiness evidence | Не readiness evidence |

## 13. Найденные contract/runtime mismatches

В покрытой Stage 7.40 области новых contract/runtime mismatches не обнаружено.

Непокрытые gaps не трактуются как mismatch closure:

- malformed JSON behavior;
- unknown JSON fields behavior;
- `message.maxLength` runtime enforcement;
- full response schema validation;
- generated-client compile/runtime compatibility;
- lifecycle/security/session ownership semantics.

## 14. Что осталось not runtime-validated

Остается not runtime-validated:

- malformed JSON body behavior;
- unknown JSON fields behavior;
- non-object JSON body behavior;
- `message.maxLength` runtime enforcement;
- exact OpenAPI schema validation against runtime responses;
- runtime behavior for optional future-facing fields `hotelSearchRequest` и `searchIntentSummary`;
- generated-client compatibility;
- conformance-tool runtime/API comparison.

## 15. Что осталось future-only

Остается future-only:

- durable session persistence;
- session retrieval/history;
- auth/account ownership and cross-user access control;
- lifecycle state machine beyond process-local foundation;
- real clarification flow;
- natural-language requirements extraction;
- real `hotelSearchRequest` construction;
- provider-backed hotel search;
- generated-client-ready subset expansion;
- generated clients;
- CI/Gradle conformance gate integration;
- Stage 8 AI/LLM orchestration activation.

## 16. Readiness / non-claims confirmation

Stage 7.40 подтверждает:

- no generated-client readiness claim;
- no OpenAPI finalization claim;
- no generated clients;
- no production backend behavior change;
- no OpenAPI contract change;
- no conformance tool behavior change;
- no manifest expansion;
- no CI/Gradle integration;
- no Stage 8 activation.

## 17. Validation commands/results

Выполнено:

- `git status --short --untracked-files=all` — baseline clean before Stage 7.40;
- `git log --oneline -12` — подтвердил `e30023e` как последний Stage 7.39 commit;
- `git diff --stat` — baseline diff empty before Stage 7.40;
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test --no-daemon` из `services/backend` — passed.
- `git diff --check` — passed;
- `git status --short --untracked-files=all` — показывает только ожидаемые Stage 7.40 files: `docs/reviews/README.md`, `docs/roadmap/roadmap.md`, `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt` и новый untracked report `docs/reviews/stage-7-40-assistant-endpoint-runtime-contract-test-cleanup.md`;
- `git diff --stat` — показывает только tracked Stage 7.40 изменения в reviews index, roadmap и backend Assistant route test;
- `git diff --name-only` — показывает только tracked Stage 7.40 изменения: `docs/reviews/README.md`, `docs/roadmap/roadmap.md`, `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`.

Первый запуск `./gradlew test --no-daemon` без explicit `JAVA_HOME` не дошел до tests, потому что локальный `JAVA_HOME` указывал на invalid directory. Повторный запуск с documented `JAVA_HOME` прошел успешно.

## 18. Scope-control assessment

Scope удержан:

- backend production code не менялся;
- backend tests изменены только в `AssistantSessionRoutesTest.kt`;
- OpenAPI contract files не менялись;
- frontend code не менялся;
- generated clients не создавались;
- `tools/openapi-conformance/**` не менялся;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml` не менялся;
- backend server вручную не запускался;
- external HTTP/network requests не выполнялись;
- Stage 7.41 не начинался.

## 19. Risks/open questions

- `message.maxLength` остается contract-level limit без runtime enforcement test.
- malformed/unknown JSON behavior остается intentionally not runtime-validated, чтобы не закреплять случайное поведение без отдельного contract/runtime cleanup.
- `clientContext` покрыт только как accepted behavior-neutral input; его валидация, нормализация или использование не заявлены.
- `nextAction` покрыт как current required response field, но не является proof of search readiness.
- `SESSION_NOT_FOUND` покрыт для process-local foundation, но не закрывает future auth/account/session ownership semantics.

## 20. Recommended next stage

Рекомендуемый следующий этап: Stage 7.41 — Assistant Endpoint Conformance/Tooling Follow-up Decision, только отдельной явной roadmap-aligned задачей.

Stage 7.41 не должен начинаться автоматически из этого report. До любых manifest updates или readiness claims нужно отдельно решить, какие Assistant endpoint checks можно безопасно добавить в conformance/tooling слой после Stage 7.39 contract cleanup и Stage 7.40 runtime test cleanup.
