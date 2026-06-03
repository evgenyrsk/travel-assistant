# Аудит избыточности и структуры документации

Дата аудита: 2026-06-03

## 1. Цель аудита

Проверить, является ли документационная система Travel Assistant чистой, неизбыточной, читаемой и безопасной для дальнейших roadmap-aligned задач.

Аудит является review-only. Он не меняет roadmap status, architecture decisions, MVP scope, backend/frontend implementation, OpenAPI, ADR, структуру файлов или существующие документы.

Проверялись:

- корневая документация: `README.md`, `AGENTS.md`;
- roadmap: `docs/roadmap/roadmap.md`, `docs/ROADMAP.md`;
- продуктовая документация: `docs/product/**`, `docs/PROJECT_BRIEF.md`;
- архитектурная документация: `docs/architecture/**`, `docs/ARCHITECTURE.md`;
- development-документация: `docs/development/**`;
- prompt/governance-документация: `docs/prompts/**`, `.github/**`;
- review/audit-документация: `docs/reviews/**`;
- documentation governance: `docs/guides/documentation-style-guide.md`;
- index decisions/ADR: `docs/decisions/README.md`;
- соседняя локальная implementation-документация: `services/backend/README.md`, только как referenced implementation-local note.

## 2. Общая оценка документационной структуры

Документация не является хаотичной: в репозитории уже есть явная иерархия источников истины, compact baselines, stage artifacts, review trail и guardrails для Codex. Это сильная сторона проекта.

Но документация пока не является чистой и неизбыточной. Главная проблема не в отсутствии документов, а в избытке пересекающихся слоев:

- текущий статус Stage 7 расходится между primary roadmap/navigation docs и более свежими review/status cleanup artifacts;
- roadmap, README, indexes, AGENTS.md, prompts и development docs повторяют одни и те же guardrails;
- product baseline, product stage docs и `docs/PROJECT_BRIEF.md` частично повторяют MVP scope;
- architecture baseline, root architecture note, architecture README и Stage 5 artifacts повторяют architecture boundaries;
- development roadmap, milestones и implementation strategy описывают похожую будущую реализацию разными формами;
- reviews сохраняют полезный audit trail, но часть старых review artifacts содержит устаревшие Stage 6 statuses без явной archival маркировки.

Итог: структура уже защищает проект от грубого scope drift, но требует controlled cleanup перед тем, как ее можно назвать clean/non-redundant.

## 3. Полный inventory документации

