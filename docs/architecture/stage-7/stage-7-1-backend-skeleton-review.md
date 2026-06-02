# Stage 7.1 — Backend Skeleton Scope Audit

## 1. Review scope

Review checked Stage 7.1 backend skeleton against the explicit Stage 7.1 task scope, Stage 6 OpenAPI draft, provider boundary guardrails and roadmap activation constraints.

This review did not start Stage 7.2, did not activate Stage 8+, did not modify backend source and did not expand MVP scope.

## 2. Inputs reviewed

- `AGENTS.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/architecture/README.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/provider-boundary-mapping-notes.md`
- `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md`
- `docs/architecture/stage-6/stage-6-final-closure-and-handoff.md`
- `services/backend/README.md`
- `services/backend/build.gradle.kts`
- `services/backend/settings.gradle.kts`
- `services/backend/src/`

## 3. Summary verdict

**Verdict:** Passed with minor notes.

Stage 7.1 backend skeleton is aligned with the allowed Stage 7.1 scope. It provides a minimal Spring Boot Java 17 backend foundation, OpenAPI-shaped endpoint skeletons, client-facing DTO/error skeletons and a health smoke test.

No Critical or Major blockers were found. Stage 7.2 may be started only through a separate explicit roadmap task.

## 4. Findings table

| ID | Severity | File/path | Description | Why it matters | Recommended fix | Blocks Stage 7.2 |
|---|---|---|---|---|---|---|
| S7.1-CR-001 | Minor | `services/backend/src/main/java/com/travelassistant/backend/api/model/ApiModels.java` | `ErrorCode` is reused by both `ErrorResponse` and `ValidationErrorResponse`, while Stage 6 OpenAPI models `VALIDATION_ERROR` only under `ValidationErrorResponse` and resource/internal codes under `ErrorResponse`. | This is acceptable for a skeleton, but future client-facing error handling may benefit from tighter schema separation before richer endpoint behavior. | Before richer error handling or generated clients, consider splitting validation and generic error code enums or documenting the intentional shared enum. | No |
| S7.1-CR-002 | Minor | `services/backend/src/main/java/com/travelassistant/backend/api/*Controller.java` | Skeleton endpoints return placeholder responses and do not implement not-found branching for session/search/offer/shortlist resources. | Stage 7.1 explicitly avoids storage and business logic, so this is expected now. Stage 7.2+ should avoid treating current placeholder success paths as real behavior. | In the next implementation task that introduces state/use cases, add resource lookup boundaries and map missing resources to the Stage 6 shared 404 `ErrorResponse` codes. | No |
| S7.1-CR-003 | Notes | `services/backend/src/main/java/com/travelassistant/backend/api/AssistantExplanationController.java` | `sessionId` is accepted as a path variable and not used in the placeholder response. | This matches skeleton-only behavior and keeps the OpenAPI route shape, but it should not be mistaken for session-scoped explanation logic. | Use `sessionId` when Stage 7 explicitly adds session-aware application logic. | No |
| S7.1-CR-004 | Notes | `services/backend/README.md` | README accurately states skeleton-only limitations and lists explanation/comparison through the single OpenAPI endpoint with `mode`. | Confirms no unsupported comparison API surface was added. | No action needed. | No |

## 5. OpenAPI alignment notes

Implemented skeleton routes match Stage 6 OpenAPI route and method shape:

- `GET /api/v1/health`
- `POST /api/v1/assistant/sessions`
- `POST /api/v1/assistant/sessions/{sessionId}/messages`
- `POST /api/v1/hotel-searches`
- `GET /api/v1/hotel-searches/{searchId}/offers`
- `GET /api/v1/assistant/sessions/{sessionId}/shortlist`
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`
- `POST /api/v1/assistant/sessions/{sessionId}/explanations`

No extra non-OpenAPI business routes were found.

Explanation and comparison handling follows Stage 6 by using `AssistantExplanationRequest.mode` with `explain` or `compare`; no separate comparison endpoint was introduced.

DTO skeleton names are client-facing and OpenAPI-aligned enough for Stage 7.1. `ProviderFact`, `providerOfferRef`, freshness and provider-state fields mirror Stage 6 client-facing concepts and do not introduce provider-specific DTO/contracts.

## 6. Scope boundary verification

Confirmed absent from tracked Stage 7.1 source:

- real hotel provider integration;
- provider-specific DTOs/contracts;
- DB migrations;
- JPA entities;
- repositories;
- Redis/cache implementation;
- LLM integration/orchestration;
- frontend code;
- generated clients;
- booking/payment/flights/account/combined itinerary flows;
- real search business logic.

Ignored Gradle build outputs were created by verification under `services/backend/build/` and `services/backend/.gradle/`; they are not tracked source artifacts.

## 7. Build/test results

Commands run:

```bash
cd services/backend
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home GRADLE_USER_HOME=/private/tmp/travel-assistant-gradle ./gradlew test --no-daemon
```

Result: passed. `BUILD SUCCESSFUL`; `HealthControllerTest` was up to date after the previous successful run.

```bash
git diff --check
```

Result: passed with no output.

Manual checks:

- no trailing whitespace reported by `git diff --check`;
- no forbidden source folders/files found outside ignored build output;
- no generated clients found;
- no unrelated source files were modified by this review.

## 8. Roadmap/status verification

Verified:

- `docs/roadmap/roadmap.md` marks Stage 7 as `In progress`.
- Stage 7.1 is recorded as completed by explicit roadmap task.
- Stage 7.2+ are not marked completed and require separate explicit tasks.
- Stage 8, Stage 9 and Stage 10 remain `Planned`.
- Roadmap order remains Stage 0 through Stage 10.
- `docs/ROADMAP.md`, root `README.md` and `docs/architecture/README.md` are consistent with Stage 7.1-only activation.

## 9. Recommendations, not executed

- Before richer endpoint behavior or generated clients, tighten or document the shared error-code enum shape.
- In the next explicit Stage 7 implementation task, introduce resource lookup/use-case boundaries before returning real 404 responses.
- Keep any cleanup of stale Ktor/status wording in secondary future-reference docs as a separate documentation cleanup task, not part of this review.

## 10. Final decision on whether Stage 7.2 may be started

Stage 7.2 may be started only through a separate explicit roadmap task.

This review found no Critical or Major blockers that would prevent a properly scoped Stage 7.2 task from being proposed.

