# Stage 7.29 — Active Documentation Language Normalization

## 1. Scope

- [x] Language normalization only
- [x] Active/navigation/rules docs only
- [x] No code changes
- [x] No roadmap sequencing changes
- [x] No product scope changes
- [x] No architecture decision changes
- [x] No roadmap structure refactor
- [x] Historical artifacts preserved

## 2. Executive summary

Stage 7.29 нормализовал ordinary English prose в active/navigation документации по Russian-first policy, закрепленной Stage 7.27. Cleanup затронул README, navigation roadmap, primary roadmap, product/architecture/development indexes и reviews index.

Изменения сохраняют смысл документов: roadmap sequencing, product scope, architecture decisions, generated-client readiness и historical audit trail не менялись. `docs/roadmap/roadmap.md` обновлен только в части фактического завершения Stage 7.29 и следующего разрешенного шага Stage 7.30.

## 3. Files changed

| File | Change type | Reason |
|---|---|---|
| `README.md` | Language normalization | Перевести navigation-only prose и descriptors вокруг документационной карты, сохранив technical terms и file paths. |
| `docs/ROADMAP.md` | Language normalization | Перевести overview/status wording без добавления detailed roadmap ledger. |
| `docs/roadmap/roadmap.md` | Language normalization and factual Stage 7.29 status update | Перевести status-heavy prose и отметить Stage 7.29 как завершенный после создания этого report; Stage 7.30 остается следующим явным шагом. |
| `docs/product/README.md` | Language normalization | Перевести обычную prose вокруг product index/source-of-truth guidance без переписывания stage artifacts. |
| `docs/architecture/README.md` | Language normalization and navigation-only status wording | Перевести обычную prose и заменить stale detailed Stage 7 duplicate ссылкой на primary roadmap, не меняя architecture decisions. |
| `docs/development/README.md` | Language normalization | Перевести active engineering rules index prose. |
| `docs/reviews/README.md` | Reviews index update and narrow language normalization | Добавить Stage 7.29 report в audit trail и нормализовать несколько текущих index фраз без переписывания historical entries. |
| `docs/reviews/stage-7-29-active-documentation-language-normalization.md` | New review report | Зафиксировать scope, changed files, terms left in English, validation and next step. |

## 4. Language normalization summary

| File | Normalized wording type | Notes |
|---|---|---|
| `README.md` | Navigation descriptors, entry-point wording, generated/file/template prose | Technical terms such as `roadmap`, `backend`, `frontend`, `implementation`, `review` and file paths were preserved where useful. |
| `docs/ROADMAP.md` | Overview/status prose | `Completed`, `Planned`, `not started` style strings were converted to Russian wording. |
| `docs/roadmap/roadmap.md` | Status tables, Stage 7 dashboard, Stage 6/7 headings, next-step wording | Status meaning was preserved; Stage 7.29 completion was recorded because this task creates the Stage 7.29 report. |
| `docs/product/README.md` | Product index prose and source-of-truth guidance | Historical product artifact inventory was not rewritten in bulk. |
| `docs/architecture/README.md` | Architecture index prose and current-status summary | Detailed Stage 7 duplicate was reduced to a primary-roadmap pointer to avoid maintaining a second status ledger. |
| `docs/development/README.md` | Active rules index prose | Short English descriptions were converted to Russian-first wording. |
| `docs/reviews/README.md` | Current cleanup context and index navigation prose | Added Stage 7.29 entries; old report role labels were preserved as audit trail. |

## 5. Terms intentionally left in English

