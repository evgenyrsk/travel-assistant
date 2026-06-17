# Stage 7.28 — Roadmap Structure Refactor

## 1. Scope

- [x] Documentation cleanup only
- [x] No code changes
- [x] No roadmap sequencing changes
- [x] No product scope changes
- [x] No architecture decision changes
- [x] No language normalization pass
- [x] Historical artifacts preserved

## 2. Executive summary

Stage 7.28 refactored roadmap/status documentation for readability. `docs/roadmap/roadmap.md` remains the primary detailed roadmap/status source of truth, while `docs/ROADMAP.md` and `README.md` now point readers to the primary roadmap instead of duplicating long Stage 7 status strings.

The cleanup replaced long Stage 7 prose chains with compact tables and a documentation stabilization checklist. It did not change roadmap sequencing, product scope, architecture decisions, code, runtime behavior or historical audit trail.

## 3. Files changed

| File | Change type | Reason |
|---|---|---|
| `docs/roadmap/roadmap.md` | Primary roadmap structure cleanup | Replace long Stage 7 status prose with dashboard tables, grouped progress table, documentation stabilization checklist and compact exclusions. |
| `docs/ROADMAP.md` | Navigation roadmap cleanup | Keep high-level overview and remove detailed Stage 7 status ledger duplication. |
| `README.md` | Entry-point status cleanup | Replace long current-baseline duplicate with short navigation-only status and link to primary roadmap. |
| `docs/reviews/README.md` | Reviews index update | Add Stage 7.28 report to audit trail without rewriting historical entries. |
| `docs/reviews/stage-7-28-roadmap-structure-refactor.md` | New cleanup report | Record Stage 7.28 scope, files changed, validation and next step. |

## 4. Source-of-truth roles after cleanup

| File | Role | Notes |
|---|---|---|
| `docs/roadmap/roadmap.md` | Primary detailed roadmap/status source of truth | Owns stage status, Stage 7 checklist, current exclusions and next allowed step. |
| `docs/ROADMAP.md` | Navigation summary | Gives compact stage overview and points to primary roadmap for detailed status. |
| `README.md` | Repository entry point | Gives top-level status framing and points to primary roadmap without replacing it. |
| `docs/reviews/README.md` | Review/audit index | Lists Stage 7.28 report in audit trail; does not become roadmap or backlog. |
| `docs/reviews/stage-7-28-roadmap-structure-refactor.md` | Cleanup report / audit artifact | Records this cleanup only; not a roadmap source of truth. |

## 5. Roadmap readability changes

- Added a compact current status dashboard in `docs/roadmap/roadmap.md`.
- Added an area-based Stage 7 progress table.
- Added a Stage 7.26-7.30 documentation stabilization checklist:
  - [x] Stage 7.26 — Documentation Quality Calibration Audit
  - [x] Stage 7.27 — Documentation Governance Rules Cleanup
  - [x] Stage 7.28 — Roadmap Structure Refactor
  - [ ] Stage 7.29 — Active Documentation Language Normalization
  - [ ] Stage 7.30 — Documentation Final Quality Gate
- Replaced repeated Stage 7 exclusion prose with a compact current-exclusions table and core guardrail.
- Replaced the full Stage 7 report inventory in the primary roadmap with grouped artifact references and delegated the full audit trail to `docs/reviews/README.md`.
- Shortened `docs/ROADMAP.md` so it remains a navigation overview rather than a detailed status ledger.
- Shortened README's current status paragraph so README remains an entry point rather than a roadmap replacement.

## 6. Scope protection

- Roadmap sequencing was not changed.
- Product scope was not changed.
- Architecture decisions were not changed.
- Code, backend/frontend runtime, generated clients, OpenAPI readiness and conformance semantics were not changed.
- Stage 8 was not started.
- Historical stage/review artifacts were not rewritten.
- Stage 7.28 was marked complete because this task creates the Stage 7.28 cleanup report and updates the roadmap final state for this completed documentation cleanup.

## 7. Validation

| Command | Result |
|---|---|
| `git status --short` | Passed before edits; clean working tree, no output. |
| `git diff --check` | Passed; no whitespace errors reported. |
| `git status --short` | Passed; only Stage 7.28 documentation cleanup files are modified/untracked. |

Backend tests were not run because no code, runtime behavior, API contracts or backend documentation commands were changed.

## 8. Recommended next step

Stage 7.29 — Active Documentation Language Normalization.
