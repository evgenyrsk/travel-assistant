# Stage 7.33 — Ready Subset Manifest Candidate Definition

## 1. Scope

- [x] Bounded Stage 7 task
- [x] Manifest candidate only
- [x] No generated-client readiness claim
- [x] No OpenAPI/API contract changes
- [x] No generated clients
- [x] No backend/frontend runtime changes
- [x] No CI/Gradle integration
- [x] No Stage 8 activation

## 2. Executive summary

Stage 7.33 creates the first non-readiness manifest candidate at `docs/architecture/stage-7/generated-client-ready-subset.yaml`.

The manifest gives `tools/openapi-conformance/` a real candidate input for read-only skeleton validation while explicitly preserving `status: "not_ready"` and `readinessClaim: false`. It includes only `GET /api/v1/health` as a candidate-only foundation endpoint and keeps known placeholder endpoints excluded.

This stage does not claim generated-client readiness, does not generate clients, does not change OpenAPI/API contracts, does not change backend/frontend runtime behavior and does not connect the tool to CI/Gradle.

## 3. Files changed

| File | Change type | Reason |
|---|---|---|
| `docs/architecture/stage-7/generated-client-ready-subset.yaml` | Created | First non-readiness manifest candidate for skeleton validation. |
| `tools/openapi-conformance/README.md` | Updated | Clarifies missing manifest vs present non-readiness candidate semantics. |
| `tools/openapi-conformance/src/report.test.ts` | Updated | Aligns existing present-manifest test fixture with candidate manifest semantics and asserts no readiness promotion. |
| `docs/reviews/README.md` | Updated | Adds Stage 7.33 report to the review/audit index. |
| `docs/roadmap/roadmap.md` | Updated | Records Stage 7.33 completion and next bounded Stage 7 recommendation without readiness claim. |
| `docs/reviews/stage-7-33-ready-subset-manifest-candidate-definition.md` | Created | Stage 7.33 report and validation record. |

## 4. Manifest candidate semantics

| Field/section | Value | Meaning |
|---|---|---|
| `manifestVersion` | `stage-7-generated-client-ready-subset-v1` | Uses the Stage 7.23 schema contract identifier. |
| `scopeName` | `travel-assistant-stage-7-foundation-subset` | Names the candidate subset scope. |
| Top-level `status` | `not_ready` | Human-visible non-readiness status. |
| Top-level `readinessClaim` | `false` | Explicitly blocks interpreting the manifest as readiness evidence. |
| `validationStatus.status` | `not_ready` | Tool-facing non-readiness status. |
| `validationStatus.readinessClaim` | `false` | Tool-facing readiness non-claim. |
| `includedEndpoints` | `GET /api/v1/health` only | Candidate-only foundation endpoint; still `readiness: "not_ready"`. |
| `excludedEndpoints` | Known placeholder hotel search, offers, shortlist and explanation endpoints | Keeps placeholder behavior outside generated-client-ready scope. |
| `readinessCriteria` | All relevant gates set to `false` | No runtime contract, generated-client target, generation or compile gate has passed. |
| `generatedClientTargets` | Empty list | No generated-client target is selected or configured. |
| `knownLimitations` | Static inventory, endpoint reference validation, generated-client target and runtime contract checks remain blockers | Records why the manifest cannot be treated as readiness proof. |

## 5. Tool validation behavior

| Scenario | Expected behavior | Validation |
|---|---|---|
| Missing manifest | `manifestDetection.status: "missing"`, `manifestValidation.status: "not_run"`, report remains `not_ready`. | Existing test `reports missing manifest detection without readiness promotion` still passes. |
| Present candidate manifest | Manifest is read-only parsed and skeleton schema validation passes as advisory. | `./tools/openapi-conformance/check` returned exit code `0`, `manifestDetection.status: "present"` and `manifestValidation.status: "advisory_passed"`. |
| `readinessClaim: false` | Report keeps top-level `readinessClaim: false`; manifest validation does not promote readiness. | `npm test` passed; `./tools/openapi-conformance/check` output kept `readinessClaim: false`. |
| `status: not_ready` | Report keeps top-level `status: "not_ready"` even with a present valid candidate manifest. | `npm test` passed; `./tools/openapi-conformance/check` output kept `status: "not_ready"`. |

## 6. Readiness non-claim

- Generated-client readiness is not claimed.
- The manifest is not an approval list.
- Endpoints are not considered ready.
- `GET /api/v1/health` is only a candidate endpoint and remains `readiness: "not_ready"`.
- Generated clients are not created.
- Generated-client target is not declared.
- Generated-client generation and compile checks are not configured or run.
- Runtime HTTP contract checks are not run.
- CI/Gradle gate is not enabled.

## 7. Validation

| Command | Result |
|---|---|
| `git status --short` | Passed before edits; clean working tree, no output. |
| Required `sed -n ...` reads for AGENTS, README, roadmap docs, reviews index, Stage 7.25 report, Stage 7.32 report, tool README/source/tests and Stage 7 architecture directory | Passed; required context reviewed. |
| `test -f docs/architecture/stage-7/generated-client-ready-subset.yaml && sed -n '1,220p' docs/architecture/stage-7/generated-client-ready-subset.yaml || printf 'MISSING\n'` | Passed before edits; manifest was missing. |
| `npm test` from `tools/openapi-conformance/` | Passed; 7 tests passed. |
| `./tools/openapi-conformance/check` | Passed with exit code `0`; output kept `status: "not_ready"`, `readinessClaim: false`, `manifestDetection.status: "present"`, `manifestValidation.status: "advisory_passed"`, `blockingFindings: []`, `generated_client_generation: "future_only"`, `generated_client_compile: "not_run"` and `runtime_http_contract_tests: "not_run"`. |
| `git diff --check` | Passed; no whitespace errors. |
| `git status --short` | Passed after edits; only expected Stage 7.33 files changed. |
| `git diff --cached --check` | Passed after staging; no whitespace errors in new or modified Stage 7.33 files. |

Backend tests were not run because backend source, backend behavior, runtime behavior, OpenAPI contracts and generated-client artifacts were not changed.

## 8. Remaining limitations

- Endpoint reference validation remains future-only in the current tool.
- The manifest candidate is not a strict readiness gate.
- `GET /api/v1/health` still needs future runtime contract/schema validation before any readiness claim.
- Assistant foundation endpoints are not included in this first candidate and need a separate endpoint candidate review if they are considered later.
- Placeholder hotel search, offers, shortlist and explanation endpoints remain excluded.
- Generated-client target selection, generation, compile validation and runtime HTTP contract validation remain future work.
- CI/Gradle integration remains out of scope.

## 9. Recommended next step

Recommended next bounded Stage 7 task: **Stage 7.34 — Manifest Endpoint Reference Validation Hardening**.

The next task should make the tool validate candidate manifest endpoint references against the existing OpenAPI and static runtime inventories while preserving `status: "not_ready"`, `readinessClaim: false`, no generated clients, no OpenAPI/API contract changes, no backend/frontend runtime changes, no CI/Gradle gate and no Stage 8 activation.
