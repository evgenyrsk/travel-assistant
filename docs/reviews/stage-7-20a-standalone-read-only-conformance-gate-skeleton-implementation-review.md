# Stage 7.20a — Standalone Read-only Conformance Gate Skeleton Implementation Review

## 1. Цель review

Цель Stage 7.20a — провести review-only проверку Stage 7.20 standalone read-only conformance gate skeleton implementation.

Review проверяет, что Stage 7.20 changes соответствуют решениям Stage 7.19, остаются isolated/read-only, не заявляют generated-client/OpenAPI readiness, не создают generated-client-ready subset, не реализуют full conformance gate, не меняют OpenAPI/backend behavior и не запускают Stage 7.21+.

## 2. Проверенные источники

- `AGENTS.md`
- `docs/prompts/codex-task-template.md`
- `docs/prompts/codex-review-template.md`
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
- `docs/reviews/README.md`
- `services/backend/README.md`
- `docs/reviews/stage-7-16-generated-client-openapi-conformance-gate-planning.md`
- `docs/reviews/stage-7-17-generated-client-ready-subset-placeholder-exclusion-policy.md`
- `docs/reviews/stage-7-18-conformance-gate-skeleton-planning-to-tooling.md`
- `docs/reviews/stage-7-19-conformance-gate-skeleton-implementation-planning.md`
- `docs/reviews/stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md`
- `docs/architecture/stage-6/openapi-contract-notes.md`
- `docs/architecture/stage-6/openapi-draft.yaml`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/ApiRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/HealthRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/AssistantPlaceholderRoutes.kt`
- `services/backend/src/main/kotlin/com/travelassistant/backend/api/HotelSearchPlaceholderRoutes.kt`
- `tools/openapi-conformance/README.md`
- `tools/openapi-conformance/check`
- `tools/openapi-conformance/package.json`
- `tools/openapi-conformance/package-lock.json`
- `tools/openapi-conformance/tsconfig.json`
- `tools/openapi-conformance/src/cli.ts`
- `tools/openapi-conformance/src/openapi.ts`
- `tools/openapi-conformance/src/paths.ts`
- `tools/openapi-conformance/src/placeholder-policy.ts`
- `tools/openapi-conformance/src/report.ts`
- `tools/openapi-conformance/src/route-inventory.ts`
- `tools/openapi-conformance/src/subset-manifest.ts`
- `tools/openapi-conformance/src/types.ts`

В корне репозитория отдельный `openapi-draft.yaml` не найден. Актуальный OpenAPI source находится в `docs/architecture/stage-6/openapi-draft.yaml`.

## 3. Использование task/review templates

`docs/prompts/codex-task-template.md` был прочитан до repository inspection и использован для проверки allowed/forbidden scope, validation expectations и final report structure.

`docs/prompts/codex-review-template.md` был прочитан до repository inspection и использован как primary review structure: проверены scope drift, roadmap/status consistency, architecture boundary violations, backend layering, missing validation, documentation consistency, source-of-truth duplication, stale status wording, broken navigation и recommendations not implemented.

## 4. Текущее состояние

Pre-check `git status --short` показал, что Stage 7.20 changes остаются незакоммиченными:

- `README.md`
- `docs/ROADMAP.md`
- `docs/reviews/README.md`
- `docs/roadmap/roadmap.md`
- `docs/reviews/stage-7-20-standalone-read-only-conformance-gate-skeleton-implementation.md`
- `tools/`

Stage 7.20 находится under review. Stage 7.21+ не начат. Generated-client/OpenAPI readiness не заявлена. Generated-client-ready subset не создан. Full conformance gate не реализован.

## 5. Reviewed diff scope

Reviewed diff scope соответствует Stage 7.20:

- новый isolated tool под `tools/openapi-conformance/`;
- Stage 7.20 implementation report;
- узкая запись Stage 7.20 в `docs/reviews/README.md`;
- узкая status/navigation синхронизация в `README.md`, `docs/ROADMAP.md`, `docs/roadmap/roadmap.md`.

Unrelated backend, OpenAPI, Gradle, CI, frontend, provider, LLM, DB/storage или generated-client files не изменялись.

## 6. Tool isolation review

Tool isolation passed:

- tool расположен под `tools/openapi-conformance/`;
- root package/build files не созданы и не изменены;
- dependencies локальны для tool package;
- `node_modules` и `dist` существуют локально после validation, но не tracked by `git status --short --untracked-files=all`;
- package files находятся только внутри `tools/openapi-conformance/`;
- README scoped только к tool usage;
- wrapper `tools/openapi-conformance/check` присутствует и executable.

## 7. Command/runtime review

Command/runtime review passed:

- command соответствует Stage 7.19: `./tools/openapi-conformance/check`;
- runtime/language соответствует Stage 7.19: Node.js + TypeScript;
- command собирает local TypeScript CLI и запускает `dist/cli.js`;
- backend server не запускается;
- HTTP requests не выполняются;
- root Gradle/CI lifecycle не меняется.

## 8. Dependency review

Dependency review passed:

- `yaml` используется как minimal runtime dependency для YAML/OpenAPI parsing;
- `typescript` и `@types/node` используются как tool-local dev dependencies;
- `package-lock.json` создан внутри tool directory и `npm install` завершился без изменений lockfile;
- root package files отсутствуют;
- `node_modules` не tracked.

## 9. JSON report review

JSON report behavior passed:

- successful command emits JSON to stdout;
- top-level `status` остается `"not_ready"`;
- `readinessClaim` остается `false`;
- report содержит `openApiSource`, `subsetManifest`, `inventories`, `endpoints`, `checks`, `blockingFindings`, `advisoryFindings`, `futureOnlyChecks`;
- generated-client generation/compile и runtime HTTP contract checks не выводятся как passed.

## 10. Exit code behavior review

Exit code behavior passed:

- `./tools/openapi-conformance/check` завершился с exit code `0` и JSON report;
- `./tools/openapi-conformance/check --bad-arg` завершился с exit code `2` и structured JSON error report;
- missing/invalid command usage не маскируется как successful readiness.

## 11. OpenAPI source/inventory review

OpenAPI source/inventory review passed:

- default detection checks `docs/architecture/stage-6/openapi-draft.yaml` before root `openapi-draft.yaml`;
- отсутствующий root `openapi-draft.yaml` не fatal, если Stage 6 draft найден;
- текущий report находит `docs/architecture/stage-6/openapi-draft.yaml`;
- OpenAPI version `3.1.0` и 9 operations извлекаются корректно;
- parser выполняет minimal skeleton-level structure checks only;
- OpenAPI draft не изменяется;
- OpenAPI finalization не заявляется.

## 12. Runtime route inventory review

Runtime route inventory review passed:

- scanner читает static Ktor route source files under `services/backend/src/main/kotlin/com/travelassistant/backend/api`;
- backend server не запускается;
- backend files не меняются;
- report явно помечает `runtime_route_inventory` как `advisory`;
- static scanner limitations отражены в README/report wording как conservative static scan;
- route inventory не трактуется как full runtime conformance.

## 13. Subset manifest handling review

Subset manifest handling passed:

- future manifest path: `docs/architecture/stage-7/generated-client-ready-subset.yaml`;
- Stage 7.20 не создает manifest;
- отсутствующий manifest выводится как `missing_not_created`;
- absence of manifest не является tool execution failure;
- manifest не заявляется существующим.

## 14. Placeholder/exclusion handling review

Placeholder/exclusion handling passed:

- `POST /api/v1/hotel-searches`;
- `GET /api/v1/hotel-searches/{searchId}/offers`;
- `GET /api/v1/assistant/sessions/{sessionId}/shortlist`;
- `PUT /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`;
- `DELETE /api/v1/assistant/sessions/{sessionId}/shortlist/{offerId}`;
- `POST /api/v1/assistant/sessions/{sessionId}/explanations`;

are classified as `placeholder_excluded`.

`GET /api/v1/health`, `POST /api/v1/assistant/sessions` and `POST /api/v1/assistant/sessions/{sessionId}/messages` are classified only as `foundation_candidate`, not generated-client-ready.

## 15. False readiness prevention review

False readiness prevention passed:

- `status: "not_ready"` is type-level/report-level enforced;
- `readinessClaim: false` is type-level/report-level enforced;
- endpoint-level `readiness` remains `"not_ready"`;
- future-only checks remain `future_only` / `not_run`;
- placeholders are excluded, not ready;
- OpenAPI parse success is not treated as runtime conformance;
- roadmap/status wording denies generated-client/OpenAPI readiness, subset existence, full conformance gate and generated clients.

## 16. Roadmap/status wording review

Roadmap/status wording review passed:

- Stage 7.20 wording is narrow: standalone read-only conformance gate skeleton implementation;
- Stage 7.21+ is not started;
- generated-client/OpenAPI readiness is not claimed;
- OpenAPI finalization readiness is not claimed;
- full conformance gate implementation is not claimed;
- generated-client-ready subset is not claimed as existing;
- generated clients are not claimed as generated.

## 17. Validation results

Validation commands:

- `git status --short` — passed as pre-check; showed Stage 7.20 changes are uncommitted.
- `npm install` from `tools/openapi-conformance/` — passed, output: `up to date`.
- `npm run build` from `tools/openapi-conformance/` — passed.
- `./tools/openapi-conformance/check` — passed with exit code `0`, emitted JSON report with `status: "not_ready"`, `readinessClaim: false`, 9 OpenAPI operations and 9 runtime routes.
- `./tools/openapi-conformance/check --bad-arg` — passed as negative smoke check with expected exit code `2`, emitted structured JSON error report.
- `git diff --check` — passed.
- `git status --short --untracked-files=all` — passed for isolation review; showed only expected tracked/untracked Stage 7.20 files, with no tracked `node_modules` or `dist`.

Backend Gradle tests were not run because Stage 7.20 did not change backend source, backend tests, Gradle files, API behavior or runtime behavior.

## 18. Findings by severity

No Critical, Major or Minor findings.

Notes:

- Stage 7.20 changes are still uncommitted and under review.
- `node_modules` and `dist` exist locally after validation but are ignored and not tracked.
- Static route scanning is intentionally conservative and should not be treated as full runtime conformance.

## 19. Critical findings

None.

## 20. Major findings

None.

## 21. Minor findings

None.

## 22. Notes

- The implementation matches Stage 7.19 decisions for standalone location, command name, runtime/language, JSON stdout, `not_ready` status and no Gradle/CI integration.
- The tool remains a reporting skeleton, not a blocking conformance gate.
- The review did not identify false readiness claims in tool output or status docs.

## 23. Final verdict

Passed.

## 24. Required follow-ups, if any

None.

## 25. What was intentionally not reviewed

Not reviewed as executable/accepted scope:

- generated-client generation;
- generated-client-ready subset manifest semantics;
- full OpenAPI schema validation;
- runtime HTTP contract tests;
- live backend route behavior;
- provider integration;
- LLM orchestration;
- real hotel search behavior;
- frontend behavior;
- booking, payment or flights.

These areas remain future-only and out of scope for Stage 7.20a.

## 26. Created files

- `docs/reviews/stage-7-20a-standalone-read-only-conformance-gate-skeleton-implementation-review.md`

## 27. Changed files

- `docs/reviews/README.md`

## 28. Scope control confirmation

Stage 7.20a was review-only. No implementation files were changed. No tool behavior was changed. No OpenAPI draft, backend code, generated clients, subset manifest, Gradle/CI integration, provider/LLM/search/frontend/booking/payment/flights work or Stage 7.21+ work was started.
