# Stage 7.21 — OpenAPI Conformance Report Depth and Tests

## 1. Цель Stage 7.21

Цель Stage 7.21 — выполнить самый узкий безопасный следующий шаг после Stage 7.20/7.20a: улучшить read-only reporting depth standalone OpenAPI conformance skeleton и добавить tool-local tests для report semantics.

Stage 7.21 не была явно заранее описана в roadmap как отдельная задача. Scope был inferred из current roadmap carryover, Stage 7.20 recommended next task и Stage 7.20a review notes: проверить report schema, route scanner limitations и false-readiness wording before any generated-client-ready subset manifest.

## 2. Проверенные источники

- `AGENTS.md`
- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/product/product-baseline.md`
- `docs/architecture/architecture-baseline.md`
- `docs/architecture/backend-layering-rules.md`
- `docs/development/README.md`
- `docs/development/coding-standards.md`
- `docs/development/testing-strategy.md`
- `docs/development/documentation-guidelines.md`
- `docs/development/definition-of-done.md`
- `docs/development/quality-gates.md`
- `docs/prompts/codex-task-template.md`
- `docs/prompts/codex-review-template.md`
- `docs/decisions/README.md`
- `docs/reviews/README.md`
- `docs/reviews/stage-7-16-generated-client-openapi-conformance-gate-planning.md`
- `docs/reviews/stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md`
- `docs/reviews/stage-7-18-conformance-gate-skeleton-planning-to-tooling.md`
- `docs/reviews/stage-7-19-conformance-gate-skeleton-implementation-planning.md`
- `docs/reviews/stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md`
- `docs/reviews/stage-7-20a-standalone-read-only-conformance-gate-skeleton-implementation-review.md`
- `tools/openapi-conformance/**`

Standalone accepted ADR files were not present.

## 3. Baseline repository state

Mandatory pre-checks before implementation:

- `git status --short --untracked-files=all` — clean.
- `git log --oneline -5` — latest commit was `9ce6371 tools: add stage 7.20 read-only openapi conformance skeleton`.
- `git diff --stat` — clean.

Ignored local validation artifacts under `tools/openapi-conformance/dist` and `tools/openapi-conformance/node_modules` may exist locally and were not added to scope.

## 4. Scope source

Stage 7.21 was inferred, not explicitly documented as a named roadmap task.

The safe scope came from:

- roadmap status that Stage 7.21+ required a separate bounded task;
- Stage 7.20 report recommendation for report schema / route scanner limitation / false-readiness wording review before subset manifest work;
- Stage 7.20a passed review with no required follow-ups, while preserving static scanner limitation and false-readiness prevention as key boundaries;
- user request allowing a smallest safe next step if Stage 7.21 was not explicitly defined.

## 5. What changed

Tool-local report depth:

- added `endpointClassificationSummary` to every JSON report;
- summary counts total endpoints, classification buckets, OpenAPI-only endpoints, runtime-only endpoints and endpoints present in both inventories;
- added advisory `endpoint_classification_summary` check;
- added advisory findings for visible unclassified endpoints and runtime-only endpoints when present;
- kept these findings advisory only in the current classification skeleton mode.

Tool-local tests:

- added `npm test`;
- added Node built-in test coverage for `buildReport`;
- tests assert `status: "not_ready"`, `readinessClaim: false`, no blocking findings, endpoint classification counts and advisory visibility for unclassified/runtime-only drift.

Documentation/status:

- documented `endpointClassificationSummary` and local tests in the tool README;
- created this Stage 7.21 report;
- updated reviews index and active status wording to record Stage 7.21 completion and Stage 7.22+ not started.

## 6. Created files

- `docs/reviews/stage-7-21-openapi-conformance-report-depth-tests.md`
- `tools/openapi-conformance/src/report.test.ts`

## 7. Modified files

- `README.md`
- `docs/ROADMAP.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/README.md`
- `tools/openapi-conformance/README.md`
- `tools/openapi-conformance/package.json`
- `tools/openapi-conformance/src/cli.ts`
- `tools/openapi-conformance/src/report.ts`
- `tools/openapi-conformance/src/types.ts`

## 8. Validation

- `npm install` from `tools/openapi-conformance/` — passed; output: `up to date`.
- `npm run build` from `tools/openapi-conformance/` — passed.
- `npm test` from `tools/openapi-conformance/` — passed; 2 tests passed.
- `./tools/openapi-conformance/check` — passed with exit code `0`; emitted JSON with `status: "not_ready"`, `readinessClaim: false`, 9 OpenAPI operations, 9 runtime routes, 3 `foundation_candidate`, 6 `placeholder_excluded`, 0 `runtime_only`, 0 `unclassified`.
- `./tools/openapi-conformance/check --bad-arg` — passed as negative smoke check with expected exit code `2`; emitted structured JSON error report with `status: "not_ready"` and `readinessClaim: false`.
- `git diff --check` — passed.

Backend Gradle tests were not run because Stage 7.21 did not change backend source, backend tests, Gradle files, API behavior or runtime behavior.

## 9. Readiness semantics

Readiness semantics were not overstated:

- top-level `status` remains `"not_ready"`;
- top-level `readinessClaim` remains `false`;
- endpoint-level `readiness` remains `"not_ready"`;
- `endpointClassificationSummary` is reporting-only;
- runtime-only/unclassified visibility is advisory in current skeleton mode;
- subset manifest is still missing/not_created and optional for the skeleton;
- generated-client generation/compile and runtime HTTP contract checks remain future-only/not_run;
- placeholder endpoints remain excluded and not generated-client-ready.

## 10. What was intentionally not implemented

- Generated-client-ready subset manifest.
- Generated-client generation or compile gate.
- Full OpenAPI/runtime conformance gate.
- Runtime HTTP contract tests.
- OpenAPI draft changes or OpenAPI finalization.
- Backend code, backend behavior, Gradle integration or CI integration.
- Provider integration, LLM orchestration, requirements extraction or real hotel search behavior.
- Frontend, booking, payment, flights or combined itinerary work.

## 11. Risks and follow-ups

- Static route scanning remains conservative and advisory; it is still not full runtime conformance.
- `endpointClassificationSummary` expands the JSON report shape, so downstream consumers should treat the report as skeleton-stage and not a stable external API.
- A future explicit task is still required before creating `docs/architecture/stage-7/generated-client-ready-subset.yaml`.
- A future review task may inspect Stage 7.21 if desired before any subset manifest or generated-client work.

## 12. Scope control confirmation

Stage 7.21 remained isolated to `tools/openapi-conformance/**` plus narrow navigation/status/report documentation.

No generated-client readiness, OpenAPI finalization, subset creation, backend behavior, CI/Gradle integration, generated clients, provider/LLM/search/frontend/booking/payment/flights work or Stage 7.22+ work was started.
