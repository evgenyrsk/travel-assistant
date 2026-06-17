# Stage 7.27 — Documentation Governance Rules Cleanup

## 1. Scope

- [x] Documentation-governance only
- [x] No code changes
- [x] No roadmap sequencing changes
- [x] No product scope changes
- [x] No architecture decision changes
- [x] No roadmap refactor
- [x] No mass language normalization
- [x] Historical artifacts preserved

## 2. Executive summary

Stage 7.27 усилил existing documentation governance вместо создания нового source-of-truth документа. Правила теперь строже закрепляют Russian-first active prose, роль `docs/roadmap/roadmap.md` как единственного detailed roadmap/status source of truth, checklist/table formatting для status-heavy и review/audit документов, document role discipline и поведение Codex при documentation cleanup.

Изменения не меняют roadmap sequencing, product scope, architecture decisions, code/runtime или historical audit trail. Stage 7.28 остается отдельным будущим roadmap structure refactor.

## 3. Files changed

| File | Change type | Reason |
|---|---|---|
| `AGENTS.md` | Governance rules clarified | Закрепить Russian-first active documentation policy, roadmap/status source-of-truth boundaries, checklist/table expectations, document role checks и Codex behavior for documentation cleanup. |
| `docs/guides/documentation-style-guide.md` | Style/governance rules expanded | Добавить detailed rules для Russian-first prose, document roles, roadmap/status formatting, checklist/table usage, source-of-truth protection, no beautification и Codex documentation workflow. |
| `docs/prompts/codex-rules.md` | Prompt companion guidance clarified | Добавить short reminders for document roles, Russian-first prose, source-of-truth protection, status-heavy formatting and no mixed cleanup scopes. |
| `docs/prompts/review-template.md` | Legacy review template checks expanded | Добавить review checks for document role clarity, Russian-first issues, checklist/table formatting, source-of-truth duplication and beautification without purpose. |
| `docs/reviews/README.md` | Reviews index updated | Добавить Stage 7.27 report to audit trail without changing roadmap/product/architecture status. |
| `docs/reviews/stage-7-27-documentation-governance-rules-cleanup.md` | New cleanup report | Зафиксировать scope, changes, validation and next step for Stage 7.27. |

## 4. Rules added or clarified

| Area | Rule | Location |
|---|---|---|
| Russian-first documentation | Active/source-of-truth documentation uses Russian prose by default; English is reserved for technical terms, paths, commands, APIs, libraries, classes, status labels, commit/review labels and established terms. | `AGENTS.md`, `docs/guides/documentation-style-guide.md`, `docs/prompts/codex-rules.md` |
| Mixed prose | Ordinary English prose in Russian active documentation is a readability issue unless technically necessary. | `AGENTS.md`, `docs/guides/documentation-style-guide.md`, `docs/prompts/codex-rules.md`, `docs/prompts/review-template.md` |
| Historical language protection | Historical artifacts are not mass-normalized for language or style; role labels/indexes should explain stale or historical wording. | `AGENTS.md`, `docs/guides/documentation-style-guide.md`, `docs/prompts/codex-rules.md`, `docs/prompts/review-template.md` |
| Roadmap source of truth | `docs/roadmap/roadmap.md` owns detailed roadmap/status; `docs/ROADMAP.md` stays navigation summary; `README.md` stays entry point. | `AGENTS.md`, `docs/guides/documentation-style-guide.md`, `docs/prompts/codex-rules.md` |
| Roadmap sequencing protection | Detailed status should be updated in one source-of-truth location; roadmap sequencing changes require explicit scope. | `AGENTS.md`, `docs/guides/documentation-style-guide.md` |
| Checklist/table formatting | Review/audit scope and gate criteria should use checklists; status-heavy docs should prefer compact tables/status matrices over long prose. | `AGENTS.md`, `docs/guides/documentation-style-guide.md`, `docs/prompts/review-template.md` |
| Document roles | Codex must check whether a document is source-of-truth, navigation/index, guide/rules, review/audit artifact or historical artifact before changing it. | `AGENTS.md`, `docs/guides/documentation-style-guide.md`, `docs/prompts/codex-rules.md`, `docs/prompts/review-template.md` |
| Source-of-truth protection | Do not create a new source-of-truth document when an existing source-of-truth can be updated. | `AGENTS.md`, `docs/guides/documentation-style-guide.md`, `docs/prompts/codex-rules.md`, `docs/prompts/review-template.md` |
| Codex cleanup behavior | Do not mix cleanup types, do not perform beautification without a verifiable goal, and stop on source-of-truth conflict risk. | `AGENTS.md`, `docs/guides/documentation-style-guide.md`, `docs/prompts/codex-rules.md`, `docs/prompts/review-template.md` |

## 5. Source-of-truth protection

The cleanup strengthens existing documents instead of creating a new governance source. `AGENTS.md` remains the mandatory agent entry point, while `docs/guides/documentation-style-guide.md` carries detailed documentation style and structure rules.

The new rules explicitly keep:

- `docs/roadmap/roadmap.md` as the only detailed roadmap/status source of truth;
- `docs/ROADMAP.md` as a navigation summary;
- `README.md` as repository entry point;
- `docs/reviews/**` as audit trail rather than backlog or roadmap.

The rules also require Codex to prefer updating an existing source-of-truth document over creating a new one, and to stop if a documentation change risks creating a competing source of truth.

## 6. Russian-first policy

Russian-first policy is now anchored in `AGENTS.md`, expanded in `docs/guides/documentation-style-guide.md`, and repeated as prompt-level guidance in `docs/prompts/codex-rules.md` and `docs/prompts/review-template.md`.

Allowed English remains limited to technical precision: file paths, commands, APIs, libraries, class/package names, database identifiers, status labels, commit/review labels, ADR terms and established project terms. Ordinary English prose inside Russian active docs is explicitly treated as a readability issue unless technically necessary.

Historical artifacts are protected from mass language normalization.

## 7. Checklist/table formatting policy

Checklist/table formatting rules are now explicit in `AGENTS.md` and `docs/guides/documentation-style-guide.md`, with review checks in `docs/prompts/review-template.md`.

The policy says:

- review/audit reports should use checklists for scope and gate criteria;
- each checklist item must be verifiable;
- status-heavy documents should use compact tables, status matrices or checklists;
- tables should support comparison, status, traceability or safe-to-change decisions;
- long status paragraphs should be avoided.

## 8. Historical artifact protection

The cleanup keeps historical artifacts untouched and reinforces that they are audit trail. Historical stage/review artifacts may contain old wording, old status, superseded context or mixed language. They should not be retroactively translated, restyled or normalized without separate explicit scope.

If historical wording is confusing, the preferred remedy is role labeling in an index or current source-of-truth document, not rewriting the historical artifact.

## 9. Validation

| Command | Result |
|---|---|
| `git status --short` | Passed before edits; clean working tree, no output. |
| `rg -n "markdownlint|remark|mdl|lint.*markdown|markdown.*lint" .github docs services tools README.md` | No project markdown lint/check command found; only an old review note mentions that no repository-level markdown tooling was found. |
| `git diff --check` | Passed; no whitespace errors reported. |
| `git status --short` | Passed; only Stage 7.27 documentation-governance files are modified/untracked. |

Backend tests were not run because no code, runtime behavior, API contracts or backend documentation commands were changed.

## 10. Recommended next step

Stage 7.28 — Roadmap Structure Refactor.
