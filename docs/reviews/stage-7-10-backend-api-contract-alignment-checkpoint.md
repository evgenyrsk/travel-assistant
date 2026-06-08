# Stage 7.10 — Backend API / Contract Alignment Checkpoint

## 1. Цель проверки

Проверить текущий Stage 7 backend foundation относительно Stage 6 OpenAPI / contract artifacts и зафиксировать API/contract alignment checkpoint перед возможной следующей Stage 7 задачей.

Проверка является review-only. Она не меняет backend behavior, public API, OpenAPI draft, generated clients, roadmap, product baseline или architecture baseline и не начинает Stage 7.11+.

## 2. Проверенные источники

- `AGENTS.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-8-internal-hotel-requirements-slot-metadata-boundary.md`
- `docs/reviews/stage-7-8-internal-hotel-requirements-slot-metadata-boundary-review.md`
- `docs/reviews/stage-7-9-internal-slot-coverage-clarification-planning-boundary.md`
- `docs/reviews/stage-7-9-internal-slot-coverage-clarification-planning-boundary-review.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `docs/architecture/stage-6/openapi-contract-review.md`
- `docs/architecture/stage-6/openapi-fixes-summary.md`
- `docs/architecture/stage-6/post-fix-contract-review.md`
- `docs/architecture/stage-6/provider-boundary-mapping-notes.md`
- `docs/architecture/stage-6/stage-6-completion-review.md`
- `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md`
- `docs/architecture/stage-6/stage-6-final-closure-and-handoff.md`
- `services/backend/build.gradle.kts`
- `services/backend/src/main/kotlin/**`
- `services/backend/src/test/kotlin/**`
- Current `git status --short`.

## 3. Проверенный backend scope

- `GET /api/v1/health`.
- `POST /api/v1/assistant/sessions`.
- `POST /api/v1/assistant/sessions/{sessionId}/messages`.
- Placeholder endpoints for hotel search, shortlist and explanations.
- Public DTOs in `AssistantPlaceholderRoutes.kt`, `HealthResponse.kt`, `ErrorResponse.kt` and placeholder responses.
- Structured error behavior for validation, unknown assistant session, unknown route, placeholder routes and internal errors.
- Internal assistant/session state objects and route tests that assert no state exposure.

## 4. Проверенный contract/API scope

- Stage 6.8 OpenAPI draft version `0.1.2-draft`.
- Stage 6 contract notes and final closure/handoff.
- Assistant session and assistant message schemas.
- Hotel search, hotel offers, shortlist and explanation endpoints.
- Shared error response components and error code enum.
- Current-session behavior, generated-client notes and provider boundary guardrails.

## 5. Итоговый verdict

Verdict: Pass for continued bounded Stage 7 backend foundation work, with Minor contract-alignment findings before generated clients or public API finalization.

The current backend is intentionally a foundation-only subset of the Stage 6 OpenAPI draft. Endpoint paths are broadly aligned, internal state remains internal, and the implemented behavior does not introduce requirements extraction, slot filling, dynamic clarification, durable persistence, provider integration, generated clients, frontend, booking, payment or flights.

The main gaps are not blockers for another bounded backend foundation slice, but they must be resolved or explicitly accepted before generated-client preparation, API contract finalization or real hotel-search behavior.

## 6. Findings by severity

### Critical

Нет.

### Major

Нет.

### Minor

#### MI-S7.10-001 — Assistant response shape is foundation-only and does not match Stage 6 `AssistantMessageResponse`

- **Classification:** should be fixed before generated clients.
- **Location:** `POST /api/v1/assistant/sessions`, `POST /api/v1/assistant/sessions/{sessionId}/messages`, `docs/architecture/stage-6/openapi-draft.yaml`.
- **Issue:** OpenAPI describes `AssistantMessageResponse` with nested `session`, `assistantMessage`, optional `nextAction` and optional `hotelSearchRequest`. Current backend returns narrow foundation DTOs: `AssistantSessionCreatedResponse` with `sessionId`, `status`, `createdAt`, and `AssistantMessageIntakeResponse` with `sessionId`, `status`, `receivedAt`, `assistantReply`.
- **Why it matters:** Generated clients created from the Stage 6 draft would not match the current backend payloads.
- **Recommendation:** Before generated clients or public API finalization, either align backend DTOs to OpenAPI or create an explicit OpenAPI/foundation contract cleanup that documents the temporary foundation shape.

#### MI-S7.10-002 — `POST /assistant/sessions` does not support the OpenAPI optional initial message behavior

- **Classification:** should be fixed before generated clients.
- **Location:** `POST /api/v1/assistant/sessions`, `AssistantPlaceholderRoutes.kt`, `openapi-draft.yaml`.
- **Issue:** OpenAPI allows an optional `AssistantMessageRequest` on session creation. Current backend ignores request body and only creates a process-local session.
- **Why it matters:** A generated client could send an initial message and expect assistant turn behavior that the foundation backend does not implement.
- **Recommendation:** Decide in a future API alignment task whether session creation should accept initial message behavior, or whether initial message intake must be performed only through `POST /assistant/sessions/{sessionId}/messages`.

#### MI-S7.10-003 — Foundation error payloads diverge from Stage 6 error schemas and enum

- **Classification:** should be fixed before generated clients.
- **Location:** `ErrorResponse.kt`, `respondValidationError`, `respondNotImplementedPlaceholder`, `configureErrorHandling`, `openapi-draft.yaml`.
- **Issue:** Stage 6 OpenAPI separates `ValidationErrorResponse` with `fields`, while the backend returns `ErrorResponse` with `details` for `VALIDATION_ERROR`. Backend also uses foundation-only `NOT_IMPLEMENTED` and generic `NOT_FOUND`, which are not in the Stage 6 `ErrorResponse.code` enum.
- **Why it matters:** Generated clients branching on the OpenAPI error schemas and enum would not model current foundation errors.
- **Recommendation:** Keep this as acceptable foundation behavior for now, but resolve the error schema/code alignment before generated-client preparation.

#### MI-S7.10-004 — Placeholder endpoints return `501 NOT_IMPLEMENTED` instead of Stage 6 success/error contract responses

- **Classification:** acceptable foundation-only deviation; should be resolved before real hotel-search behavior and generated clients.
- **Location:** `POST /hotel-searches`, `GET /hotel-searches/{searchId}/offers`, shortlist endpoints, explanation endpoint.
- **Issue:** Stage 6 draft defines success responses for hotel search, offers, shortlist and explanations. Current backend keeps these endpoints as placeholder `501 NOT_IMPLEMENTED` boundaries.
- **Why it matters:** This is safe for Stage 7 foundation, but generated clients or real hotel-search behavior cannot treat these endpoints as contract-ready implementation.
- **Recommendation:** Continue to document these endpoints as placeholders until the relevant behavior slices are explicitly activated. Do not generate clients against the current runtime behavior without a contract alignment plan.

### Notes

- `git status --short` before this checkpoint was clean, so Stage 7.9a was already committed.
- Implemented endpoint paths are consistent with the Stage 6 base path `/api/v1` and route naming.
- `GET /api/v1/health` aligns with the Stage 6 health contract for required fields. The OpenAPI optional `currentTime` is not returned by the backend and is not a blocker.
- `SESSION_NOT_FOUND` behavior on assistant message intake is aligned in code and concept, though the runtime payload uses the foundation `ErrorResponse` structure.
- `assistantReply` is safe as placeholder behavior because docs and tests keep it deterministic/static and do not present it as final `AssistantMessage` contract shape.
- `clarificationState`, `hotelRequirementsState` and `hotelRequirementsCoveragePlan` remain internal and are not leaked through public DTOs.
- Stage 7.10+ was not previously activated by roadmap; this checkpoint is the explicit Stage 7.10 review task and does not activate Stage 7.11+.

## 7. Implemented endpoint inventory

| Endpoint | Current behavior | Contract alignment classification |
|---|---|---|
| `GET /api/v1/health` | Returns `200` with `status`, `service`, `version`. | Mostly aligned; optional `currentTime` absent. |
| `POST /api/v1/assistant/sessions` | Returns `201` with process-local `sessionId`, `status`, `createdAt`. | Foundation-only deviation from OpenAPI `AssistantMessageResponse`; fix before generated clients. |
| `POST /api/v1/assistant/sessions/{sessionId}/messages` | Validates `message`, checks process-local session, updates internal metadata, returns static placeholder `assistantReply`. | Foundation-only deviation from OpenAPI `AssistantMessageResponse`; acceptable until API alignment/client generation. |
| `GET /api/v1/assistant/sessions/{sessionId}/shortlist` | Returns `501 NOT_IMPLEMENTED` placeholder. | Acceptable foundation-only placeholder; not contract-ready. |
| `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | Returns `501 NOT_IMPLEMENTED` placeholder. | Acceptable foundation-only placeholder; not contract-ready. |
| `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | Returns `501 NOT_IMPLEMENTED` placeholder. | Acceptable foundation-only placeholder; not contract-ready. |
| `POST /api/v1/assistant/sessions/{sessionId}/explanations` | Returns `501 NOT_IMPLEMENTED` placeholder. | Acceptable foundation-only placeholder; not contract-ready. |
| `POST /api/v1/hotel-searches` | Returns `501 NOT_IMPLEMENTED` placeholder. | Acceptable foundation-only placeholder; must be resolved before real hotel-search behavior. |
| `GET /api/v1/hotel-searches/{searchId}/offers` | Returns `501 NOT_IMPLEMENTED` placeholder. | Acceptable foundation-only placeholder; must be resolved before real hotel-search behavior. |
| Unknown route | Returns structured `404 NOT_FOUND`. | Foundation-only generic 404; not represented in Stage 6 error enum. |

## 8. Public response shape review

- Session creation response is intentionally narrow and does not expose OpenAPI `session`, `assistantMessage`, `nextAction` or `hotelSearchRequest`.
- Message intake response is intentionally narrow and exposes only `sessionId`, `status`, `receivedAt` and `assistantReply`.
- `assistantReply` is a deterministic placeholder with `replyType` and `message`; it is not equivalent to Stage 6 `AssistantMessage`.
- Successful responses do not expose `clarificationState`, `hotelRequirementsState`, `hotelRequirementsCoveragePlan`, slot keys, slot statuses or coverage metadata.
- Placeholder responses use `ErrorResponse` with `code = NOT_IMPLEMENTED`, message and `details.boundary`.
- Public shape is safe for current foundation tests, but not ready as generated-client contract output.

## 9. Error behavior / error code review

- `VALIDATION_ERROR` is returned for missing body, missing `message` and blank `message` on assistant message intake.
- `SESSION_NOT_FOUND` is returned for unknown process-local assistant session during message intake.
- `NOT_IMPLEMENTED` is returned for placeholder hotel search, shortlist and explanation boundaries.
- Generic unknown routes return `NOT_FOUND`.
- Generic uncaught exceptions return `INTERNAL_ERROR`.
- Runtime error payloads include optional `requestId` from `X-Request-ID`.
- Runtime validation errors use `details.field` and `details.message`, while Stage 6 OpenAPI expects `fields`.
- `NOT_IMPLEMENTED` and generic `NOT_FOUND` are foundation-level runtime codes and are not part of the Stage 6 draft error enum.

## 10. OpenAPI / contract alignment review

- Base path `/api/v1` matches the OpenAPI server URL.
- Endpoint path names match the Stage 6 draft for health, assistant sessions, assistant messages, hotel searches, offers, shortlist and explanations.
- The Stage 6 draft is a full client-facing hotel-only MVP contract; current Stage 7 runtime is a minimal backend foundation subset.
- `GET /health` is close to the contract and returns the required fields.
- Assistant session creation and message intake do not match the draft response schema and should not be treated as contract-complete.
- Current assistant session creation does not implement optional initial message behavior from the draft.
- Hotel search, offers, shortlist and explanation endpoints exist as placeholders only and do not implement the draft success responses.
- Current runtime error model is useful as foundation behavior, but not aligned enough for generated-client work.
- No OpenAPI draft changes were made in this task.

## 11. Internal state exposure review

- `clarificationState` remains inside domain/application state and use-case results; it is not serialized by route DTOs.
- `hotelRequirementsState` remains inside process-local session snapshots and use-case results; it is not serialized by route DTOs.
- `hotelRequirementsCoveragePlan` remains inside process-local session snapshots and use-case results; it is not serialized by route DTOs.
- Route tests assert absence of `hotelRequirementsState`, `hotelRequirementsCoveragePlan`, `slotCoveragePlan`, `requirementsState` and `slots`.
- No public DTO exposes internal slot metadata, coverage metadata, accepted message counts, timestamps beyond public `createdAt`/`receivedAt`, or process-local store details.

## 12. Placeholder/foundation-only behavior review

- `assistantReply` remains static and deterministic.
- Valid message intake does not parse or analyze message text.
- Valid message intake does not extract, infer, fill or store slot values.
- Valid message intake recomputes internal coverage metadata from existing `hotelRequirementsState`.
- Process-local session state is injectable/testable and not durable persistence.
- Placeholder endpoints do not call providers, DB, Redis, LLM, cache, queue or generated clients.
- No frontend, OpenAPI generation, provider integration, hotel search behavior, ranking, shortlist behavior, explanation/comparison behavior, booking, payment or flights were introduced.

## 13. Documentation/roadmap review

- `README.md` and `docs/ROADMAP.md` accurately describe Stage 7.9 completion and Stage 7.10+ non-activation before this task.
- `docs/roadmap/roadmap.md` keeps Stage 7 in progress / awaiting explicit next task and does not start Stage 7.10+ before the explicit task.
- `services/backend/README.md` accurately documents the current foundation endpoints, process-local metadata, placeholder routes and non-implemented scope.
- `docs/reviews/README.md` indexes Stage 7 implementation reports through Stage 7.9 but does not yet list this new Stage 7.10 checkpoint because this task is allowed to create only the required report.
- Product baseline and architecture baseline contain older Stage 7.0f-era status wording, but primary roadmap and navigation docs carry current status; this review did not rewrite baselines.
- No documentation/governance drift requiring immediate correction was found.

## 14. Risks before generated clients

- Generated clients from Stage 6 OpenAPI would expect `AssistantMessageResponse`, but runtime assistant endpoints return foundation DTOs.
- Generated clients would expect `ValidationErrorResponse.fields`, but runtime validation errors use `ErrorResponse.details`.
- Generated clients would not model foundation-only `NOT_IMPLEMENTED` and generic `NOT_FOUND` codes from the current runtime.
- Generated clients would expect hotel search, offer, shortlist and explanation success schemas, while runtime returns placeholder `501`.
- The optional initial message behavior on session creation is not implemented.
- Before generated clients, the project needs either API runtime alignment to OpenAPI or an explicit temporary foundation contract/documented deviation strategy.

## 15. Risks before real hotel-search behavior

- `POST /hotel-searches` currently has no validation, no criteria model, no session linkage and no `HotelSearchResponse`.
- `GET /hotel-searches/{searchId}/offers` currently has no result envelope, no search state, no offers, no provider facts, no assumptions and no unknown data.
- Placeholder `NOT_IMPLEMENTED` must be replaced by contract-aligned behavior before real hotel search is treated as active.
- Provider/API mapping is still deferred until the provider contract is available and explicitly activated.
- Error taxonomy for real search/search-not-found/provider failure must align with Stage 6 draft before real hotel-search behavior.

## 16. Что не проверялось

- Runtime behavior against a generated TypeScript/OpenAPI client.
- Full OpenAPI schema validation or code generation.
- Provider/API mapping against a real provider contract.
- Durable DB/storage behavior, migrations, repositories or schema design.
- Redis/cache behavior.
- Session retrieval/listing endpoints.
- Message history, resume behavior or account-level ownership.
- Real stateful clarification flow.
- Requirements extraction, slot filling or intent classification.
- Dynamic assistant replies.
- LLM provider integration, prompt quality or orchestration behavior.
- Hotel provider integration.
- Hotel search, ranking, shortlist, explanations and comparison behavior.
- Frontend implementation.
- Booking, payment, flights and combined itinerary.
- Production hardening, observability, deployment and Docker.

## 17. Проверки

- `git status --short` — passed, worktree был чистым перед report creation.
- `git diff --check` — passed before report creation.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed before report creation.
- `git diff --check` — passed after report creation.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed after report creation.

## 18. Рекомендации

- Do not generate clients from Stage 6 OpenAPI against the current foundation runtime without a separate alignment task.
- Treat current `assistantReply`, `NOT_IMPLEMENTED`, generic `NOT_FOUND` and narrow assistant DTOs as foundation-level behavior only.
- Before generated-client preparation, run a bounded OpenAPI/runtime alignment cleanup or explicitly document temporary foundation deviations.
- Before real hotel-search behavior, replace placeholder `501` search endpoints with contract-aligned validation, response envelopes and error handling through a separate roadmap-aligned task.
- Keep internal state objects out of public DTOs unless a future contract task explicitly defines a user-facing state summary.

## 19. Recommended next task

Recommended next task: a bounded Stage 7 OpenAPI/runtime alignment cleanup for assistant response/error foundation shapes before generated-client preparation.

Alternative acceptable next task: a controlled internal slot update boundary, if it remains internal, does not expose new public API fields and does not start generated clients, real hotel search, dynamic clarification, LLM, provider integration or storage.

## 20. Scope control confirmation

- Review-only checkpoint completed.
- Backend code, tests, OpenAPI draft, roadmap, README, product baseline and architecture baseline were not changed.
- Stage 7.11+ was not started.
- No new endpoints, public API behavior, generated clients, DB/storage, Redis, message history, requirements extraction, slot filling, dynamic clarification, LLM orchestration, provider integration, frontend, booking, payment or flights were added.
- Broad documentation cleanup was not performed.