| Файл | Роль | Оценка |
|---|---|---|
| `README.md` | navigation/index, root entry point | Полезен, но перегружен картой документации и содержит stale Stage 7 wording про блокировку до restart readiness review. |
| `AGENTS.md` | prompt/governance guidance, source of operational rules for Codex | Критически полезен, но сильно дублирует `docs/prompts/codex-rules.md`, task/review templates и roadmap guardrails. |
| `.github/ISSUE_TEMPLATE/codex_task.yml` | prompt/governance guidance, GitHub workflow template | Полезен; частично дублирует task template и AGENTS.md. |
| `.github/pull_request_template.md` | governance/checklist template | Полезен; частично дублирует review template и AGENTS.md. |
| `docs/ROADMAP.md` | navigation/index, high-level roadmap overview | Полезен как overview, но дублирует primary roadmap и содержит stale Stage 7 restart wording. |
| `docs/roadmap/roadmap.md` | source of truth по stage status/progression | Главный source of truth, но сейчас содержит stale status относительно завершенного Stage 7 restart readiness review; также перегружен historical detail. |
| `docs/PROJECT_BRIEF.md` | product context / compact brief | Полезен как high-level product context; частично дублирует product baseline и содержит full-product future scope рядом с MVP scope. |
| `docs/ARCHITECTURE.md` | preliminary architecture note | Полезен как historical/preliminary stack note, но пересекается с architecture baseline и должен читаться как secondary. |
| `docs/guides/documentation-style-guide.md` | documentation guidance / governance baseline | Полезен, но содержит stale Stage 6 `Planned / not started` wording в текущем guidance-документе. |
| `docs/decisions/README.md` | decisions/ADR index and governance | Полезен; standalone ADR отсутствуют, роль ясная. |
| `docs/product/README.md` | product navigation/index | Полезен; длинный, частично повторяет stage summaries и architecture index. |
| `docs/product/product-baseline.md` | baseline, current product/MVP scope | Полезный compact baseline; после status cleanup актуальнее старых product stage docs. |
| `docs/product/stage-0/product-framing.md` | historical stage artifact | Оставить historical; широкий product framing полезен для traceability. |
| `docs/product/stage-0/initial-scenarios.md` | historical stage artifact | Оставить historical; содержит flight/combined context, не active MVP. |
| `docs/product/stage-0/mvp-boundaries.md` | historical stage artifact | Оставить historical; ранние MVP boundaries superseded product baseline. |
| `docs/product/stage-0/assumptions-and-open-questions.md` | historical stage artifact | Оставить historical; часть вопросов superseded или carried over. |
| `docs/product/stage-1/target-audience.md` | historical stage artifact | Оставить historical; часть future travel context не active MVP. |
| `docs/product/stage-1/business-scenarios.md` | historical stage artifact | Оставить historical; flight/combined scenarios являются future context. |
| `docs/product/stage-1/user-journeys.md` | historical stage artifact | Оставить historical; содержит future journeys. |
| `docs/product/stage-1/business-requirements.md` | historical stage artifact | Оставить historical; некоторые old MVP labels superseded. |
| `docs/product/stage-1/functional-requirements.md` | historical stage artifact | Оставить historical; old flight/combined MVP labels superseded. |
| `docs/product/stage-1/non-functional-requirements.md` | historical stage artifact | Оставить historical; частично superseded by architecture NFR baseline. |
| `docs/product/stage-1/assumptions-and-open-questions.md` | historical stage artifact | Оставить historical; часть assumptions/outstanding questions перенесена. |
| `docs/product/stage-1/stage-1-summary.md` | historical summary / audit trail | Полезен для stage traceability; не current baseline. |
| `docs/product/stage-1/stage-1-consistency-review.md` | review/audit artifact | Оставить historical; findings могут быть superseded later. |
| `docs/product/stage-2/use-cases.md` | historical stage artifact | Полезен, но очень большой; содержит future flight/combined use cases. |
| `docs/product/stage-2/edge-cases.md` | historical stage artifact | Полезен; часть edge cases future-scope. |
| `docs/product/stage-2/assistant-behaviour-rules.md` | product guidance / historical artifact | Полезен; может быть referenced by baseline, но не prompt implementation. |
| `docs/product/stage-2/combined-search-levels.md` | historical/superseded artifact | Кандидат на archival labeling; active MVP не включает combined search. |
| `docs/product/stage-2/data-requirements.md` | product data guidance / historical artifact | Полезен; не DB/API schema. |
| `docs/product/stage-2/stage-2-summary.md` | historical summary | Полезен для traceability; не current baseline. |
| `docs/product/stage-2/stage-2-consistency-review.md` | review/audit artifact | Оставить historical; содержит documentation-structure findings. |
| `docs/product/stage-3/screen-map.md` | product/UX stage artifact | Полезен; часть future screens не active MVP. |
| `docs/product/stage-3/required-fields-and-acceptance-criteria.md` | product/UX baseline detail | Полезен для Stage 7, но должен читаться через product baseline. |
| `docs/product/stage-3/mvp-search-flow-details.md` | product/UX baseline detail | Полезен для hotel flow; большой и частично overlapping with Stage 3 summary. |
| `docs/product/stage-3/combined-search-ux-decision.md` | historical/superseded decision artifact | Кандидат на archival labeling; не active MVP decision. |
| `docs/product/stage-3/stage-3-hotel-only-consistency-review.md` | review/audit artifact | Оставить historical; подтверждает hotel-only refocus. |
| `docs/product/stage-3/stage-3-summary-and-carryover.md` | historical summary / UX baseline detail | Полезен; частично superseded compact product baseline. |
| `docs/product/stage-3/stage-3-plan-reconciliation.md` | review/audit artifact | Полезен как completion audit; не roadmap. |
| `docs/product/stage-4/visual-design-direction.md` | product/design stage artifact | Полезен как design direction; не implementation tokens. |
| `docs/product/stage-4/design-system-foundations.md` | product/design stage artifact | Полезен, но может выглядеть как design token implementation. |
| `docs/product/stage-4/component-inventory.md` | product/design stage artifact | Полезен, но может выглядеть как frontend component backlog. |
| `docs/product/stage-4/screen-specifications.md` | product/design stage artifact | Полезен; future flight/combined screens требуют historical/future labeling. |
| `docs/product/stage-4/interaction-patterns.md` | product/design stage artifact | Полезен для UX implementation tasks; пересекается с behaviour rules and search flow details. |
| `docs/product/stage-4/stage-4-summary-and-carryover.md` | historical summary | Полезен; не current baseline. |
| `docs/product/stage-4/stage-4-consistency-review.md` | review/audit artifact | Оставить historical; подтверждает design scope. |
| `docs/architecture/README.md` | architecture navigation/index | Полезен, но stale Stage 7 restart wording; дублирует baseline guardrails. |
| `docs/architecture/architecture-baseline.md` | baseline, current architecture | Ключевой architecture source; полезен и должен остаться. |
| `docs/architecture/stage-5/architecture-scope-and-principles.md` | historical architecture artifact | Оставить; detailed source для Stage 5 baseline. |
| `docs/architecture/stage-5/system-context-and-boundaries.md` | historical architecture artifact | Оставить; detailed context. |
| `docs/architecture/stage-5/domain-model-and-boundaries.md` | historical architecture artifact | Оставить; detailed conceptual domain. |
| `docs/architecture/stage-5/application-orchestration.md` | historical architecture artifact | Оставить; полезная детализация, но не state-machine implementation. |
| `docs/architecture/stage-5/integration-architecture.md` | historical architecture artifact | Оставить; provider/LLM boundary details. |
| `docs/architecture/stage-5/data-and-storage-boundaries.md` | historical architecture artifact | Оставить; conceptual storage boundary, not DB schema. |
| `docs/architecture/stage-5/non-functional-requirements.md` | historical architecture artifact | Оставить; architecture quality attributes, not active backlog. |
| `docs/architecture/stage-5/architecture-decisions-draft.md` | non-ADR decision inventory / future ADR candidates | Оставить, но role labeling важен; это не accepted ADR. |
| `docs/architecture/stage-5/stage-5-consistency-review.md` | review/audit artifact | Оставить historical. |
| `docs/architecture/stage-5/stage-5-summary-and-carryover.md` | historical summary | Оставить; частично superseded by architecture baseline. |
| `docs/architecture/stage-6/openapi-draft.yaml` | Stage 6 contract artifact | Оставить; primary Stage 6 OpenAPI draft, не provider contract. |
| `docs/architecture/stage-6/openapi-contract-notes.md` | contract notes / stage artifact | Оставить; title says Stage 6.8, но роль файла шире, есть minor naming confusion. |
| `docs/architecture/stage-6/openapi-contract-review.md` | review/audit artifact | Оставить historical. |
| `docs/architecture/stage-6/openapi-fixes-summary.md` | review/fix summary artifact | Оставить historical. |
| `docs/architecture/stage-6/post-fix-contract-review.md` | review/audit artifact | Оставить historical. |
| `docs/architecture/stage-6/provider-boundary-mapping-notes.md` | architecture/contract boundary notes | Оставить; useful handoff, не provider DTO mapping contract. |
| `docs/architecture/stage-6/stage-6-completion-review.md` | review/audit artifact | Оставить historical. |
| `docs/architecture/stage-6/pre-implementation-decisions-cleanup.md` | cleanup/handoff artifact | Оставить historical; details decisions before implementation. |
| `docs/architecture/stage-6/stage-6-final-closure-and-handoff.md` | final closure/handoff artifact | Оставить; полезный audit trail. |
| `docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md` | review/audit artifact, stale implementation review | Оставить historical, но явно промаркировать: old Java/Spring skeleton review superseded by correction. |
| `docs/development/roadmap.md` | development guidance / future reference | Полезен, но пересекается с milestones and implementation strategy; выглядит как backlog. |
| `docs/development/milestones.md` | development guidance / future reference checkpoints | Полезен, но пересекается с development roadmap; выглядит как backlog. |
| `docs/development/implementation-strategy.md` | development guidance / process | Полезен; пересекается с AGENTS.md and prompt templates. |
| `docs/prompts/codex-rules.md` | prompt/governance guidance | Полезен, но дублирует AGENTS.md; может быть сокращен до reusable delta/template rules. |
| `docs/prompts/task-template.md` | prompt template | Полезен; final report format differs from AGENTS.md full required format. |
| `docs/prompts/review-template.md` | review prompt template | Полезен; final report format differs from AGENTS.md full required format. |
| `docs/reviews/pre-stage-6-documentation-consistency-review.md` | review/audit artifact | Оставить historical; contains stale Stage 6 status by design. |
| `docs/reviews/roadmap-structure-and-process-fitness-review.md` | review/audit artifact | Оставить historical; contains stale Stage 6 status by design. |
| `docs/reviews/global-documentation-quality-review.md` | review/audit artifact | Оставить historical; many findings still relevant, но status stale. |
| `docs/reviews/documentation-refactoring-plan.md` | cleanup plan / audit artifact | Полезен, но stale after later baseline/docs work; candidate to supersede/archival label, not delete first. |
| `docs/reviews/project-consistency-audit.md` | review/audit artifact | Оставить historical; identified backend stack blocker. |
| `docs/reviews/backend-stack-decision-sync.md` | review/handoff artifact | Оставить historical; stack decision sync. |
| `docs/reviews/backend-skeleton-correction.md` | review/handoff artifact | Оставить historical; correction report. |
| `docs/reviews/stage-7-restart-readiness-review.md` | review/readiness gate | Полезен и относительно актуален; says Stage 7 may restart with minor notes. |
| `docs/reviews/product-baseline-status-cleanup.md` | cleanup report | Оставить historical; confirms product baseline status fix. |
| `services/backend/README.md` | implementation-local documentation | Полезен; находится вне core docs tree, но referenced by README/architecture index. |

