# Stage 7.35 — Endpoint Candidate Review

## 1. Scope

- [x] Review-only endpoint candidate analysis
- [x] No manifest changes
- [x] No generated-client readiness claim
- [x] No OpenAPI/API contract changes
- [x] No backend/frontend runtime changes
- [x] No generated clients
- [x] No CI/Gradle integration
- [x] No Stage 8 activation

## 2. Executive summary

Stage 7.35 reviewed current OpenAPI endpoints, static backend routes and the existing `generated-client-ready-subset.yaml` baseline without changing the manifest.

Current inventory has 9 OpenAPI operations and 9 matching backend routes. `tools/openapi-conformance` reports 3 `foundation_candidate` endpoints and 6 `placeholder_excluded` endpoints, with no OpenAPI-only, runtime-only or unclassified endpoints.

Only `GET /api/v1/health` is already included in the manifest as a `foundation_candidate`, and it remains `not_ready`. `POST /api/v1/assistant/sessions` and `POST /api/v1/assistant/sessions/{sessionId}/messages` are reasonable future candidates, but they require explicit clarification before any manifest update because they carry session/message semantics and have not gone through runtime contract/schema validation.

Generated-client readiness is not claimed.

## 3. Current manifest baseline

| Field/section | Current value | Notes |
|---|---|---|
| Manifest path | `docs/architecture/stage-7/generated-client-ready-subset.yaml` | Reviewed read-only; not changed in Stage 7.35. |
| Top-level `status` | `not_ready` | Candidate baseline only, not a readiness declaration. |
| Top-level `readinessClaim` | `false` | Explicit non-claim. |
| `validationStatus.status` | `not_ready` | Tool-facing non-readiness state. |
| `validationStatus.readinessClaim` | `false` | Tool-facing non-claim. |
| `includedEndpoints` | `GET /api/v1/health` as `foundation_candidate`, `readiness: "not_ready"` | Already included as the smallest foundation candidate. |
| `excludedEndpoints` | 6 endpoints: hotel search, offers, shortlist read/upsert/delete and explanation | All remain `placeholder_excluded`, `readiness: "not_ready"`. |
| `readinessCriteria` | All criteria are `false` | Generated-client target, generation, compile, runtime contract and schema validation gates have not passed. |
| `knownLimitations` | Static inventory, future-only endpoint reference validation, missing generated-client target and missing runtime contract checks block readiness | These are blocking before readiness, not task failures. |
| `generatedClientTargets` | Empty list | No generated-client target is declared or configured. |

## 4. OpenAPI endpoint inventory

| Method | Path | Purpose | Complexity | Candidate note |
|---|---|---|---|---|
| GET | `/api/v1/health` | Backend availability check. | Low: no request body; simple 200/500 response. | `foundation_candidate`; already in manifest, still `not_ready`. |
| POST | `/api/v1/assistant/sessions` | Create current assistant session, optionally with initial message. | Medium: optional request body, validation error, session/message response. | `future_candidate`; possible manifest expansion candidate after clarification. |
| POST | `/api/v1/assistant/sessions/{sessionId}/messages` | Continue assistant session with user message. | Medium: path parameter, required body, 200/400/404/500 paths. | `future_candidate`; needs session lifecycle and runtime contract validation. |
| POST | `/api/v1/hotel-searches` | Create hotel search request. | High: required hotel criteria body and provider-boundary semantics. | `excluded_placeholder`; real hotel search behavior not implemented. |
| GET | `/api/v1/hotel-searches/{searchId}/offers` | Return hotel offers/search state. | High: provider facts, result envelope and terminal search states. | `excluded_placeholder`; offers/provider behavior not implemented. |
| GET | `/api/v1/assistant/sessions/{sessionId}/shortlist` | Read current-session shortlist. | Medium: session-bound user data, 200/404/500 paths. | `excluded_placeholder`; current backend route is not implemented. |
| PUT | `/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | Add or update shortlist item. | Medium/high: state mutation, path params, optional body. | `excluded_placeholder`; current backend route is not implemented. |
| DELETE | `/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | Remove shortlist item. | Medium: state mutation and 204/404 behavior. | `excluded_placeholder`; current backend route is not implemented. |
| POST | `/api/v1/assistant/sessions/{sessionId}/explanations` | Explain or compare hotel offers. | High: assistant text grounded in constraints/provider facts. | `excluded_placeholder`; explanation behavior not implemented. |

