# Stage 7.39 — Assistant Endpoint Contract Shape Cleanup

## 1. Назначение Stage 7.39

Stage 7.39 выполняет узкий contract/documentation cleanup для Assistant endpoint contract shape после Stage 7.37 и Stage 7.38.

Цель этапа — уточнить форму request/response/error contract для двух Assistant endpoints, не меняя backend runtime behavior, backend tests, frontend code, generated clients, `tools/openapi-conformance/**`, `generated-client-ready-subset.yaml` или readiness state.

## 2. Baseline после Stage 7.38

Baseline после Stage 7.38:

- Stage 7.37 зафиксировал contract/runtime alignment notes для двух Assistant endpoint candidates.
- Stage 7.38 классифицировал gaps и рекомендовал порядок: contract shape cleanup first, runtime tests only after contract decision, conformance/tooling after contract/test clarity, manifest update only later.
- `docs/architecture/stage-7/generated-client-ready-subset.yaml` остается `status: "not_ready"` и `readinessClaim: false`.
- Assistant endpoints не включены в generated-client-ready subset.
- Generated-client/OpenAPI readiness не заявлена.
- Stage 7.40 не начат.

## 3. Source-of-truth и прочитанные правила/шаблоны

Прочитанные правила, шаблоны и source-of-truth документы:

- `AGENTS.md`;
- `docs/prompts/codex-rules.md`;
- `docs/prompts/review-template.md`;
- `docs/prompts/codex-review-template.md`;
- `docs/guides/documentation-style-guide.md`;
- `README.md`;
- `docs/ROADMAP.md`;
- `docs/roadmap/roadmap.md`;
- `docs/reviews/README.md`.

Примененные правила:

- `docs/roadmap/roadmap.md` остается primary roadmap/status source of truth.
- `README.md` и `docs/ROADMAP.md` остаются navigation docs.
- Review artifacts являются audit trail, а не active backlog.
- Historical reports не переписываются.
- Active human-readable documentation пишется на русском языке; technical identifiers, paths, commands, endpoint paths, statuses, field names, YAML values, stage/report names и commit messages не переводятся.
- Documentation/contract cleanup не должен превращаться в backend implementation, runtime test work, conformance/tooling work или manifest expansion.

## 4. Какие Stage 7 reports были прочитаны

Прочитанные Stage 7 reports:

- `docs/reviews/stage-7-24-openapi-conformance-manifest-validation-design.md`;
- `docs/reviews/stage-7-25-openapi-conformance-manifest-detection-validation.md`;
- `docs/reviews/stage-7-26-documentation-quality-calibration-audit.md`;
- `docs/reviews/stage-7-27-documentation-governance-rules-cleanup.md`;
- `docs/reviews/stage-7-28-roadmap-structure-refactor.md`;
- `docs/reviews/stage-7-29-active-documentation-language-normalization.md`;
- `docs/reviews/stage-7-30-documentation-final-quality-gate.md`;
- `docs/reviews/stage-7-31-resume-development-handoff.md`;
- `docs/reviews/stage-7-32-resume-stage-7-technical-context-review.md`;
- `docs/reviews/stage-7-33-ready-subset-manifest-candidate-definition.md`;
- `docs/reviews/stage-7-34-manifest-candidate-validation-hardening.md`;
- `docs/reviews/stage-7-35-endpoint-candidate-review.md`;
- `docs/reviews/stage-7-36-assistant-endpoint-candidate-clarification.md`;
- `docs/reviews/stage-7-37-assistant-endpoint-contract-runtime-alignment-notes.md`;
- `docs/reviews/stage-7-38-assistant-endpoint-alignment-cleanup-decision.md`.

## 5. Документированность Stage 7.39

Stage 7.39 явно зафиксирован в `docs/roadmap/roadmap.md` как следующий планируемый шаг: `Assistant Endpoint Contract Shape Cleanup`.

Roadmap задает название и boundary. Детальный scope взят из текущей явной задачи и Stage 7.38 sequencing decision.

## 6. Источник scope

Источник scope:

- явная user task для Stage 7.39;
- текущий primary roadmap/status в `docs/roadmap/roadmap.md`;
- Stage 7.38 cleanup decision;
- Stage 7.37 alignment notes;
- Stage 7.35-7.36 endpoint candidate clarification;
- Stage 7.24-7.25 manifest/conformance baseline;
- read-only просмотр OpenAPI, backend Assistant routes и conformance tool context.

