# Stage 7.14 — Generated Client / OpenAPI Readiness Cleanup

## 1. Цель Stage 7.14

Выполнить bounded generated-client/OpenAPI readiness cleanup после Stage 7.13 checkpoint.

Фокус cleanup:

- placeholder endpoint strategy;
- runtime error taxonomy readiness.

Stage 7.14 не является generated-client implementation, OpenAPI finalization, real hotel search, provider integration, frontend work, DB/storage, Redis, LLM behavior или production behavior task.

## 2. Проверенные источники

- `docs/prompts/codex-task-template.md`
- `docs/prompts/codex-review-template.md`
- `AGENTS.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/architecture/backend-layering-rules.md`
- `docs/development/README.md`
- `docs/development/coding-standards.md`
- `docs/development/kotlin-backend-style-guide.md`
- `docs/development/testing-strategy.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/definition-of-done.md`
- `docs/development/quality-gates.md`
- `docs/prompts/README.md`
- `docs/decisions/README.md`
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-13-generated-client-openapi-readiness-checkpoint.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md`
- `docs/architecture/stage-6/provider-boundary-mapping-notes.md`
- `docs/architecture/stage-6/stage-6-final-closure-and-handoff.md`
- `services/backend/src/main/kotlin/**`
- `services/backend/src/test/kotlin/**`
- `git status --short`

Standalone accepted ADR files отсутствуют.

## 3. Использование task/review templates

`docs/prompts/codex-task-template.md` использован как execution structure:

- goal: bounded cleanup for placeholder strategy and error taxonomy readiness;
- expected outcome: минимальные изменения runtime wording/docs/index и новый Stage 7.14 report;
- allowed scope: placeholder/error readiness cleanup, backend README, reviews index, tests only if runtime behavior changes;
- forbidden scope: generated clients, OpenAPI rewrite/finalization, real hotel search, provider/LLM/frontend/DB/storage work, Stage 7.15+ activation;
- validation: `git status --short`, `git diff --check`, backend `./gradlew test`.

`docs/prompts/codex-review-template.md` использован для self-review:

- проверены scope drift, unrelated changes, roadmap/status consistency, architecture/layering, API/contract consistency, test coverage, documentation/navigation и recommendations not implemented;
- self-review summary включен в этот report.

## 4. Что было реализовано / изменено

Изменено:

- placeholder error response message теперь явно говорит, что boundary является Stage 7 foundation placeholder и не является generated-client-ready;
- `PlaceholderRoutesTest` обновлен под новый placeholder message;
- `services/backend/README.md` уточняет Stage 7.14 generated-client readiness strategy:
  - placeholder endpoints остаются runtime-only foundation placeholders;
  - placeholder endpoints исключаются из будущего generated-client-ready subset до появления contract-aligned behavior;
  - placeholder responses не должны имитировать реальные hotel search, offers, shortlist или explanation success schemas;
  - `NOT_IMPLEMENTED` и generic `NOT_FOUND` остаются foundation-only runtime codes;
  - resource-specific not-found codes должны появляться только вместе с реальными resource semantics;
- `docs/reviews/README.md` узко индексирует Stage 7.12b, Stage 7.12c, Stage 7.12d, Stage 7.13 и Stage 7.14 artifacts;
- создан этот Stage 7.14 cleanup report.

Не изменено:

- endpoint paths;
- HTTP status codes;
- public assistant success response shape;
- OpenAPI draft;
- roadmap/status docs;
- product baseline;
- architecture baseline.

## 5. Созданные файлы

- `docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup.md`

## 6. Изменённые файлы

- `services/backend/src/main/kotlin/com/travelassistant/backend/api/PlaceholderResponses.kt`
- `services/backend/src/test/kotlin/com/travelassistant/backend/api/PlaceholderRoutesTest.kt`
- `services/backend/README.md`
- `docs/reviews/README.md`

## 7. Addressed Stage 7.13 findings

### MA-S7.13-001 — Placeholder public endpoints block generated-client readiness

Status: partially addressed as strategy/documentation and runtime wording cleanup.

Stage 7.14 does not make placeholder endpoints generated-client-ready. Instead it makes the safe strategy explicit:

- keep placeholder endpoints as runtime-only foundation placeholders;
- exclude placeholder endpoints from any future generated-client-ready subset until separate roadmap-aligned tasks implement contract-aligned behavior;
- do not return fake `HotelSearchResponse`, `HotelOffersResponse`, `ShortlistResponse`, `ShortlistItem` or `AssistantExplanationResponse`;
- keep `501 NOT_IMPLEMENTED` visible for foundation endpoints.

This reduces ambiguity without pretending real hotel behavior exists.

### MA-S7.13-002 — Runtime error taxonomy is not generated-client-safe

Status: partially addressed as readiness strategy.

Stage 7.14 documents that:

- `VALIDATION_ERROR`, `SESSION_NOT_FOUND` and `INTERNAL_ERROR` are the closest current runtime codes to Stage 6 direction;
- `NOT_IMPLEMENTED` and generic `NOT_FOUND` remain foundation-only runtime codes;
- `NOT_IMPLEMENTED` and generic `NOT_FOUND` must not be treated as final generated-client taxonomy;
- resource-specific `HOTEL_SEARCH_NOT_FOUND`, `HOTEL_OFFER_NOT_FOUND` and `SHORTLIST_ITEM_NOT_FOUND` should not be emitted until corresponding resource semantics exist.

No broad error taxonomy redesign was performed.

## 8. Deferred Stage 7.13 findings

Deferred:

- final generated-client readiness for hotel search, offers, shortlist and explanation endpoints;
- full runtime/OpenAPI error taxonomy alignment;
- assistant response semantics:
  - dynamic `nextAction`;
  - `hotelSearchRequest`;
  - public `SearchIntentSummary`;
  - assumptions / derived assumptions / unknowns;
- optional initial message as real intent capture / clarification behavior;
- generated-client/OpenAPI conformance test gate;
- real hotel search request/value boundary.

These remain deferred because resolving them requires separate behavior, contract or tooling tasks.

## 9. Placeholder endpoint strategy

Stage 7.14 placeholder strategy:

1. Placeholder endpoints remain public runtime boundaries for foundation visibility and route inventory.
2. Placeholder endpoints are **not** part of a future generated-client-ready subset until they stop returning `501 NOT_IMPLEMENTED`.
3. Generated clients must not be generated against current placeholder endpoint behavior as final API behavior.
4. Placeholder endpoints must not return fake success schemas.
5. Placeholder endpoints must not call mock search, fake ranking, fake offers, fake shortlist state, fake explanations, provider adapters, DB, Redis, LLMs or frontend tooling.
6. Success-schema alignment is deferred until real contract-aligned behavior exists.
7. If a future task wants subset generation before real hotel behavior, it must explicitly define which endpoints are included and how placeholder endpoints are excluded.

Current placeholder endpoints:

- `POST /api/v1/hotel-searches`;
- `GET /api/v1/hotel-searches/{searchId}/offers`;
- `GET /api/v1/assistant/sessions/{sessionId}/shortlist`;
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`;
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`;
- `POST /api/v1/assistant/sessions/{sessionId}/explanations`.

## 10. Error taxonomy readiness

Current runtime codes:

- `VALIDATION_ERROR`;
- `SESSION_NOT_FOUND`;
- `NOT_IMPLEMENTED`;
- `NOT_FOUND`;
- `INTERNAL_ERROR`.

Stage 7.14 decision:

- keep `VALIDATION_ERROR` shape with `fields`;
- keep `SESSION_NOT_FOUND` for unknown process-local assistant sessions;
- keep `NOT_IMPLEMENTED` for foundation placeholder endpoints;
- keep generic `NOT_FOUND` for unknown routes as foundation-only runtime behavior;
- keep `INTERNAL_ERROR` as structured fallback;
- do not add or emit `HOTEL_SEARCH_NOT_FOUND`, `HOTEL_OFFER_NOT_FOUND` or `SHORTLIST_ITEM_NOT_FOUND` until real resource semantics exist;
- do not add `NOT_IMPLEMENTED` or generic `NOT_FOUND` to OpenAPI final taxonomy in this task;
- do not finalize generated-client error semantics in this task.

Rationale:

- changing placeholder errors to resource-specific 404 codes would falsely imply resources exist;
- changing placeholder errors to `INTERNAL_ERROR` would hide intentional foundation boundaries;
- returning success schemas would create fake product behavior;
- keeping explicit `501 NOT_IMPLEMENTED` is safer until real endpoint slices exist.

## 11. Public API behavior

Changed:

- placeholder error `message` text now explicitly says the boundary is a Stage 7 foundation placeholder and is not generated-client-ready.

Unchanged:

- endpoint paths;
- `501 Not Implemented` status for placeholder endpoints;
- `code = NOT_IMPLEMENTED` for placeholder endpoints;
- `details.boundary` for placeholder endpoints;
- assistant session creation success shape;
- assistant message intake success shape;
- validation error shape;
- `SESSION_NOT_FOUND`;
- generic unknown route `NOT_FOUND`;
- `INTERNAL_ERROR`.

## 12. Internal state exposure

Internal state remains internal:

- `clarificationState` is not returned in public DTOs;
- `hotelRequirementsState` is not returned in public DTOs;
- `hotelRequirementsCoveragePlan` is not returned in public DTOs;
- internal slot update command/result types are not exposed through Ktor routes;
- no public slot update endpoint was added.

## 13. Documentation/navigation updates

Updated:

- `services/backend/README.md` now documents Stage 7.14 placeholder/generated-client subset strategy and foundation-only error taxonomy.
- `docs/reviews/README.md` now indexes:
  - Stage 7.12b cleanup report;
  - Stage 7.12c review report;
  - Stage 7.12d consolidation checkpoint;
  - Stage 7.13 generated-client/OpenAPI readiness checkpoint;
  - Stage 7.14 generated-client/OpenAPI readiness cleanup.

Not updated:

- `docs/roadmap/roadmap.md`;
- `docs/ROADMAP.md`;
- `README.md`;
- product baseline;
- architecture baseline.

Roadmap/status docs were not changed because this task explicitly allowed recording the cleanup only in `docs/reviews` and backend docs when in doubt. The explicit user task is the activation source for Stage 7.14.

## 14. Что осталось foundation-only

- Process-local assistant session state.
- Static `assistantMessage`.
- Static `nextAction = ask_clarification`.
- Omitted `hotelSearchRequest`.
- Optional initial message as local intake only.
- `NOT_IMPLEMENTED` placeholder endpoints.
- Generic unknown-route `NOT_FOUND`.
- Internal `clarificationState`.
- Internal `hotelRequirementsState`.
- Internal `hotelRequirementsCoveragePlan`.
- Internal structured slot status update boundary.
- No generated-client-ready subset.
- No OpenAPI/runtime conformance gate.

## 15. Что намеренно не реализовывалось

- Generated clients.
- Client generation command.
- OpenAPI generation.
- OpenAPI draft rewrite.
- OpenAPI finalization.
- Real hotel search.
- Hotel search request/value boundary.
- Hotel offers.
- Ranking.
- Shortlist behavior.
- Explanation/comparison behavior.
- Requirements extraction.
- Natural-language slot filling.
- Dynamic clarification.
- Message history.
- Durable persistence.
- DB/storage.
- Redis.
- Provider integration.
- Provider SDKs.
- LLM orchestration.
- OpenAI or external LLM calls.
- Frontend.
- Booking.
- Payment.
- Flights.
- Product baseline rewrite.
- Architecture baseline rewrite.
- Roadmap redefinition.
- Stage 7.15+ activation.
- Broad documentation cleanup.

## 16. Проверки

- `git status --short` — passed, clean before changes.
- `git diff --check` — passed after changes.
- `git diff --no-index --check /dev/null docs/reviews/stage-7-14-generated-client-openapi-readiness-cleanup.md` — no whitespace error output for the new untracked report file; command exit code `1` is expected for `--no-index` because the compared files differ.
- `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./gradlew test` из `services/backend` — passed after changes.
- `git status --short` — reviewed after validation; only scoped Stage 7.14 files changed.

## 17. Known limitations

- Runtime is still not ready for generated clients.
- Placeholder endpoints still do not match OpenAPI success schemas.
- Error taxonomy remains partial and foundation-only for placeholder / unknown-route behavior.
- No generated-client subset config exists.
- No OpenAPI/runtime conformance test gate exists.
- Assistant response semantics remain foundation-only.
- Real search/value/resource semantics remain absent.
- Roadmap still has older Stage 7.12/7.13+ wording; not changed in this bounded cleanup.

## 18. Self-review summary

Self-review выполнен по `docs/prompts/codex-review-template.md`.

- Scope control: passed; no generated clients, OpenAPI finalization, real search or provider/frontend/storage work added.
- Roadmap alignment: passed; Stage 7.15+ not activated.
- Source-of-truth alignment: passed; checked roadmap, baselines, Stage 7.13 report, Stage 6 OpenAPI notes, backend README and development rules.
- Architecture/layering: passed; no domain/application dependency on Ktor added, no provider/DB/LLM dependencies added.
- API/contract consistency: passed; placeholder mismatch documented and not hidden.
- Test coverage: passed; only placeholder message changed and route test was updated.
- Documentation/navigation: passed; backend README and reviews index updated narrowly.
- Historical docs: passed; no historical reports were rewritten.

## 19. Recommended next task

Recommended next task: **Stage 7.15 — Assistant Response Semantics / Search Readiness Boundary Cleanup**, if explicitly requested.

Suggested scope for that future task:

- define when `nextAction` can move beyond `ask_clarification`;
- define when `hotelSearchRequest` can be produced;
- keep internal slot state private;
- avoid real search/provider/LLM/frontend/DB work unless explicitly activated.

Alternative next task: generated-client/OpenAPI conformance gate planning, still without running generation against placeholder endpoints.

## 20. Scope control confirmation

- Stage 7.14 completed as bounded generated-client/OpenAPI readiness cleanup.
- Backend behavior changed only by placeholder error message text.
- Placeholder endpoints still return `501 NOT_IMPLEMENTED`.
- Public assistant success behavior unchanged.
- Internal state not exposed.
- OpenAPI draft not changed.
- Generated clients not created.
- No real hotel/search/shortlist/explanation behavior added.
- No provider, LLM, DB/storage, Redis or frontend work added.
- Product baseline and architecture baseline not rewritten.
- Roadmap/order not changed.
- Stage 7.15+ not started.