OpenAPI does not declare auth/security schemes in the current draft. This does not make session/user-data endpoints ready; it means security/auth implications remain future clarification before any readiness stage.

## 5. Backend route inventory

| Method | Path | Backend evidence | OpenAPI evidence | Alignment |
|---|---|---|---|---|
| GET | `/api/v1/health` | `HealthRoutes.kt:9` | `openapi-draft.yaml:22` / `operationId: getHealth` | Exists in both; minimal health response. |
| POST | `/api/v1/assistant/sessions` | `AssistantPlaceholderRoutes.kt:26` | `openapi-draft.yaml:38` / `operationId: createAssistantSession` | Exists in both; implemented local session boundary, not runtime-contract validated. |
| POST | `/api/v1/assistant/sessions/{sessionId}/messages` | `AssistantPlaceholderRoutes.kt:59` | `openapi-draft.yaml:66` / `operationId: continueAssistantSession` | Exists in both; implemented local message boundary, not runtime-contract validated. |
| POST | `/api/v1/hotel-searches` | `HotelSearchPlaceholderRoutes.kt:11` | `openapi-draft.yaml:98` / `operationId: createHotelSearch` | Exists in both, but backend responds with `NOT_IMPLEMENTED` placeholder. |
| GET | `/api/v1/hotel-searches/{searchId}/offers` | `HotelSearchPlaceholderRoutes.kt:15` | `openapi-draft.yaml:129` / `operationId: getHotelOffers` | Exists in both, but backend responds with `NOT_IMPLEMENTED` placeholder. |
| GET | `/api/v1/assistant/sessions/{sessionId}/shortlist` | `AssistantPlaceholderRoutes.kt:86` | `openapi-draft.yaml:155` / `operationId: getSessionShortlist` | Exists in both, but backend responds with `NOT_IMPLEMENTED` placeholder. |
| PUT | `/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | `AssistantPlaceholderRoutes.kt:90` | `openapi-draft.yaml:179` / `operationId: upsertShortlistItem` | Exists in both, but backend responds with `NOT_IMPLEMENTED` placeholder. |
| DELETE | `/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | `AssistantPlaceholderRoutes.kt:94` | `openapi-draft.yaml:179` / `operationId: removeShortlistItem` | Exists in both, but backend responds with `NOT_IMPLEMENTED` placeholder. |
| POST | `/api/v1/assistant/sessions/{sessionId}/explanations` | `AssistantPlaceholderRoutes.kt:98` | `openapi-draft.yaml:223` / `operationId: createAssistantExplanation` | Exists in both, but backend responds with `NOT_IMPLEMENTED` placeholder. |

Static inventory found no backend-only or OpenAPI-only routes in the current tool report.

## 6. Candidate classification

| Method | Path | Classification | Reason | Readiness |
|---|---|---|---|---|
| GET | `/api/v1/health` | `foundation_candidate` | Lowest-risk foundation endpoint; already in manifest; still lacks runtime HTTP/schema/generated-client validation. | `not_ready` |
| POST | `/api/v1/assistant/sessions` | `future_candidate` | Non-placeholder route exists in both inventories, but session response semantics and optional request behavior need explicit runtime contract validation before manifest expansion. | `not_ready` |
| POST | `/api/v1/assistant/sessions/{sessionId}/messages` | `future_candidate` | Non-placeholder route exists in both inventories, but existing-session lifecycle, 404 behavior and message response semantics need clarification/validation. | `not_ready` |
| POST | `/api/v1/hotel-searches` | `excluded_placeholder` | Current backend route is a 501 placeholder and real hotel search/provider behavior is not implemented. | `not_ready` |
| GET | `/api/v1/hotel-searches/{searchId}/offers` | `excluded_placeholder` | Current backend route is a 501 placeholder and offers/provider facts are not implemented. | `not_ready` |
| GET | `/api/v1/assistant/sessions/{sessionId}/shortlist` | `excluded_placeholder` | Current backend route is a 501 placeholder; shortlist persistence/behavior is not implemented. | `not_ready` |
| PUT | `/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | `excluded_placeholder` | Current backend route is a 501 placeholder; shortlist mutation behavior is not implemented. | `not_ready` |
| DELETE | `/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | `excluded_placeholder` | Current backend route is a 501 placeholder; shortlist mutation behavior is not implemented. | `not_ready` |
| POST | `/api/v1/assistant/sessions/{sessionId}/explanations` | `excluded_placeholder` | Current backend route is a 501 placeholder; explanation/LLM/provider-fact grounding is not implemented. | `not_ready` |

