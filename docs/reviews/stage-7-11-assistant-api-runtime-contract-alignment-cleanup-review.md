# Stage 7.11a — Assistant API Runtime Contract Alignment Cleanup Review / Quality Gate

## 1. Цель проверки

Проверить Stage 7.11 Assistant API Runtime Contract Alignment Cleanup как review-only quality gate перед возможной следующей bounded Stage 7 backend задачей.

Проверка не является feature implementation task, не меняет runtime behavior, public API, OpenAPI draft, generated clients, roadmap order, product baseline или architecture baseline и не начинает Stage 7.12+.

## 2. Проверенные источники

- `AGENTS.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-10-backend-api-contract-alignment-checkpoint.md`
- `docs/reviews/stage-7-11-assistant-api-runtime-contract-alignment-cleanup.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md`
- `docs/architecture/stage-6/stage-6-final-closure-and-handoff.md`
- `services/backend/src/main/kotlin/**`
- `services/backend/src/test/kotlin/**`
- `git show` для Stage 7.11 commit `c7a066a`.
- Current `git status --short`.

## 3. Проверенный backend scope

- `POST /api/v1/assistant/sessions`.
- `POST /api/v1/assistant/sessions/{sessionId}/messages`.
- Assistant success response DTOs.
- Optional initial message handling.
- Validation error response DTO.
- `SESSION_NOT_FOUND`, `NOT_IMPLEMENTED`, generic `NOT_FOUND` and `INTERNAL_ERROR` behavior.
- Placeholder routes for hotel search, shortlist and explanations.
- Route tests for assistant sessions, validation, optional initial message and placeholders.
- Application/domain boundaries around `AssistantSessionBoundary`, `AssistantSession`, local session store and internal metadata.

## 4. Проверенный contract/API scope

- Stage 6 `AssistantMessageRequest`.
- Stage 6 `AssistantMessageResponse`.
- Stage 6 `AssistantSession`.
- Stage 6 `AssistantMessage`.
- Stage 6 `ValidationErrorResponse`.
- Stage 6 notes around current-session behavior, generated-client caution and client-facing contract direction.
- Stage 7.10 findings and Stage 7.11 addressed/deferred finding claims.

## 5. Итоговый verdict

Verdict: Pass with Notes.

Stage 7.11 safely aligns assistant runtime response shape closer to Stage 6 direction while preserving foundation-only behavior. It does not expose internal state, does not implement real assistant behavior, does not analyze or store optional initial message text as history, and leaves hotel search, shortlist and explanations as explicit placeholders.

No Critical, Major or Minor blockers were found.

## 6. Findings by severity

### Critical

Нет.

### Major

Нет.

### Minor

Нет.

### Notes

- `git status --short` before review was clean, so Stage 7.11 changes were already committed before this quality gate.
- Assistant success responses are closer to Stage 6 `AssistantMessageResponse`, but still intentionally omit `hotelSearchRequest` and full generated-client-ready semantics.
- `nextAction = ask_clarification` is static foundation metadata, not dynamic clarification planning.
- Optional initial message is accepted as bounded foundation intake, but the use case still does not read message text for extraction or slot filling.
- `NOT_IMPLEMENTED` and generic `NOT_FOUND` remain foundation-only runtime codes and are correctly documented as deferred generated-client/API alignment work.
- Code comment wording in `AssistantSessionBoundary.kt` still says `Stage 7.3-7.9 boundary`; this is stale documentation inside code, but not a behavior or architecture blocker.

## 7. Addressed Stage 7.10 findings review

### MI-S7.10-001 — Assistant response shape

Status: safely addressed for assistant foundation runtime.

Session creation and message intake now return a nested response with:

- `session`;
- `assistantMessage`;
- `nextAction`.

This aligns the public shape closer to Stage 6 without exposing internal process-local state or claiming final generated-client readiness.

### MI-S7.10-002 — Optional initial message

Status: safely addressed as bounded foundation intake.

`POST /api/v1/assistant/sessions` accepts optional `message`. When present and non-blank, it creates a session and immediately routes the message through the existing local intake path. The result remains a static placeholder assistant message.

The implementation does not introduce message history, requirements extraction, slot filling, dynamic clarification, LLM orchestration, provider calls or durable persistence.

### MI-S7.10-003 — Validation error fields

Status: partially and safely addressed.