| Term | Reason |
|---|---|
| `Stage`, `roadmap`, `source of truth` | Established project/governance terms used throughout current docs. |
| `backend`, `frontend`, `runtime`, `endpoint`, `API`, `OpenAPI` | Technical terms where translation would reduce precision. |
| `generated client`, `generated-client-ready subset`, `generated-client/OpenAPI readiness` | Established Stage 7 terminology tied to conformance/readiness work. |
| `review`, `audit`, `audit trail`, `quality gate` | Review/reporting terms used as artifact and process labels. |
| `baseline`, `carryover`, `guardrail`, `scope`, `implementation backlog` | Established governance terms used consistently in project docs. |
| `MVP`, `ADR`, `LLM`, `NFR`, `DTO`, `DB`, `CI/Gradle` | Technical/project abbreviations. |
| File paths, commands, report filenames and artifact names | Must stay exact for navigation and traceability. |
| Historical role labels such as `Completed implementation report` | Preserved in `docs/reviews/README.md` to avoid rewriting historical audit entries. |

## 6. Historical artifact protection

Historical stage/review artifacts were not edited. In particular, this cleanup did not modify:

- `docs/product/stage-*`;
- `docs/architecture/stage-*`;
- older `docs/reviews/stage-*` reports;
- historical reports such as `project-consistency-audit.md`, `documentation-refactoring-plan.md` or pre-Stage 6 review artifacts.

Only active/navigation index files and the new Stage 7.29 report were changed.

## 7. Scope protection

Roadmap sequencing was not changed. Stage 7.30 remains the next allowed documentation stabilization step and Stage 8 was not started.

Product scope was not changed. MVP v1 remains hotel-only, and flights, booking, payment, account history, provider integration, DB/storage and frontend implementation remain outside the current activated work.

Architecture decisions were not changed. Kotlin + Ktor remains the confirmed backend stack. No generated-client readiness promotion was made: generated-client/OpenAPI readiness remains not declared, generated clients remain not created, and the full conformance gate remains not implemented.

No code, runtime behavior, API contract, OpenAPI content, generated-client artifact, DB schema, provider integration or historical audit artifact was changed.

## 8. Remaining language candidates

| File | Candidate | Reason deferred |
|---|---|---|
| `docs/guides/documentation-style-guide.md` | Mixed Russian/English terminology examples and role taxonomy labels | The guide already defines which English terms are allowed. Changing examples/taxonomy would be a separate style-guide wording cleanup, not required after this factual cleanup. |
| `docs/reviews/README.md` | Historical role labels such as `Completed cleanup report`, `Review context`, `Historical audit trail` | These are audit-trail role labels for old reports; rewriting them broadly could change historical reading semantics. |
| `docs/product/README.md` | Inventory classification labels such as `Historical product stage artifact` and `Product review/audit artifact` | These labels are role taxonomy, not ordinary prose; a future taxonomy normalization can address them consistently if needed. |
| `docs/architecture/README.md` | Inventory classification labels and some historical Stage 5/6 artifact descriptions | They describe technical/historical artifacts and were left to avoid mass rewriting historical context. |

## 9. Validation

| Command | Result |
|---|---|
| `git status --short` | Passed before edits; clean working tree, no output. |
| Required `sed -n ...` reads for AGENTS, style guide, prompt docs, Stage 7.26-7.28 reports, README, roadmap docs, product/architecture/development indexes and reviews index | Passed; required context reviewed before edits. |
| `rg -n "Current|Completed|Not started|Implementation|readiness|This document|source of truth|scope|review|audit|roadmap|status|generated-client|Backend|Frontend|No .* claimed|See .* roadmap" ...` | Passed; found language-normalization candidates in allowed files. |
| Follow-up `rg` for `Completed`, `Not started`, `See`, `This document`, `Current status`, `Implementation remains`, `No .* claimed`, `readiness status`, `correct as-of` | Passed; remaining hits are mostly historical role labels in `docs/reviews/README.md`. |
| `git diff --check` | Passed; no whitespace errors reported. |
| `git status --short` | Passed; only Stage 7.29 documentation files are modified/untracked. |

Backend tests were not run because no backend code, runtime behavior, API contract or generated-client artifact was changed.

## 10. Recommended next step

Stage 7.30 — Documentation Final Quality Gate.
