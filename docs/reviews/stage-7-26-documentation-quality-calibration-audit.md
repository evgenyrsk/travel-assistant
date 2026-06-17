# Stage 7.26 — Documentation Quality Calibration Audit

## 1. Scope

- [x] Review-only
- [x] No code changes
- [x] No product scope changes
- [x] No roadmap sequencing changes
- [x] No architecture decision changes
- [x] Historical artifacts preserved

## 2. Executive verdict

Verdict: Needs targeted cleanup.

Документационная система остается управляемой: primary roadmap, product baseline, architecture baseline, AGENTS.md и reviews index задают понятную иерархию источников истины. Блокирующих конфликтов по roadmap, продукту или архитектуре не найдено.

При этом active/navigation документация снова накопила длинные статусные строки Stage 7, обычную англоязычную prose внутри русскоязычных документов и повторяющиеся guardrails между README, roadmap, AGENTS.md, prompts и development rules. Это не блокирует работу, но требует узкого cleanup трека перед следующим финальным quality gate.

## 3. Current git status

До начала работы был выполнен:

```bash
git status --short
```

Результат: clean working tree; команда не вывела строк.

## 4. Source-of-truth map

| Area | Current source of truth | Supporting docs | Notes |
|---|---|---|---|
| Roadmap status, progression, next allowed step | `docs/roadmap/roadmap.md` | `docs/ROADMAP.md`, `README.md`, `docs/reviews/README.md` | Главный источник уже назван однозначно, но Stage 7 summary слишком длинный и повторяется в navigation docs. |
| Product / MVP scope | `docs/product/product-baseline.md` | `docs/product/README.md`, `docs/product/stage-*`, `docs/PROJECT_BRIEF.md` | Hotel-only MVP boundary ясен; historical product artifacts не следует переписывать только ради языка. |
| Architecture / backend stack | `docs/architecture/architecture-baseline.md` | `docs/architecture/README.md`, `docs/architecture/backend-layering-rules.md`, `docs/ARCHITECTURE.md`, Stage 5/6 artifacts | Kotlin + Ktor authority ясен; root architecture note должен оставаться secondary. |
| Codex / AI-agent governance | `AGENTS.md` | `docs/prompts/README.md`, `docs/prompts/codex-rules.md`, `docs/prompts/codex-review-template.md`, `docs/prompts/review-template.md`, `.github/*` | Canonical role ясен; не хватает более строгой Russian-first и formatting policy для active docs. |
| Development rules | `docs/development/README.md` and linked active rules | `docs/development/documentation-guidelines.md`, `quality-gates.md`, `definition-of-done.md` | Правила полезны, но текущая language policy конфликтует с более строгой Russian-first калибровкой из этой задачи. |
| Review / audit trail | `docs/reviews/README.md` | `docs/reviews/*.md` | Role labels are strong; historical reports should stay preserved and not become cleanup backlog. |
| ADR / decisions | `docs/decisions/README.md` | `docs/architecture/stage-5/architecture-decisions-draft.md` | No accepted ADR files exist; candidates remain non-active. |

## 5. Roadmap readability findings

| File | Finding | Severity | Recommendation |
|---|---|---|---|
| `docs/roadmap/roadmap.md` | Статус Stage 7 точный, но плохо сканируется: таблица текущего состояния и раздел Stage 7 содержат длинные prose-списки завершенных подэтапов до Stage 7.25. | Major | В Stage 7.28 заменить длинную prose-цепочку компактной таблицей статусов, сгруппированной по темам: backend foundation, assistant boundaries, OpenAPI/conformance tooling, documentation/status. Детали оставить в linked reports. |
| `docs/roadmap/roadmap.md` | Completed, active, and future stages are present, but Stage 7 sub-stage completion is not expressed as a checkbox or compact matrix. | Minor | Add a Stage 7 progress checklist/table for 7.0-7.25 and a short `Not started` marker for 7.26+. |
| `README.md`, `docs/ROADMAP.md`, `docs/roadmap/roadmap.md` | Current Stage 7 status is duplicated in three places. The duplication is mostly consistent now, but any Stage 7.26+ update will require synchronized edits. | Major | Keep `docs/roadmap/roadmap.md` as the only detailed roadmap source. Reduce README and `docs/ROADMAP.md` to one-line pointers plus current top-level stage state. |
| `docs/ROADMAP.md` | Документ заявлен как navigation overview, но таблица текущего статуса все еще несет подробный Stage 7 implementation/conformance status. | Minor | Оставить таблицу, но сократить Stage 7 до `In progress; details in primary roadmap` или русскоязычного аналога. Не держать подробные exclusions в этом overview. |
| `docs/roadmap/roadmap.md` | Explicit exclusions for many Stage 7 sub-stages are valuable, but repeated long negative lists reduce readability. | Minor | Consolidate repeated exclusions into a reusable Stage 7 guardrail block plus per-stage deltas only where they differ materially. |