## 4. Классификация файлов по роли

### Source of truth

- `docs/roadmap/roadmap.md` — statuses, progression, stage boundaries, carryover и next allowed step.
- `docs/product/product-baseline.md` — compact current product/MVP scope.
- `docs/architecture/architecture-baseline.md` — compact current architecture baseline и backend stack authority.
- `AGENTS.md` — operational rules for Codex в этом репозитории.

### Baseline

- `docs/product/product-baseline.md`.
- `docs/architecture/architecture-baseline.md`.
- `docs/guides/documentation-style-guide.md`, с caveat: stale Stage 6 examples/status wording нужно обновить.
- `docs/decisions/README.md`, как decision/ADR index baseline, не как accepted ADR.

### Navigation / index

- `README.md`.
- `docs/ROADMAP.md`.
- `docs/product/README.md`.
- `docs/architecture/README.md`.
- `docs/decisions/README.md`.
- `.github/ISSUE_TEMPLATE/codex_task.yml` and `.github/pull_request_template.md`, as workflow entry templates.

### Historical stage artifacts

- `docs/product/stage-0/**`.
- `docs/product/stage-1/**`.
- `docs/product/stage-2/**`.
- `docs/product/stage-3/**`, except the parts that still serve as detailed UX baseline.
- `docs/product/stage-4/**`.
- `docs/architecture/stage-5/**`.
- `docs/architecture/stage-6/**`.
- `docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md`.

