# Stage 7.36 — Assistant Endpoint Candidate Clarification

## 1. Scope

- [x] Review-only assistant endpoint clarification
- [x] No manifest changes
- [x] No generated-client readiness claim
- [x] No OpenAPI/API contract changes
- [x] No backend/frontend runtime changes
- [x] No generated clients
- [x] No CI/Gradle integration
- [x] No Stage 8 activation

## 2. Executive summary

Stage 7.36 уточнил только два assistant endpoint candidates из Stage 7.35: `POST /api/v1/assistant/sessions` и `POST /api/v1/assistant/sessions/{sessionId}/messages`.

Оба endpoint существуют в OpenAPI draft и backend route inventory, но остаются `not_ready`. Они не добавлялись в `generated-client-ready-subset.yaml`, не считаются generated-client-ready и требуют отдельного contract/runtime alignment перед возможным будущим manifest update.

Итоговое решение для обоих endpoint: `blocked_pending_contract_clarification`. Основная причина — нужно явно согласовать optional/required request semantics, runtime DTO/schema alignment, session lifecycle/error behavior и security/product assumptions до любого manifest expansion.

## 3. Endpoints reviewed

| Method | Path | Stage 7.35 classification | Stage 7.36 decision |
|---|---|---|---|
| POST | `/api/v1/assistant/sessions` | `future_candidate` | `blocked_pending_contract_clarification` |
| POST | `/api/v1/assistant/sessions/{sessionId}/messages` | `future_candidate` | `blocked_pending_contract_clarification` |

## 4. Contract shape review

| Endpoint | Request shape | Response shape | Errors/status codes | Stability note |
|---|---|---|---|---|
| `POST /api/v1/assistant/sessions` | OpenAPI declares optional request body using `AssistantMessageRequest`; if body is present, schema requires `message` with `minLength: 1`, `maxLength: 4000`; optional `clientContext.locale` and `clientContext.timezone`; `additionalProperties: false`. | `201` returns `AssistantMessageResponse`: required `session` and `assistantMessage`; optional `nextAction` and `hotelSearchRequest` in OpenAPI schema. | `201`, `400` `ValidationError`, `500` `InternalError`. | Not stable enough for manifest update. Clarify no-body vs body-without-message behavior, runtime DTO nullability vs OpenAPI required `message`, and whether runtime-always-present `nextAction` should remain optional in contract before generated-client candidate consideration. |
| `POST /api/v1/assistant/sessions/{sessionId}/messages` | Requires `sessionId` path parameter and required `AssistantMessageRequest` body; schema requires `message` with `minLength: 1`, `maxLength: 4000`; optional `clientContext.locale` and `clientContext.timezone`; `additionalProperties: false`. | `200` returns `AssistantMessageResponse`: required `session` and `assistantMessage`; optional `nextAction` and `hotelSearchRequest` in OpenAPI schema. | `200`, `400` `ValidationError`, `404` `SessionNotFound`, `500` `InternalError`. | Not stable enough for manifest update. Clarify session lifecycle, missing/expired session semantics, runtime validation behavior, DTO/schema alignment and whether response optionality matches generated-client expectations. |

## 5. Backend runtime shape review

| Endpoint | Backend evidence | Runtime behavior | Dependencies | Stability note |
|---|---|---|---|---|
| `POST /api/v1/assistant/sessions` | `AssistantPlaceholderRoutes.kt:26`; DTOs in `AssistantPlaceholderRoutes.kt:104-176`; local boundary in `AssistantSessionBoundary.kt:15-122`; in-memory store in `AssistantSessionStateStore.kt:1-24`. | Reads nullable JSON request. If request exists and `message` is blank/missing, returns `400`. Creates process-local session. If initial message exists, immediately records it and returns assistant response with `201`. Without initial message, returns session response with `201`. | Local Kotlin/Ktor route, process-local `InMemoryAssistantSessionStateStore`, local placeholder clarification reply. No external provider, no LLM call, no durable persistence. | Partial/minimal Stage 7 boundary. It is useful as a future candidate but still needs runtime contract/schema alignment before manifest inclusion. |
| `POST /api/v1/assistant/sessions/{sessionId}/messages` | `AssistantPlaceholderRoutes.kt:59`; `AssistantSessionNotFoundException` handling in `ErrorHandling.kt`; local boundary in `AssistantSessionBoundary.kt:99-121`; in-memory store in `AssistantSessionStateStore.kt:1-24`. | Reads nullable JSON request. Missing/blank `message` returns `400`. Existing session message is accepted through process-local store and returns placeholder clarification response with `200`. Missing session throws `AssistantSessionNotFoundException`, mapped to `404`. | Local Kotlin/Ktor route, process-local `InMemoryAssistantSessionStateStore`, local placeholder clarification reply. No external provider, no LLM call, no durable persistence. | Partial/minimal Stage 7 boundary. It depends on process-local session identity and has no runtime HTTP/schema validation gate yet. |