## 6. Russian-first documentation findings

| File | Finding | Severity | Recommendation |
|---|---|---|---|
| `AGENTS.md` | Обязательный agent entry point начинается с англоязычной prose и использует английский текст для многих governance rules. Это читаемо для агентов, но конфликтует с запрошенной Russian-first policy для active documentation. | Major | В Stage 7.27 сделать русский языком обычной prose для active governance docs, сохранив technical terms, file paths, commands, class names и established labels. |
| `docs/development/documentation-guidelines.md` | Language policy сейчас предпочитает English для development standards и engineering governance docs. Это конфликтует с более строгим принципом калибровки: active documentation должна быть Russian-first. | Major | Согласовать policy с Russian-first active docs: английский для code/API identifiers и technical terms, русский для обычной explanatory prose. |
| `docs/roadmap/roadmap.md` | Ordinary prose contains avoidable English phrases, for example `Development rule documents under...`, `All development docs must follow this roadmap`, and long mixed strings like `Minimal Kotlin + Ktor backend foundation exists...`. | Minor | Normalize surrounding prose to Russian; keep technical terms such as Kotlin, Ktor, OpenAPI, generated clients and provider as needed. |
| `README.md` | Documentation map uses many English phrases as prose: `navigation only`, `product source of truth`, `compact development reference`, `quality work`, `practical Codex review-only template`. | Minor | Convert ordinary descriptors to Russian while preserving document names and technical labels. |
| `docs/ROADMAP.md` | Таблица статусов смешивает English prose с русским, особенно в строках Stage 7 и Code/API/DB/UI implementation. | Minor | Использовать русскую prose для содержания строк, а technical terms оставлять на английском только там, где это повышает точность. |
| Historical `docs/reviews/*.md` and stage artifacts | Many historical artifacts contain English/Russian mixed wording. This is acceptable for audit trail and should not be normalized in bulk. | Note | Do not rewrite historical reports solely for language normalization; clarify role via indexes if needed. |

## 7. Documentation structure findings

| File | Finding | Severity | Recommendation |
|---|---|---|---|
| `docs/roadmap/roadmap.md` | The document serves both as source-of-truth roadmap and detailed Stage 7 audit index. That makes the current active status harder to find. | Major | Split within the same file: short status dashboard first, detailed stage history later, with Stage 7 details table/checklist. Do not create a new roadmap source. |
| `README.md` | The documentation map is useful but dense; it combines navigation, role labels, source-of-truth hints and status hints in one long list. | Minor | Keep README navigational; move detailed role explanations to relevant indexes and keep concise Russian labels. |
| `docs/reviews/README.md` | Reviews index сильный, но список `Current/latest документы` уже достаточно длинный, чтобы становиться maintenance burden после каждого micro-stage. | Minor | Сгруппировать Stage 7 reports по темам вместо единого ordered list на 40+ пунктов. |
| `docs/guides/documentation-style-guide.md` | The guide has useful rules for language and safety, but it does not yet mandate concrete table/checklist patterns for roadmap status updates. | Minor | Add a small formatting section: when to use status tables, checkbox lists, and grouped sub-stage matrices. |
| `docs/prompts/review-template.md` and `docs/prompts/codex-review-template.md` | Оба template задают review expectations; роли ясны, но legacy template имеет другой final-report shape относительно current governance. | Note | Сохранить legacy compatibility, но в Stage 7.27 уточнить, что `codex-review-template.md` предпочтителен для новых review-only tasks. |