### Review / audit artifacts

- `docs/product/**/stage-*-consistency-review.md`.
- `docs/product/stage-3/stage-3-plan-reconciliation.md`.
- `docs/architecture/stage-5/stage-5-consistency-review.md`.
- `docs/architecture/stage-6/*review*.md`.
- `docs/reviews/**`.

### Development guidance

- `docs/development/roadmap.md`.
- `docs/development/milestones.md`.
- `docs/development/implementation-strategy.md`.
- `services/backend/README.md`, local to backend skeleton.

### Prompt / governance guidance

- `AGENTS.md`.
- `docs/prompts/codex-rules.md`.
- `docs/prompts/task-template.md`.
- `docs/prompts/review-template.md`.
- `.github/ISSUE_TEMPLATE/codex_task.yml`.
- `.github/pull_request_template.md`.
- `docs/guides/documentation-style-guide.md`.

### Duplicated / overlapping

- `docs/roadmap/roadmap.md` и `docs/ROADMAP.md`.
- `README.md`, `docs/product/README.md`, `docs/architecture/README.md`.
- `docs/product/product-baseline.md`, `docs/PROJECT_BRIEF.md`, Stage 3/4 summaries.
- `docs/architecture/architecture-baseline.md`, `docs/ARCHITECTURE.md`, `docs/architecture/README.md`, Stage 5 summary.
- `docs/development/roadmap.md`, `docs/development/milestones.md`, `docs/development/implementation-strategy.md`.
- `AGENTS.md`, `docs/prompts/*`, `.github/*`.
- старые review artifacts и более новые cleanup/readiness reviews.