## 6. Product/security clarification needs

| Endpoint | Clarification needed | Reason |
|---|---|---|
| `POST /api/v1/assistant/sessions` | Clarify no-body vs body-with-initial-message behavior, treatment of malformed/empty JSON, current-session identity assumptions, whether anonymous current-session data may include travel preferences, and whether auth/security remains intentionally absent for this stage. | The endpoint can accept user travel text and creates session state. OpenAPI says current-session only, but manifest expansion should not imply account history, durable persistence, auth readiness, privacy review completion or generated-client readiness. |
| `POST /api/v1/assistant/sessions/{sessionId}/messages` | Clarify session ownership/identity, missing/expired session behavior, message validation boundaries, personal/travel preference data handling, and whether placeholder clarification reply is acceptable for a non-readiness candidate. | The endpoint mutates current-session state and carries user text. It must not imply production session persistence, account-level storage, provider/LLM behavior, auth/security completion or generated-client readiness. |

## 7. Candidate decision

| Endpoint | Decision | Reason | Readiness |
|---|---|---|---|
| `POST /api/v1/assistant/sessions` | `blocked_pending_contract_clarification` | Stage 7.35 marked it as a `future_candidate`, but optional request body semantics, runtime nullability, response optionality and current-session/security assumptions need explicit alignment notes before any manifest update. | `not_ready` |
| `POST /api/v1/assistant/sessions/{sessionId}/messages` | `blocked_pending_contract_clarification` | Stage 7.35 marked it as a `future_candidate`, but session lifecycle, `404` behavior, runtime validation and message/session data assumptions need explicit alignment notes before any manifest update. | `not_ready` |

## 8. Readiness non-claim

- Generated-client readiness не заявлена.
- Manifest не менялся.
- Endpoints не считаются ready.
- Generated clients не созданы.
- CI gate не включен.
- Backend не запускался.
- HTTP requests не выполнялись.

## 9. Validation

| Command | Result |
|---|---|
| `git status --short` | Passed before edits; clean working tree, no output. |
| Required `sed -n ...`, `rg ...` and `nl -ba ...` reads for AGENTS, README, roadmap docs, reviews index, Stage 7.33-7.35 reports, manifest, OpenAPI draft, backend assistant route files and relevant product/architecture baseline docs | Passed; required context reviewed. |
| `npm test` from `tools/openapi-conformance/` | Passed; 12 tests passed. |
| `./tools/openapi-conformance/check` | Passed with exit code `0`; report kept `status: "not_ready"`, `readinessClaim: false`, `manifestValidation.status: "advisory_passed"`, `blockingFindings: []`, 9 OpenAPI operations, 9 runtime routes, 3 `foundation_candidate`, 6 `placeholder_excluded`, 0 `runtime_only`, 0 `unclassified`. |
| `git diff --check` | Passed; no whitespace errors. |
| `git status --short` | Passed after edits; only expected Stage 7.36 docs changed before commit. |

Backend tests were not run because Stage 7.36 did not change backend code, backend runtime behavior, OpenAPI/API contracts or generated-client artifacts.

## 10. Recommended next step

Recommended next bounded Stage 7 task: **Stage 7.37 — Assistant Endpoint Contract/Runtime Alignment Notes**.

Reason: both assistant endpoint candidates remain plausible future candidates, but they need explicit contract/runtime/security/product alignment notes before a separate manifest update stage can safely decide whether to add them as non-readiness `future_candidate` / `not_ready` entries.