## 8. Duplication and redundancy findings

| Files | Duplication risk | Severity | Recommendation |
|---|---|---|---|
| `README.md`, `docs/ROADMAP.md`, `docs/roadmap/roadmap.md` | Stage 7 status, next step, and exclusions are repeated in three active/navigation places. | Major | Keep full status only in primary roadmap; navigation docs should summarize and link. |
| `AGENTS.md`, `docs/development/documentation-guidelines.md`, `docs/guides/documentation-style-guide.md` | Language policy and documentation safety rules appear in multiple places with slightly different emphasis. | Major | Make AGENTS.md point to the style guide for detailed documentation language rules; keep only critical guardrails in AGENTS.md. |
| `AGENTS.md`, `docs/prompts/*`, `.github/*` | Scope control, roadmap control, validation and final report instructions are repeated across agent and prompt templates. | Minor | Keep AGENTS.md canonical and make templates shorter task scaffolds with links. |
| `docs/reviews/README.md`, `docs/roadmap/roadmap.md` | Stage 7 audit/report inventory appears in both places. | Minor | Roadmap should list only key artifacts or grouped report families; reviews index should own full audit trail. |
| `docs/product/product-baseline.md`, `docs/roadmap/roadmap.md`, `README.md` | MVP scope and explicit exclusions are repeated. | Note | Keep a short reminder outside product baseline, but avoid detailed restatement unless status changes. |

## 9. Codex instruction gaps

| Rule gap | Current location | Recommended change |
|---|---|---|
| Russian-first active documentation rule is not strict enough and conflicts with existing English-preferred governance/development wording. | `AGENTS.md`, `docs/development/documentation-guidelines.md`, `docs/guides/documentation-style-guide.md` | State that active project documentation prose should be Russian-first, with English reserved for code identifiers, paths, commands, APIs, libraries, class names, status labels and established technical terms. |
| Roadmap update formatting is not prescriptive enough. | `AGENTS.md`, `docs/guides/documentation-style-guide.md` | Add guidance that Stage status should use compact tables/checklists and that long sub-stage prose chains should be avoided. |
| No explicit "do not create a new document if an existing source-of-truth can be updated" rule. | `AGENTS.md`, `docs/development/documentation-guidelines.md` | Add a rule: prefer updating the current source-of-truth or index when role/status changes; create new docs only for explicit reports, artifacts or accepted roadmap deliverables. |
| Historical artifact preservation exists, but language-normalization boundaries could be sharper. | `AGENTS.md`, `docs/guides/documentation-style-guide.md`, `docs/reviews/README.md` | Clarify that historical artifacts should not be mass-translated or stylistically rewritten; use role labels and current indexes instead. |
| Review-only reporting rules do not require explicit "recommendations not implemented" in all templates. | `docs/prompts/review-template.md`, `docs/prompts/codex-review-template.md` | Align review templates with AGENTS.md final reporting expectations or clearly mark the template-specific override. |
| Checkbox/table formatting expectations are only general writing advice. | `docs/guides/documentation-style-guide.md` | Add examples for roadmap status tables, cleanup checklists, and safe-to-change matrices. |

## 10. Recommended stabilization track

### Stage 7.27 — Documentation Governance Rules Cleanup

- Normalize `AGENTS.md`, `docs/development/documentation-guidelines.md` and `docs/guides/documentation-style-guide.md` around Russian-first active prose.
- Add explicit rules for roadmap status tables, checkbox/checklist usage and avoiding long prose status chains.
- Add the rule "do not create a new source-of-truth document if an existing source-of-truth can be updated."
- Clarify that historical artifacts are not targets for mass language normalization.
- Keep edits narrow; do not update roadmap status, product scope or architecture decisions.