### Stale

- `docs/roadmap/roadmap.md`, `README.md`, `docs/ROADMAP.md`, `docs/architecture/README.md`, `docs/development/*.md`: still say Stage 7 implementation is blocked until restart readiness review, хотя `docs/reviews/stage-7-restart-readiness-review.md` уже существует и прошел с minor notes, а `docs/reviews/product-baseline-status-cleanup.md` уже был выполнен после него.
- `docs/guides/documentation-style-guide.md`: still says Stage 6 remains `Planned / not started` in current style guidance.
- старые review artifacts до Stage 6 естественно содержат stale historical statuses; как история они не ошибочны, но требуют более ясного archival framing.
- `docs/reviews/documentation-refactoring-plan.md`: still frames cleanup as before Stage 6 и говорит, что product/architecture baseline docs are proposed/not created, хотя они уже существуют.

### Redundant / unclear purpose / cleanup candidates

- `docs/ROADMAP.md` — keep as overview, but shorten to avoid competing with primary roadmap.
- `docs/PROJECT_BRIEF.md` — keep as brief, but avoid duplicating current baseline details.
- `docs/ARCHITECTURE.md` — keep as preliminary/root note, but mark as secondary to architecture baseline.
- `docs/development/milestones.md` — candidate to merge/shorten with development roadmap.
- `docs/prompts/codex-rules.md` — candidate to shorten as reusable prompt delta if AGENTS.md remains canonical.
- `docs/reviews/documentation-refactoring-plan.md` — candidate for archival/superseded labeling after this audit.

## 5. Найденные дублирования

### Roadmap duplicated outside primary roadmap

- `docs/ROADMAP.md` repeats stage list, current status and exclusions.
- `README.md` repeats current baseline and next-step status.
- `docs/development/roadmap.md` repeats future stages and sequencing.
- `docs/development/milestones.md` repeats implementation sequence as milestones.
- prompt templates and GitHub templates repeat roadmap guardrails.

Impact: any status update now requires edits in many places; drift already exists around Stage 7 restart readiness.

### Architecture duplicated outside architecture baseline

- `docs/ARCHITECTURE.md` repeats stack hypotheses and boundaries.
- `docs/architecture/README.md` repeats architecture baseline summary and guardrails.
- Stage 5 summary repeats baseline boundaries now captured in `architecture-baseline.md`.
- development docs repeat stack and layer guidance.

Impact: architecture baseline is clear, but multiple secondary summaries increase maintenance cost.

### Product requirements duplicated outside product baseline

- `docs/PROJECT_BRIEF.md`, `docs/product/product-baseline.md`, Stage 3 summary, Stage 4 summary and roadmap MVP scope all describe hotel-only MVP and exclusions.
- Stage 0-2 documents preserve broader flight/combined context with top-level notes.

Impact: product baseline is usable, but a new reader can still over-read historical flight/combined artifacts.

### Codex rules duplicated across AGENTS.md and docs/prompts

- `AGENTS.md`, `docs/prompts/codex-rules.md`, `docs/prompts/task-template.md`, `docs/prompts/review-template.md`, `.github/ISSUE_TEMPLATE/codex_task.yml`, `.github/pull_request_template.md` repeat:
  - primary roadmap rule;
  - no future-stage implementation rule;
  - backend stack Kotlin + Ktor rule;
  - no Java/Spring Boot rule;
  - no API/OpenAPI/DB/storage/auth/security/DevOps/testing backlog without activation;
  - final report requirements.

Impact: useful safety, but high drift risk. Final report formats already differ between AGENTS.md and prompt templates.

### Development guidance duplicated across docs/development

- `roadmap.md` gives future implementation sequence.
- `milestones.md` gives similar sequence as checkpoints.
- `implementation-strategy.md` gives task sizing, roles, DoD and another sequence.

Impact: the three docs are individually useful but collectively backlog-like and overlapping.

### Reviews duplicate findings and cleanup plans