Validation errors now return `fields`, matching the Stage 6 direction for `ValidationErrorResponse`. The change is limited and does not redesign the full error taxonomy.

## 8. Deferred Stage 7.10 findings review

### MI-S7.10-004 — Placeholder `501 NOT_IMPLEMENTED`

Status: correctly deferred.

Hotel search, shortlist and explanation endpoints still return `501 NOT_IMPLEMENTED`. This is appropriate because Stage 7.11 does not activate hotel search, ranking, shortlist behavior, explanations/comparison, provider integration or LLM behavior.

### Remaining error taxonomy alignment

Status: correctly deferred.

Foundation-only `NOT_IMPLEMENTED` and generic `NOT_FOUND` remain documented as non-final runtime codes. They should be revisited only when placeholder endpoints are replaced or generated-client preparation is explicitly activated.

## 9. Endpoint behavior review

- `POST /api/v1/assistant/sessions` returns `201 Created` with `session`, `assistantMessage` and `nextAction`.
- `POST /api/v1/assistant/sessions` accepts absent body, valid optional `message` and optional `clientContext`.
- Blank optional initial `message` returns `400 VALIDATION_ERROR`.
- `POST /api/v1/assistant/sessions/{sessionId}/messages` returns `200 OK` with the same success response shape.
- Unknown assistant session still returns structured `404 SESSION_NOT_FOUND`.
- Missing or blank message intake body still returns structured `400 VALIDATION_ERROR`.
- Hotel search, shortlist and explanation endpoints still return structured `501 NOT_IMPLEMENTED`.
- Unknown route still returns structured generic `404 NOT_FOUND`.

## 10. Assistant response shape review

- `session.sessionId` remains an opaque process-local id.
- `session.status` remains `collecting_requirements`.
- `session.createdAt` and `session.updatedAt` are public timestamps only, not durable persistence guarantees.
- `assistantMessage.role` is `assistant`.
- `assistantMessage.content` remains deterministic/static placeholder copy.
- `nextAction` is static `ask_clarification`.
- `hotelSearchRequest` is omitted because search readiness is not implemented.
- `assistantReply` is no longer exposed in public route responses.
- Response shape does not expose slot metadata, coverage metadata or clarification state.

## 11. Optional initial message review

- Optional initial message is parsed only from `AssistantMessageRequest.message`.
- Non-blank optional initial message routes through `acceptUserMessage`.
- Message text is passed through the command boundary but is not read for extraction, inference, dynamic reply selection, slot filling or hotel search.
- Message text is not stored as conversation history.
- The resulting session remains process-local.
- Internal clarification metadata is updated, but this metadata is not exposed publicly.
- Follow-up message intake works for the same process-local session.
- Blank initial message is validated with `VALIDATION_ERROR`.

## 12. Error behavior review

- `VALIDATION_ERROR` uses a `fields` array with field-level messages.
- Missing body, missing `message`, blank message and blank initial message are covered by route tests.
- `SESSION_NOT_FOUND` response remains structured and includes `details.sessionId`.
- Placeholder `NOT_IMPLEMENTED` response remains structured and includes `details.boundary`.
- Generic `NOT_FOUND` remains structured for unknown routes.
- `INTERNAL_ERROR` remains the catch-all internal error response.
- No broad error taxonomy redesign was introduced.

## 13. Placeholder behavior review

- `POST /api/v1/hotel-searches` remains a placeholder.
- `GET /api/v1/hotel-searches/{searchId}/offers` remains a placeholder.
- `GET /api/v1/assistant/sessions/{sessionId}/shortlist` remains a placeholder.
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` remains a placeholder.
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}` remains a placeholder.
- `POST /api/v1/assistant/sessions/{sessionId}/explanations` remains a placeholder.
- No real hotel search, shortlist, explanation/comparison, provider call, LLM call, DB/storage or generated-client behavior was introduced.

## 14. Internal state exposure review

- `clarificationState` is not serialized in public success responses.
- `hotelRequirementsState` is not serialized in public success responses.
- `hotelRequirementsCoveragePlan` is not serialized in public success responses.
- Route tests assert absence of internal state fields in session creation and message intake responses.
- Public response fields stay limited to Stage 6-like assistant response shape.
- Internal process-local session store details are not exposed.

## 15. Application/domain boundary review