## 7. Прочитанные contract files

Прочитанные contract files:

- `docs/architecture/stage-6/openapi-draft.yaml`;
- `docs/architecture/stage-6/openapi-contract-notes.md`;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- `tools/openapi-conformance/README.md`;
- `tools/openapi-conformance/src/placeholder-policy.ts`;
- `tools/openapi-conformance/src/report.ts`;
- `tools/openapi-conformance/src/subset-manifest.ts`.

## 8. Прочитанные runtime/backend files read-only

Прочитанные runtime/backend files read-only:

- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ApiRoutes.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorHandling.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorResponse.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/Serialization.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionBoundary.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantSessionStateStore.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantResponseSemantics.kt`;
- `services/backend/src/main/kotlin/com/travelassistant/backend/application/assistant/AssistantNextAction.kt`;
- `services/backend/README.md`.

## 9. Созданные файлы

- `docs/reviews/stage-7-39-assistant-endpoint-contract-shape-cleanup.md`

## 10. Измененные файлы

- `docs/architecture/stage-6/openapi-draft.yaml`;
- `docs/architecture/stage-6/openapi-contract-notes.md`;
- `docs/reviews/README.md`;
- `docs/roadmap/roadmap.md`.

## 11. Сводка contract cleanup

Stage 7.39 внес узкое contract shape cleanup:

- уточнил semantics optional request body для `POST /api/v1/assistant/sessions`;
- уточнил required request body semantics для `POST /api/v1/assistant/sessions/{sessionId}/messages`;
- зафиксировал `message.maxLength` как contract-level limit без runtime enforcement claim;
- уточнил `clientContext` как optional behavior-neutral client hints;
- сделал `nextAction` required response field в `AssistantMessageResponse`;
- оставил `hotelSearchRequest` optional/future-only;
- оставил `searchIntentSummary` optional/future-facing;
- зафиксировал, что malformed JSON, unknown JSON fields и hard maxLength enforcement остаются not runtime-validated;
- не расширял endpoint set, status codes, backend behavior, tests, manifest или conformance tool.

## 12. Решения по endpoint

| Endpoint | Решение |
|---|---|
| `POST /api/v1/assistant/sessions` | `requestBody` остается optional. No-body означает session-only creation. Если `application/json` body передан, он следует `AssistantMessageRequest`, где `message` required на уровне schema. Response остается `201` с `AssistantMessageResponse`, где `session`, `assistantMessage` и `nextAction` required. Current-session lifecycle остается process-local/foundation boundary. |
| `POST /api/v1/assistant/sessions/{sessionId}/messages` | `requestBody` остается required и использует `AssistantMessageRequest`. `message` required на уровне schema. Response остается `200` с `AssistantMessageResponse`, где `session`, `assistantMessage` и `nextAction` required. `404` остается `SESSION_NOT_FOUND`. Поведение malformed/unknown JSON не финализировано runtime-level tests на Stage 7.39. |

## 13. Решения по gap

| Gap | Решение Stage 7.39 |
|---|---|
| Request validation semantics | Уточнены на contract level: create-session допускает no-body, message-intake требует body, body-with-message использует `AssistantMessageRequest`. Runtime tests остаются future step. |
| Malformed/unknown JSON behavior | Не over-specified как runtime guarantee. OpenAPI schema выражает contract intent, но фактическое Ktor decoding/rejection behavior остается not runtime-validated. |
| `message.maxLength` | `maxLength: 4000` сохранен как contract-level limit. Runtime enforcement не заявлен. |
| `clientContext` | Сохранен как optional behavior-neutral container для `locale` и `timezone`; не обещает validation или behavior changes. |
| `nextAction` | Зафиксирован как required response field, потому что текущий Assistant foundation всегда возвращает action hint. |
| Optional/required response fields | `session`, `assistantMessage`, `nextAction` required; `hotelSearchRequest` и `searchIntentSummary` optional/future-facing. |
| Error taxonomy | `VALIDATION_ERROR`, `SESSION_NOT_FOUND` и `INTERNAL_ERROR` остаются contract-facing для двух Assistant endpoints. Foundation-only `NOT_IMPLEMENTED` и generic `NOT_FOUND` не считаются финальной generated-client taxonomy. |
| Lifecycle/security/session ownership assumptions | Оставлены current-session/process-local; durable persistence, account history, auth/session ownership и production security readiness future-only. |

## 14. Что осталось not runtime-validated

Остается not runtime-validated:

- malformed JSON body behavior;
- unknown JSON fields rejection/acceptance behavior;
- runtime enforcement of `message.maxLength`;
- precise treatment of non-object JSON body values;
- runtime response schema validation against OpenAPI;
- generated-client compile compatibility;
- runtime HTTP contract tests for Assistant endpoints.

## 15. Что осталось future-only

Остается future-only:

- backend runtime tests for chosen Assistant contract shape;
- conformance/tooling follow-up for endpoint reference validation or runtime HTTP checks;
- manifest expansion for Assistant endpoints;
- generated-client target selection, generation and compile checks;
- OpenAPI finalization / readiness gate;
- auth/session ownership, durable persistence, account history and cross-device sync;
- LLM orchestration, provider-backed hotel search, `hotelSearchRequest` construction, ranking/recommendation behavior and frontend integration;
- Stage 8 AI/LLM orchestration work.

## 16. Подтверждение readiness / non-claims

Stage 7.39 явно не заявляет:

- no generated-client readiness claim;
- no OpenAPI finalization claim;
- no generated clients;
- no backend behavior change;
- no backend runtime tests added;
- no runtime validation claim;
- no conformance tool behavior change;
- no manifest expansion;
- no CI/Gradle integration;
- no Stage 8 activation.

## 17. Команды validation и результаты

| Command | Result |
|---|---|
| `git status --short --untracked-files=all` | Passed; показаны только expected Stage 7.39 files: modified `openapi-contract-notes.md`, `openapi-draft.yaml`, `docs/reviews/README.md`, `docs/roadmap/roadmap.md` и untracked Stage 7.39 report. |
| `git diff --stat` | Passed; tracked diff ограничен `openapi-contract-notes.md`, `openapi-draft.yaml`, `docs/reviews/README.md` и `docs/roadmap/roadmap.md`. |
| `git diff --name-only` | Passed; tracked changed files ограничены expected Stage 7.39 tracked files. |
| `git diff --check` | Passed; no whitespace errors. |
| `./tools/openapi-conformance/check` | Passed with exit code `0`; OpenAPI 3.1.0 parsed with 9 operations, static runtime scan found 9 routes, report kept `status: "not_ready"`, `readinessClaim: false`, `manifestValidation.status: "advisory_passed"`, `blockingFindings: []`. |

Backend Gradle tests не запускались, потому что Stage 7.39 не меняет backend source, backend tests или runtime behavior. `npm test` не запускался, потому что `tools/openapi-conformance/**` не менялись. Existing OpenAPI sanity command `./tools/openapi-conformance/check` запускался из-за изменения OpenAPI YAML; command не запускал backend server и не выполнял HTTP requests.

## 18. Оценка scope-control

Stage 7.39 остался внутри разрешенного scope:

- backend code не менялся;
- backend tests не менялись;
- frontend code не менялся;
- `tools/openapi-conformance/**` не менялся;
- generated clients не создавались;
- `docs/architecture/stage-7/generated-client-ready-subset.yaml` не менялся;
- backend server не запускался;
- HTTP/network requests не выполнялись;
- Stage 7.40 не начат.

## 19. Риски / открытые вопросы

| Риск / вопрос | Статус |
|---|---|
| OpenAPI теперь точнее фиксирует `nextAction` как required, но runtime schema validation еще отсутствует. | Требуется будущий runtime contract test cleanup. |
| `message.maxLength` остается contract-level limit без runtime enforcement claim. | Требуется будущий backend/runtime test или implementation decision, если endpoint готовится к readiness. |
| Malformed JSON и unknown fields могут вести себя иначе, чем schema intent. | Требуется будущий explicit runtime behavior decision/tests. |
| `clientContext` может создать ожидание behavior changes у client users. | В Stage 7.39 явно зафиксирован behavior-neutral status. |
| Session ownership/security не решены. | Future-only до отдельного security/session stage. |

## 20. Рекомендуемый следующий этап

Рекомендуемый следующий этап: Stage 7.40 — Assistant Endpoint Runtime Contract Test Cleanup, только через отдельную явную roadmap-aligned задачу.

Рекомендуемый scope Stage 7.40: добавить или спланировать runtime tests только для уже выбранной Stage 7.39 contract shape, не меняя conformance tool, manifest, generated clients, frontend или Stage 8 state без отдельного явного scope.