- `pre-stage-6-documentation-consistency-review.md`, `roadmap-structure-and-process-fitness-review.md`, `global-documentation-quality-review.md`, `documentation-refactoring-plan.md` and this audit all discuss redundancy, source-of-truth hierarchy and cleanup.

Impact: old findings are valuable as audit trail, but without review index/archival labels they can look like current action plans.

## 6. Избыточные или спорные документы

| Файл | Почему спорный | Рекомендация |
|---|---|---|
| `docs/ROADMAP.md` | Дублирует primary roadmap and status. | Оставить как очень короткий overview; убрать detailed status duplication в future cleanup. |
| `docs/PROJECT_BRIEF.md` | Повторяет product baseline and future scope. | Оставить как short product brief; для current MVP опираться на product baseline. |
| `docs/ARCHITECTURE.md` | Повторяет architecture baseline после Stage 5/7.0b. | Пометить как preliminary/root architecture note; не использовать как current architecture authority. |
| `docs/development/milestones.md` | Пересекается с development roadmap и выглядит как backlog. | Merge/shorten with `docs/development/roadmap.md` или сделать compact checkpoint index. |
| `docs/prompts/codex-rules.md` | Почти полностью дублирует AGENTS.md по governance intent. | Оставить только reusable prompt rules или явно отметить AGENTS.md как canonical. |
| `docs/reviews/documentation-refactoring-plan.md` | Частично superseded by later baseline docs and this audit. | Оставить historical, добавить superseded/archival label в future cleanup. |
| `docs/product/stage-2/combined-search-levels.md` | Active MVP больше не включает combined search. | Оставить historical; добавить stronger archival/future-scope label, если текущего недостаточно. |
| `docs/product/stage-3/combined-search-ux-decision.md` | Superseded for MVP v1. | Оставить historical; ярче пометить как superseded в index/role table. |
| `docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md` | Reviews old Java/Spring skeleton superseded by correction. | Оставить historical; пометить как superseded by backend skeleton correction. |

## 7. Устаревшие документы

### Текущие документы со stale wording

- `docs/roadmap/roadmap.md` — still says restart readiness review must be selected and Stage 7 implementation is blocked until it, хотя `docs/reviews/stage-7-restart-readiness-review.md` уже passed.
- `README.md` — same stale Stage 7 blocking wording.
- `docs/ROADMAP.md` — same stale Stage 7 blocking wording.
- `docs/architecture/README.md` — same stale Stage 7 blocking wording.
- `docs/development/roadmap.md`, `docs/development/milestones.md`, `docs/development/implementation-strategy.md` — same stale Stage 7 blocking wording.
- `docs/guides/documentation-style-guide.md` — stale Stage 6 `Planned / not started` wording in a current guidance document.

### Historical docs с ожидаемым stale context

Их не нужно переписывать как current docs, но им нужны role labels:

- pre-Stage 6 reviews in `docs/reviews/`;
- `docs/reviews/documentation-refactoring-plan.md`;
- Stage 0-2 product docs with broader flight/combined scope;
- `docs/product/stage-2/combined-search-levels.md`;
- `docs/product/stage-3/combined-search-ux-decision.md`;
- `docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md`.

## 8. Документы, которые нужно оставить historical

Оставить как audit trail, не удалять в первом cleanup pass:

- все `docs/product/stage-0/**`;
- все `docs/product/stage-1/**`;
- все `docs/product/stage-2/**`;
- `docs/product/stage-3/combined-search-ux-decision.md`;
- `docs/product/stage-3/stage-3-hotel-only-consistency-review.md`;
- `docs/product/stage-3/stage-3-plan-reconciliation.md`;
- `docs/product/stage-4/stage-4-consistency-review.md`;
- все `docs/architecture/stage-5/**`;
- все `docs/architecture/stage-6/**`;
- `docs/architecture/stage-7/stage-7-1-backend-skeleton-review.md`;
- все существующие `docs/reviews/**`, including older review artifacts and correction reports.

Причина: эти файлы объясняют, почему существуют текущие MVP and architecture boundaries. Их удаление снизит traceability и усложнит будущие спорные решения.

## 9. Документы, которые стоит объединить

### Development docs

Рекомендуемое целевое состояние:

- оставить `docs/development/implementation-strategy.md` как process/how-to-work guide;
- merge or shorten `docs/development/roadmap.md` and `docs/development/milestones.md` into one future implementation reference;
- убрать backlog-like duplication из одного из них.