### Stage 7.28 — Roadmap Structure Refactor

- Keep `docs/roadmap/roadmap.md` as the only detailed roadmap source.
- Convert Stage 7 history into grouped tables/checklists.
- Shorten README and `docs/ROADMAP.md` status duplication to navigation-level summaries.
- Keep Stage 7.26+ marked as not started unless a separate task changes status.
- Avoid creating a second roadmap or task tracker.

### Stage 7.29 — Active Documentation Language Normalization

- Normalize active/navigation prose in `README.md`, `docs/ROADMAP.md`, `docs/roadmap/roadmap.md`, `AGENTS.md`, `docs/development/documentation-guidelines.md` and selected index files.
- Preserve technical terms such as `OpenAPI`, `provider`, `backend`, `frontend`, `generated clients`, `quality gate`, `MVP`, `ADR`, `Kotlin`, `Ktor`.
- Do not rewrite historical stage artifacts or old review reports.
- Do not change facts, statuses, roadmap sequence, product requirements or architecture decisions.

### Stage 7.30 — Documentation Final Quality Gate

- Re-check source-of-truth hierarchy after governance and roadmap cleanup.
- Verify no active doc competes with `docs/roadmap/roadmap.md`.
- Verify Russian-first prose in active docs.
- Verify historical audit trail remains preserved.
- Run `git diff --check` and manually check changed links.

## 11. Safe-to-change / do-not-change matrix

| Category | Files | Recommendation |
|---|---|---|
| Safe to change now | `AGENTS.md`, `docs/development/documentation-guidelines.md`, `docs/guides/documentation-style-guide.md`, `docs/prompts/codex-review-template.md`, `docs/prompts/review-template.md` | Narrow governance/style cleanup only; do not alter roadmap status or implementation rules beyond documentation process clarity. |
| Safe to change now | `README.md`, `docs/ROADMAP.md` | Navigation/status-summary cleanup only; keep them subordinate to `docs/roadmap/roadmap.md`. |
| Change only with narrow scope | `docs/roadmap/roadmap.md` | Structure/readability refactor is appropriate, but must preserve roadmap sequencing, current statuses and next-step rules. |
| Change only with narrow scope | `docs/reviews/README.md` | Update only when adding discoverability or role labels; avoid turning review reports into backlog. |
| Change only with narrow scope | `docs/product/product-baseline.md`, `docs/architecture/architecture-baseline.md` | Update only if a task explicitly changes product/architecture baseline or asks for active-doc language cleanup. |
| Do not change / historical audit trail | `docs/product/stage-*`, `docs/architecture/stage-*`, older `docs/reviews/*.md` | Preserve as historical context. Do not mass-translate, restyle or update old verdicts/status wording. |
| Do not change / historical audit trail | `docs/reviews/documentation-refactoring-plan.md`, pre-Stage 6 and Stage 7.0 reports | Keep as audit trail unless a dedicated archival-labeling task updates indexes. |

## 12. Commands/checks run

| Command | Result |
|---|---|
| `git status --short` | Passed; clean working tree before audit, no output. |
| `sed -n ...` / `nl -ba ...` reads for required and relevant context files | Passed; read AGENTS, README, roadmap docs, prompt/review docs, style guide, reviews index and relevant development/baseline/review context. |
| `git diff --check` | Passed; no whitespace errors reported. |
| `git status --short` | Passed; only `?? docs/reviews/stage-7-26-documentation-quality-calibration-audit.md` reported after creating this review report. |

Backend tests were not run because this was a documentation-only review report and no backend code, contracts or documented runtime behavior were changed.

## 13. Final recommendation

Следующий безопасный шаг: выполнить Stage 7.27 как narrow documentation governance cleanup. Этот этап должен сначала согласовать Russian-first policy, roadmap formatting rules и historical artifact protection в `AGENTS.md`, `docs/development/documentation-guidelines.md` и `docs/guides/documentation-style-guide.md`, а затем оставить roadmap restructuring и language normalization для отдельных bounded stages.
