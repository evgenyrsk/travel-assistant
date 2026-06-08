# Stage 7.11 — Assistant API Runtime Contract Alignment Cleanup

## 1. Цель Stage 7.11

Выполнить bounded runtime contract-alignment cleanup для assistant API foundation behavior после Stage 7.10 checkpoint.

Целью было безопасно закрыть те Stage 7.10 Minor findings, которые можно исправить без запуска generated clients, OpenAPI finalization, real assistant logic, requirements extraction, slot filling, LLM orchestration, provider integration, DB/storage, frontend или hotel search behavior.

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
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md`
- `docs/architecture/stage-6/stage-6-final-closure-and-handoff.md`
- `services/backend/src/main/kotlin/**`
- `services/backend/src/test/kotlin/**`

## 3. Что было реализовано

- Assistant session creation response переведен на bounded Stage 6-like shape:
  - `session`;
  - `assistantMessage`;
  - `nextAction`.
- Assistant message intake response переведен на тот же bounded Stage 6-like shape.
- `assistantMessage` остается deterministic/static placeholder-only.
- Добавлена минимальная поддержка optional initial `message` на `POST /api/v1/assistant/sessions`.
- Optional initial `message` обрабатывается только как foundation intake:
  - создается process-local session;
  - обновляются minimal clarification metadata;
  - internal coverage plan пересчитывается;
  - текст не сохраняется как history;
  - extraction, slot filling, dynamic reply, LLM/provider calls не выполняются.
- Validation error response переведен с `details.field` / `details.message` на `fields`.
- `SESSION_NOT_FOUND`, placeholder `NOT_IMPLEMENTED`, generic `NOT_FOUND` и `INTERNAL_ERROR` сохранены как structured foundation behavior.
- Route tests обновлены под новый response shape и validation shape.
- Добавлены route tests для optional initial message и blank initial message validation.
- `services/backend/README.md`, `docs/roadmap/roadmap.md` и `docs/reviews/README.md` обновлены минимально под Stage 7.11.

## 4. Созданные файлы

- `docs/reviews/stage-7-11-assistant-api-runtime-contract-alignment-cleanup.md`

## 5. Изменённые файлы

- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ErrorResponse.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/api/AssistantSessionRoutesTest.kt`

## 6. Addressed Stage 7.10 findings

### MI-S7.10-001 — Assistant response shape differs from Stage 6 direction

Status: addressed for assistant foundation runtime.

Assistant session creation and message intake now return:

- `session.sessionId`;
- `session.status`;
- `session.createdAt`;
- `session.updatedAt`;
- `assistantMessage.role`;
- `assistantMessage.content`;
- `nextAction`.

The runtime still does not return `hotelSearchRequest` because search readiness is not implemented.

### MI-S7.10-002 — Optional initial message on session creation

Status: addressed as bounded foundation behavior.

`POST /api/v1/assistant/sessions` now accepts optional JSON body with `message`. If the message is present and non-blank, it is passed through the existing local message intake boundary after session creation. It does not trigger requirements extraction, slot filling, message history, dynamic clarification, LLM, provider calls or hotel search.

### MI-S7.10-003 — Validation error shape differs from Stage 6 direction

Status: partially addressed.

Validation errors now return `fields`, matching the Stage 6 direction for validation payloads.

Foundation-only runtime codes `NOT_IMPLEMENTED` and generic `NOT_FOUND` remain because placeholder endpoints and generic unknown-route handling are still intentionally not final generated-client behavior.

## 7. Deferred Stage 7.10 findings

### MI-S7.10-004 — Placeholder `501 NOT_IMPLEMENTED`

Status: deferred.

Hotel search, shortlist and explanation endpoints still return `501 NOT_IMPLEMENTED`. This remains the correct foundation behavior because Stage 7.11 does not implement hotel search, shortlist, explanations, provider integration, ranking or LLM orchestration.

### Remaining error taxonomy alignment

Status: deferred.

`NOT_IMPLEMENTED` and generic `NOT_FOUND` remain foundation-level codes and are not final Stage 6 generated-client taxonomy. They should be resolved only when placeholder endpoints are replaced by real contract-aligned behavior or when a separate generated-client preparation task explicitly decides how foundation runtime errors should be modeled.

## 8. Endpoint behavior

`POST /api/v1/assistant/sessions` now returns `201 Created`:

```json
{
  "session": {
    "sessionId": "assistant-session-local-000001",
    "status": "collecting_requirements",
    "createdAt": "2026-06-04T00:00:00Z",
    "updatedAt": "2026-06-04T00:00:00Z"
  },
  "assistantMessage": {
    "role": "assistant",
    "content": "I received your hotel request. Please share destination, dates, guests, and budget so I can continue."
  },
  "nextAction": "ask_clarification"
}
```

If an optional initial `message` is provided, response shape stays the same. The session exists in the process-local store and can be used by subsequent message intake.

`POST /api/v1/assistant/sessions/{sessionId}/messages` returns `200 OK` with the same response shape and static placeholder assistant message.

Unknown session on message intake still returns structured `404 SESSION_NOT_FOUND`.

Blank or missing `message` still returns structured `400 VALIDATION_ERROR`.

## 9. Response shape alignment

- Assistant success responses now use `session` and `assistantMessage`, closer to Stage 6 `AssistantMessageResponse`.
- `nextAction` is returned as `ask_clarification`.
- `assistantMessage.role` is always `assistant`.
- `assistantMessage.content` remains deterministic placeholder copy.
- `hotelSearchRequest` is not returned because search readiness and hotel search behavior are not implemented.
- Internal `clarificationState`, `hotelRequirementsState` and `hotelRequirementsCoveragePlan` are not exposed.
- The response shape is closer to Stage 6 direction, but it is still a foundation runtime shape rather than a final generated-client-ready contract.

## 10. Error behavior alignment

- `VALIDATION_ERROR` now returns `fields`.
- Existing validation behavior for missing body, missing `message` and blank `message` is preserved.
- Blank optional initial `message` on session creation returns `VALIDATION_ERROR`.
- `SESSION_NOT_FOUND` behavior is preserved.
- `NOT_IMPLEMENTED` placeholder behavior is preserved.
- Generic `NOT_FOUND` for unknown routes is preserved.
- No broad global error taxonomy redesign was performed.

## 11. Placeholder behavior

- Hotel search endpoints remain placeholder-only.
- Shortlist endpoints remain placeholder-only.
- Explanation endpoint remains placeholder-only.
- Placeholder routes still return `501 NOT_IMPLEMENTED` with `details.boundary`.
- No real hotel search, ranking, shortlist, explanations/comparison, provider calls, DB/storage, Redis, LLM or generated clients were added.

## 12. Архитектурные границы

- Backend stack remains Kotlin + Ktor.
- Ktor routing remains thin and delegates session behavior to `AssistantSessionBoundary`.
- Domain/application code does not depend on Ktor.
- No DB, Redis, provider SDK, LLM SDK, frontend tooling or generated-client tooling dependency was added.
- Process-local `AssistantSessionStateStore` remains injectable and testable.
- Internal assistant state stays internal.
- Optional initial message support does not create message history or durable persistence.

## 13. Что осталось placeholder/future boundary

- Generated-client preparation.
- OpenAPI generation or OpenAPI finalization.
- Full runtime/OpenAPI conformance.
- Real assistant state machine.
- Dynamic clarification questions.
- Requirements extraction.
- Slot filling.
- Intent classification.
- Message history.
- Session retrieval/listing endpoints.
- Durable persistence, DB/storage and Redis/cache.
- Hotel search, offers, ranking, shortlist and explanations.
- Provider integration and provider/API mapping.
- LLM orchestration and `LlmClient`.
- Frontend integration.

## 14. Что намеренно не реализовывалось

- Generated clients.
- OpenAPI draft rewrite or finalization.
- DB migrations, entities, repositories or durable persistence.
- Redis/cache.
- Session retrieval/listing endpoints.
- Message history or storing full message text as history.
- Dynamic assistant replies.
- Real stateful clarification flow.
- Requirements extraction.
- Slot filling.
- Intent classification.
- LLM provider integration or calls.
- Hotel provider integration.
- Hotel search behavior.
- Ranking.
- Shortlist behavior.
- Explanation/comparison behavior.
- Authentication/account flows.
- Frontend.
- Booking, payment, flights or combined itinerary.
- Docker/deployment infrastructure.
- Product baseline or architecture baseline rewrite.

## 15. Проверки

- `git status --short` — passed, worktree был чистым перед изменениями.
- `git diff --check` — passed after code changes before documentation/report updates.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed after code changes before documentation/report updates.
- `git diff --check` — passed after report creation.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed after report creation.

## 16. Known limitations

- Assistant success responses are closer to Stage 6 direction but still not full generated-client-ready `AssistantMessageResponse`.
- `hotelSearchRequest` is omitted until search readiness exists.
- `nextAction` is static `ask_clarification`.
- `assistantMessage.content` is static placeholder copy.
- Optional initial message is not stored as history and is not analyzed.
- Runtime still has foundation-only `NOT_IMPLEMENTED` and generic `NOT_FOUND` codes.
- Placeholder endpoints are not contract-ready implementation.
- Session state remains process-local and is lost on restart.

## 17. Recommended next task

Good bounded next step: review/quality gate for Stage 7.11.

Alternative implementation step: controlled internal slot update boundary, if it remains internal and does not introduce dynamic clarification, generated clients, real hotel search, LLM/provider integration, DB/storage or frontend work.

## 18. Scope control confirmation

- Stage 7.11 completed as one bounded assistant API runtime contract alignment cleanup.
- Stage 7.12+ was not started.
- Roadmap order was not changed.
- OpenAPI draft was not changed.
- Product baseline and architecture baseline were not rewritten.
- Internal state was not exposed.
- Requirements extraction, slot filling, dynamic clarification, LLM/provider integration, durable storage, frontend, generated clients, hotel search, shortlist, explanations, booking, payment and flights were not added.