### Codex/governance prompts

Рекомендуемое целевое состояние:

- оставить `AGENTS.md` как canonical operational rule source;
- оставить `docs/prompts/task-template.md` and `docs/prompts/review-template.md` as templates;
- сократить `docs/prompts/codex-rules.md`, чтобы он ссылался на AGENTS.md и содержал только reusable prompt snippets;
- выровнять final report sections across AGENTS.md, prompt templates and GitHub issue template.

### Roadmap/navigation

Рекомендуемое целевое состояние:

- оставить `docs/roadmap/roadmap.md` as primary source;
- оставить `docs/ROADMAP.md` as one-screen stage overview;
- avoid duplicating next-step status and detailed exclusions in `docs/ROADMAP.md` and README.

### Reviews

Рекомендуемое целевое состояние:

- позже добавить `docs/reviews/README.md` index;
- классифицировать reviews как current gate, historical audit, superseded plan or correction report;
- пометить `documentation-refactoring-plan.md` как superseded by later work and this audit, если этот аудит станет current cleanup guide.

## 10. Документы, которые потенциально можно удалить

Удаление не должно быть следующим шагом. Более безопасный порядок: label, shorten/merge, и только потом delete после проверки links and traceability.

Потенциальные future deletion candidates после merge/archival labeling:

- `docs/development/milestones.md`, если его unique checkpoint value merged into `docs/development/roadmap.md` or `implementation-strategy.md`.
- `docs/prompts/codex-rules.md`, если AGENTS.md remains canonical and templates cover task/review usage.
- `docs/reviews/documentation-refactoring-plan.md`, только если его historical value полностью superseded by this audit and a future cleanup plan; более безопасная рекомендация — archival label, not deletion.
- `docs/ROADMAP.md`, только если README and primary roadmap become sufficiently navigable; current recommendation is shortening, not deletion.

Не удалять в ближайшем cleanup:

- product stage artifacts;
- architecture stage artifacts;
- Stage 6 OpenAPI/contract artifacts;
- backend stack correction/readiness reviews;
- `docs/decisions/README.md`;
- current product and architecture baselines.

## 11. Рекомендуемая целевая структура

```text
README.md
AGENTS.md

docs/
  ROADMAP.md                         # короткий navigation overview only
  PROJECT_BRIEF.md                   # короткий product brief only
  ARCHITECTURE.md                    # preliminary/root architecture note, secondary

  roadmap/
    roadmap.md                       # primary source of truth for status/progression

  product/
    README.md                        # product index with role labels
    product-baseline.md              # current product/MVP baseline
    stage-0/ ... stage-4/            # historical stage artifacts

  architecture/
    README.md                        # architecture index with role labels
    architecture-baseline.md         # current architecture baseline
    stage-5/                         # historical conceptual architecture artifacts
    stage-6/                         # contract artifacts and reviews
    stage-7/                         # historical Stage 7 review artifacts

  decisions/
    README.md                        # ADR index and decision taxonomy

  development/
    implementation-strategy.md       # process/how-to-work guide
    roadmap.md                       # one future reference, possibly merged with milestones

  prompts/
    task-template.md
    review-template.md
    codex-rules.md                   # optional short cross-reference, not duplicate AGENTS

  guides/
    documentation-style-guide.md     # current style/process guidance

  reviews/
    README.md                        # future index by role/status
    *.md                             # audit trail, current gates, superseded plans
```

Принципы:

- один primary roadmap;
- один product baseline;
- один architecture baseline;
- один canonical Codex operational rule source;
- historical docs preserved but labeled;
- reviews indexed and not treated as current backlog;
- development docs clearly future/reference and not implementation backlog.

## 12. Рекомендуемый cleanup plan

### Cleanup 1 — Stage 7 status/navigation sync

Цель: синхронизировать primary roadmap and navigation docs с завершенным Stage 7 restart readiness review.

Кандидаты на изменение:

- `docs/roadmap/roadmap.md`;
- `README.md`;
- `docs/ROADMAP.md`;
- `docs/architecture/README.md`;
- `docs/development/roadmap.md`;
- `docs/development/milestones.md`;
- `docs/development/implementation-strategy.md`.

Не начинать Stage 7.2 в самом cleanup. Исправить только status wording and next-step wording.