## 7. Exclusions

| Method | Path | Reason excluded |
|---|---|---|
| POST | `/api/v1/hotel-searches` | 501 placeholder; real hotel search behavior, runtime contract checks and generated-client compile checks are absent. |
| GET | `/api/v1/hotel-searches/{searchId}/offers` | 501 placeholder; real offers/provider behavior, runtime contract checks and generated-client compile checks are absent. |
| GET | `/api/v1/assistant/sessions/{sessionId}/shortlist` | 501 placeholder; shortlist behavior is not implemented. |
| PUT | `/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | 501 placeholder; shortlist mutation behavior is not implemented. |
| DELETE | `/api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` | 501 placeholder; shortlist mutation behavior is not implemented. |
| POST | `/api/v1/assistant/sessions/{sessionId}/explanations` | 501 placeholder; explanation behavior and provider-fact grounding are not implemented. |

## 8. Risks and guardrails

| Risk | Guardrail |
|---|---|
| A route can exist in both OpenAPI and backend but still be a placeholder. | Keep all placeholder routes excluded until real behavior and contract validation are implemented in a separate stage. |
| Assistant POST endpoints look like foundation candidates but carry session/message semantics. | Treat them as `future_candidate` only; require explicit clarification before any manifest update. |
| Health looks simple but still lacks runtime HTTP/schema/generated-client validation. | Keep health as `not_ready`; do not promote readiness from static inventory or manifest presence. |
| No OpenAPI security scheme may be misread as no security concern. | Treat session/user-data endpoints as requiring future security/product clarification before readiness. |
| Generated-client readiness could be inferred from a populated manifest. | Preserve `status: "not_ready"`, `readinessClaim: false`, no generated targets and no ready endpoint entries. |
| Provider/offer/explanation endpoints can create false confidence because schemas exist. | Keep provider-backed and explanation endpoints excluded until real provider/LLM behavior is implemented and validated. |

## 9. Readiness non-claim

- Generated-client readiness is not claimed.
- Manifest did not change.
- Endpoints are not considered ready.
- Generated clients are not created.
- CI gate is not enabled.
- Backend was not started.
- HTTP requests were not executed.

## 10. Validation

| Command | Result |
|---|---|
| `git status --short` | Passed before edits; clean working tree, no output. |
| Required `sed -n ...`, `rg ...` and `nl -ba ...` reads for AGENTS, README, roadmap docs, reviews index, Stage 7.25/7.32/7.33/7.34 reports, manifest, OpenAPI draft, backend route files and conformance tool source | Passed; required context reviewed. |
| `npm test` from `tools/openapi-conformance/` | Passed; 12 tests passed. |
| `./tools/openapi-conformance/check` | Passed with exit code `0`; report kept `status: "not_ready"`, `readinessClaim: false`, `manifestValidation.status: "advisory_passed"`, `blockingFindings: []`, 9 OpenAPI operations, 9 runtime routes, 3 `foundation_candidate`, 6 `placeholder_excluded`, 0 `runtime_only`, 0 `unclassified`. |
| `git diff --check` | Passed; no whitespace errors. |
| `git status --short` | Passed after edits; only expected Stage 7.35 docs changed before commit. |

Backend tests were not run because Stage 7.35 does not change backend code, backend runtime behavior, OpenAPI/API contracts or generated-client artifacts.

## 11. Recommended next step

Recommended next bounded Stage 7 task: **Stage 7.36 — Endpoint Candidate Clarification**.

Reason: review found more than one potential future candidate beyond health. Before any manifest endpoint update, the project should explicitly clarify whether `POST /api/v1/assistant/sessions` and `POST /api/v1/assistant/sessions/{sessionId}/messages` belong in the next non-readiness manifest expansion and which runtime contract/schema checks are required.
