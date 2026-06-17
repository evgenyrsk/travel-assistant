# Stage 7.34 — Manifest Candidate Validation Hardening

## 1. Scope

- [x] Bounded Stage 7 task
- [x] Manifest validation hardening only
- [x] No generated-client readiness claim
- [x] No OpenAPI/API contract changes
- [x] No generated clients
- [x] No backend/frontend runtime changes
- [x] No CI/Gradle integration
- [x] No Stage 8 activation

## 2. Executive summary

Stage 7.34 усиливает guardrails вокруг `docs/architecture/stage-7/generated-client-ready-subset.yaml`: `tools/openapi-conformance` теперь явно блокирует преждевременные readiness promotion signals в manifest candidate.

Validation остается tool-local, read-only и advisory для текущего Stage 7 scope. Missing manifest по-прежнему не блокирует command, present `not_ready` candidate проходит skeleton validation, а `readinessClaim: true`, `status: "ready"`, endpoint `readiness: "ready"` и readiness-like true gates в `readinessCriteria` получают blocking findings.

Generated-client readiness не заявлена. Generated clients, OpenAPI/API contracts, backend/frontend runtime, CI/Gradle integration и Stage 8 не менялись.

## 3. Files changed

| File | Change type | Reason |
|---|---|---|
| `tools/openapi-conformance/src/subset-manifest.ts` | Updated | Adds validation guardrails for top-level, validationStatus, endpoint-level and readinessCriteria readiness promotion signals. |
| `tools/openapi-conformance/src/report.test.ts` | Updated | Adds tool-local tests for missing manifest, present candidate manifest, invalid readiness promotion signals and current repository manifest validation. |
| `tools/openapi-conformance/README.md` | Updated | Documents allowed manifest states and why readiness promotion fields are blocked in current Stage 7 scope. |
| `docs/reviews/README.md` | Updated | Adds Stage 7.34 report to the review/audit index. |
| `docs/roadmap/roadmap.md` | Updated | Records Stage 7.34 completion and next bounded Stage 7 recommendation without readiness claim. |
| `docs/reviews/stage-7-34-manifest-candidate-validation-hardening.md` | Created | Stage 7.34 validation hardening report. |

## 4. Validation semantics

| Scenario | Expected behavior | Blocking? |
|---|---|---|
| Missing manifest | `manifestDetection.status: "missing"`, `manifestValidation.status: "not_run"`, command exits `0`, report stays `status: "not_ready"` and `readinessClaim: false`. | No |
| Present `not_ready` candidate | YAML parses, skeleton schema passes, report stays advisory/not_ready, generated-client checks remain future-only or not_run. | No |
| `readinessClaim: true` | Top-level or `validationStatus.readinessClaim` is flagged with `readiness_promotion_blocked`. | Yes |
| `status: "ready"` | Top-level or `validationStatus.status` is flagged with `readiness_promotion_blocked`. | Yes |
| Endpoint readiness `ready` | `includedEndpoints[*].readiness` or `excludedEndpoints[*].readiness` set to `ready` is flagged with `readiness_promotion_blocked`. | Yes |
| Readiness criteria true gate | Any `readinessCriteria.*: true` is flagged until a separate readiness stage runs actual checks. | Yes |

## 5. Tests added or updated

| Test area | Coverage |
|---|---|
| Missing manifest remains non-blocking | Existing test now also asserts `blockingFindings: []`. |
| Present candidate manifest passes advisory validation | Existing present-manifest test verifies `advisory_passed`, `future_only`, `status: "not_ready"`, `readinessClaim: false` and no blocking findings. |
| Top-level readiness promotion | New test blocks top-level `readinessClaim: true` and `status: "ready"`. |
| `validationStatus` readiness promotion | New test blocks `validationStatus.readinessClaim: true` and `validationStatus.status: "ready"`. |
| Endpoint readiness promotion | New test blocks endpoint `readiness: "ready"`. |
| Readiness criteria promotion | New test blocks premature true readiness gates such as `generatedClientCompilePassed: true`. |
| Current repository manifest | New test validates the real repository manifest candidate and verifies no readiness promotion or blocking findings. |

## 6. Readiness non-claim

- Generated-client readiness is not claimed.
- The manifest is not an approval list.
- Endpoints are not considered ready.
- Candidate endpoints remain `readiness: "not_ready"`.
- Generated clients are not created.
- Generated-client generation and compile checks are not configured or run.
- Runtime HTTP contract checks are not run.
- CI gate is not enabled.

## 7. Validation commands

| Command | Result |
|---|---|
| `git status --short` | Passed before edits; clean working tree, no output. |
| Required `sed -n ...` and `rg ...` reads for AGENTS, README, roadmap docs, reviews index, Stage 7.25/7.32/7.33 reports, manifest, tool README/source/tests and package metadata | Passed; required context reviewed. |
| `npm test` from `tools/openapi-conformance/` | Passed; 12 tests passed. |
| `npm run check` from `tools/openapi-conformance/` | Passed with exit code `0`; package script produced a `not_ready` report with `readinessClaim: false`. |
| `./tools/openapi-conformance/check` | Passed with exit code `0`; output kept `status: "not_ready"`, `readinessClaim: false`, `manifestValidation.status: "advisory_passed"` and `blockingFindings: []`. |
| `git diff --check` | Passed; no whitespace errors. |
| `git status --short` | Passed after edits; only expected Stage 7.34 files changed before commit. |

Backend tests were not run because backend source, backend behavior, runtime behavior, OpenAPI/API contracts and generated-client artifacts were not changed.

## 8. Remaining limitations

- Validation remains skeleton/advisory and does not replace a future generated-client readiness stage.
- Endpoint reference validation remains future-only.
- The tool does not run HTTP requests, backend runtime checks, generated-client generation or compile checks.
- `readinessCriteria` can only confirm that premature true gates are blocked; it does not prove any readiness gate passed.
- Candidate endpoint review remains separate from readiness approval.

## 9. Recommended next step

Recommended next bounded Stage 7 task: **Stage 7.35 — Manifest Endpoint Candidate Review**.

The next task should review whether current candidate/excluded endpoint classifications in `generated-client-ready-subset.yaml` are still the right non-readiness baseline, without changing OpenAPI/API contracts, backend/frontend runtime behavior, generated clients, CI/Gradle integration, generated-client readiness or Stage 8 activation.