### Cleanup 2 — Style guide stale wording cleanup

Цель: обновить `docs/guides/documentation-style-guide.md`, чтобы он больше не говорил, что Stage 6 remains `Planned / not started`, сохранив его как general guidance.

### Cleanup 3 — Reviews index and archival labels

Цель: создать `docs/reviews/README.md` или equivalent index, который классифицирует:

- current gate/recent review;
- historical audit;
- superseded plan;
- correction report.

Это снижает риск, что старые review findings будут прочитаны как active backlog.

### Cleanup 4 — Prompt/governance deduplication

Цель: сделать AGENTS.md canonical и сократить `docs/prompts/codex-rules.md`, одновременно выровняв final report formats in prompt templates and GitHub templates.

### Cleanup 5 — Development docs merge/shortening

Цель: уменьшить overlap between `docs/development/roadmap.md`, `docs/development/milestones.md` and `docs/development/implementation-strategy.md`.

### Cleanup 6 — Product/architecture index role labels

Цель: усилить role labels for historical/superseded files:

- Stage 0-2 product docs;
- combined search artifacts;
- old Stage 7.1 backend skeleton review;
- Stage 5 decision inventory.

### Cleanup 7 — Roadmap readability cleanup

Цель: сделать `docs/roadmap/roadmap.md` короче и удобнее для чтения, не теряя source-of-truth role.

## 13. Риски для Codex

### Риск 1: stale primary roadmap status может заблокировать или исказить Stage 7.2

Primary roadmap still says restart readiness review must be selected and Stage 7 implementation remains blocked until it. Так как readiness review уже существует и passed, Codex может либо отказаться от валидной Stage 7 work, либо создать conflicting final reports.

### Риск 2: old review artifacts могут выглядеть как current instructions

Pre-Stage 6 reviews and old refactoring plan still mention Stage 6 `Planned / not started`. Без review index/archival labels Codex может over-prioritize old statuses.

### Риск 3: development docs выглядят как active backlog

Даже с disclaimers detailed tasks вроде backend skeleton, LLM abstraction, hotel search abstraction, web skeleton, testing, security and Docker can be misread as implementation permission.

### Риск 4: prompt/governance duplication создает rule drift

AGENTS.md, prompts and GitHub templates repeat similar but not identical final report formats and guardrails. Codex будет следовать самому сильному current instruction, но люди и future agents могут видеть inconsistent templates.

### Риск 5: historical product scope может вернуть future features

Stage 0-2 product docs preserve flight/combined context. Current baseline защищает MVP, но задача, которая читает только historical file, может случайно revive flight/combined scope.

### Риск 6: architecture artifacts могут выглядеть более implementation-ready, чем задумано

Stage 5/6 architecture docs достаточно детальны, чтобы подталкивать к implementation choices. Им нужны role labels, reminding readers what is conceptual, contract-level or historical.

## 14. Финальный verdict

### Можно ли сказать, что документация сейчас clean and non-redundant?

Нет. Документация стала заметно лучше после baseline/status cleanup, но она все еще не clean/non-redundant. Есть существенные повторы, stale statuses and unclear role boundaries.

### Требуется ли broad cleanup?

Да. Нужен broad but controlled cleanup: не массовое переписывание и не удаление audit trail, а серия маленьких задач по status sync, role labeling, prompt/governance deduplication, development docs consolidation and roadmap readability.

### Безопасно ли стартовать Stage 7.2 до cleanup?

Не рекомендуется стартовать Stage 7.2 до минимального cleanup по статусам и навигации. Причина: primary roadmap и несколько navigation/development docs все еще говорят, что Stage 7 implementation заблокирована до restart readiness review, хотя review уже проведен. Это создает прямой риск для Codex.

Полный broad cleanup не обязан быть завершен до Stage 7.2, но Cleanup 1 and Cleanup 2 should happen first или должны быть explicitly accounted for in the Stage 7.2 activation task.

### Какая cleanup task должна быть следующей?

Следующая задача: **Stage 7 status/navigation sync cleanup**.

Минимальный scope:

- update stale Stage 7 restart wording in primary roadmap and navigation/development docs;
- preserve hotel-only MVP scope;
- do not activate Stage 7.2 inside the cleanup task unless the task explicitly says so;
- do not change architecture decisions, OpenAPI, ADR, backend/frontend code or product requirements.