- Ktor routing remains in `AssistantPlaceholderRoutes.kt`.
- Domain/application classes do not import Ktor.
- The use case remains responsible for local session creation and accepted message behavior.
- `AssistantSessionStateStore` remains process-local and injectable.
- No DB, Redis, provider SDK, LLM SDK, frontend tooling or generated-client tooling dependency was added.
- Backend stack remains Kotlin + Ktor.

## 16. Test coverage review

- Tests cover session creation response shape.
- Tests cover message intake response shape.
- Tests cover optional initial message behavior and follow-up message intake.
- Tests cover blank optional initial message validation.
- Tests cover missing body, missing message and blank message validation.
- Tests cover `SESSION_NOT_FOUND`.
- Tests cover internal state not being exposed in public success responses.
- Existing placeholder and health tests remain in place.
- Tests use Ktor `testApplication` and do not require DB, Redis, network, LLM, provider SDKs, frontend tooling or generated clients.

## 17. Documentation/roadmap review

- `services/backend/README.md` accurately describes Stage 7.11 assistant response shape, optional initial message behavior, validation `fields`, internal metadata and placeholder boundaries.
- `docs/roadmap/roadmap.md` marks Stage 7.11 completed and keeps Stage 7.12+ unactivated.
- `docs/reviews/README.md` indexes Stage 7.10 and Stage 7.11 artifacts without turning them into active backlog.
- `docs/reviews/stage-7-11-assistant-api-runtime-contract-alignment-cleanup.md` accurately describes implemented behavior, exclusions, checks and known limitations.
- OpenAPI draft, product baseline and architecture baseline were not changed.
- No broad documentation cleanup was mixed into Stage 7.11.

## 18. Risks before generated clients

- Assistant response shape is closer to Stage 6 but still omits `hotelSearchRequest`.
- `nextAction` is static and should not be treated as dynamic orchestration.
- Runtime still has foundation-only `NOT_IMPLEMENTED` and generic `NOT_FOUND`.
- Placeholder endpoints are not generated-client-ready implementations.
- Full runtime/OpenAPI conformance still needs a separate generated-client/API alignment task.

## 19. Risks before real hotel-search behavior

- Hotel search endpoints still return `501 NOT_IMPLEMENTED`.
- There is no hotel search criteria validation.
- There is no hotel search result envelope.
- There are no provider facts, assumptions, unknowns, ranking or offer results.
- Provider/API mapping remains deferred until a separate task with provider contract context.
- Real hotel-search behavior must replace placeholder responses with contract-aligned behavior through a separate roadmap-aligned task.

## 20. Что не проверялось

- Runtime behavior against generated TypeScript/OpenAPI clients.
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

## 21. Проверки

- `git status --short` — passed, worktree был чистым перед review report creation.
- `git diff --check` — passed before review report creation.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed before review report creation.
- `git diff --check` — passed after review report creation.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed after review report creation.

## 22. Рекомендации

- Commit this Stage 7.11a review report before starting the next Stage 7 task.
- Keep `nextAction` clearly foundation-only until dynamic clarification is explicitly activated.
- Keep `NOT_IMPLEMENTED` and generic `NOT_FOUND` out of final generated-client assumptions until a dedicated API alignment task resolves them.
- Consider a tiny future code-comment cleanup for the stale `Stage 7.3-7.9` wording in `AssistantSessionBoundary.kt`, if a later task touches nearby documentation comments.

## 23. Recommended next task

Good bounded next task: controlled internal slot update boundary, if it remains internal and does not introduce dynamic clarification, generated clients, real hotel search, LLM/provider integration, DB/storage or frontend work.

Alternative: generated-client/API readiness checkpoint, if the project wants to decide remaining `NOT_IMPLEMENTED`, generic `NOT_FOUND`, `hotelSearchRequest` and full OpenAPI/runtime conformance gaps before more runtime behavior.

## 24. Scope control confirmation

- Review-only quality gate completed.
- Backend behavior was not changed by this review.
- Stage 7.12+ was not started.
- OpenAPI draft was not changed.
- Product baseline and architecture baseline were not changed.
- Roadmap order was not changed.
- Internal state was not exposed.
- Generated clients, OpenAPI generation/finalization, requirements extraction, slot filling, dynamic clarification, LLM/provider integration, durable storage, frontend, hotel search, shortlist, explanations, booking, payment and flights were not added.
