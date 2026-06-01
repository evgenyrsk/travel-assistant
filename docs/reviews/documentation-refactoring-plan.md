# Documentation Refactoring Plan

## Статус документа

Этот документ является плановым артефактом и audit trail для будущего controlled documentation refactoring. Он фиксирует предложенный порядок безопасных documentation cleanup задач после global documentation quality review.

План не является активным backlog, roadmap, task tracker или разрешением на массовую правку документации. Любая фаза или suggested task требует отдельной явной задачи и должна оставаться согласованной с `docs/roadmap/roadmap.md`.

Stage 6 остается в статусе `Planned / not started`. Этот plan не начинает Stage 6, не создает implementation backlog, API/OpenAPI contracts, DB schema/storage model, auth/security/DevOps/testing backlog или production implementation.

## 1. Plan Context

Этот plan создан после `docs/reviews/global-documentation-quality-review.md`, где итоговый verdict: Documentation needs controlled refactoring before Stage 6.

Refactoring нужен не потому, что baseline сломан, а потому что документация стала тяжелой для чтения: actual baseline распределен по разным документам, stage artifacts смешиваются с historical context, roadmap перегружен, development docs выглядят как backlog будущей реализации, guardrails дублируются, а русский и английский язык смешаны сильнее, чем нужно.

Refactoring должен быть controlled: маленькими задачами, без изменения смысла, без изменения product requirements и architecture decisions, без старта Stage 6.

На момент plan:

- Stage 0 - Completed;
- Stage 1 - Completed;
- Stage 2 - Completed;
- Stage 3 - Completed;
- Stage 4 - Completed;
- Stage 4.1 - Completed;
- Stage 5 - Completed;
- Stage 6 - Planned / not started;
- Реализация Code/API/DB/UI - Not started.

## 2. Refactoring Goals

Цели controlled refactoring:

- повысить читаемость документации;
- создать компактный actual baseline;
- уменьшить неоправданное смешение русского и английского;
- облегчить primary roadmap;
- отделить historical artifacts от actual baseline;
- централизовать guardrails;
- улучшить navigation и ясность source of truth;
- сохранить product/architecture meaning;
- защитить hotel-only MVP v1 scope;
- сохранить Stage 6 в статусе Planned / not started до отдельной явной задачи.

## 3. Non-Goals

Этот plan не предлагает:

- менять product requirements;
- менять architecture decisions;
- начинать Stage 6;
- менять статус Stage 6 на In Progress;
- создавать Stage 6 deliverables;
- создавать API/OpenAPI contracts;
- создавать DB schema/storage model;
- создавать auth/security/DevOps/testing backlog;
- создавать implementation backlog;
- добавлять production code;
- удалять historical audit trail;
- расширять MVP;
- возвращать flights, combined itinerary, booking или payment в MVP;
- превращать future stages в активный backlog;
- превращать future ADR candidates в accepted ADR.

## 4. Proposed Target Documentation Structure

Это целевая структура для будущего refactoring. В рамках этого plan файлы не перемещаются.

| Section | Intended Role | Expected Files | Notes |
|---|---|---|---|
| Root entry points | Быстрый вход в проект и маршруты чтения. | `README.md`, `AGENTS.md`, `docs/PROJECT_BRIEF.md`, `docs/ARCHITECTURE.md` | README должен быть коротким entry point. `docs/ARCHITECTURE.md` стоит явно маркировать как preliminary/historical после Stage 5. |
| Roadmap and governance | Source of truth по stage status/progression и governance. | `docs/roadmap/roadmap.md`, `docs/ROADMAP.md`, `docs/guides/documentation-style-guide.md` | Primary roadmap не должен становиться backlog или архивом всех stage details. |
| Product baseline | Compact current MVP/product/UX baseline. | Proposed: `docs/product/product-baseline.md`; existing: `docs/product/README.md`, Stage 3/4 summaries | Baseline должен фиксировать hotel-only MVP v1 без переписывания historical docs. |
| Product stage artifacts | Historical deliverables и traceability этапов. | `docs/product/stage-0/*` - `docs/product/stage-4/*` | Сохранять; маркировать как historical/stage artifacts при необходимости. |
| Architecture baseline | Compact current architecture baseline. | Proposed: `docs/architecture/architecture-baseline.md`; existing: `docs/architecture/README.md`, `docs/architecture/stage-5/*` | Baseline должен отделять conceptual architecture от future implementation. |
| Architecture stage artifacts | Stage 5 architecture deliverables. | `docs/architecture/stage-5/*` | Сохранять как Stage 5 artifacts; не превращать в implementation backlog. |
| Decisions / ADR | Accepted ADRs, drafts, candidates и inventory. | `docs/decisions/README.md`, future ADR files, `docs/architecture/stage-5/architecture-decisions-draft.md` | Accepted ADRs должны быть отдельно от candidates и non-ADR inventory. |
| Reviews | Quality gates и audit trail. | `docs/reviews/*`, stage consistency reviews | Proposed: optional `docs/reviews/README.md` later. Reviews не заменяют roadmap. |
| Development reference | Справочные implementation materials для будущей реализации. | `docs/development/roadmap.md`, `docs/development/milestones.md`, `docs/development/implementation-strategy.md` | Должны явно оставаться неактивными до отдельной roadmap activation. |
| Prompts / agent rules | Шаблоны задач и правила AI/code agents. | `docs/prompts/*`, `.github/*`, `AGENTS.md` | Должны следовать primary roadmap и style guide. |

## 5. Actual Baseline Layer Proposal

Предлагаемый baseline layer:

- `docs/roadmap/roadmap.md` - stage/status baseline и primary source of truth по progression.
- `docs/product/product-baseline.md` - compact current product/UX baseline: hotel-only MVP v1, active user flows, MVP exclusions, facts/assumptions/unknowns, current-session shortlist.
- `docs/architecture/architecture-baseline.md` - compact current architecture baseline: conceptual boundaries, provider/LLM responsibilities, no API/DB/storage implementation, deferred decisions.
- `docs/decisions/README.md` - decision baseline/index: accepted ADRs, drafts, candidates, non-ADR inventory.
- `docs/guides/documentation-style-guide.md` - style/process baseline для документации.

Эти файлы являются proposal. В рамках текущей задачи создается только style guide и этот plan. `docs/product/product-baseline.md` и `docs/architecture/architecture-baseline.md` не создаются сейчас.

## 6. Refactoring Phases

### Phase 1 — Style Guide and Navigation Links

- Создать style guide.
- Создать refactoring plan.
- Позже добавить ссылки на style guide и plan в README/roadmap/index files.
- Не менять смысл существующих документов.
- Не начинать Stage 6.

### Phase 2 — Actual Baseline Layer

- Создать или усилить compact baseline docs.
- Не переписывать stage artifacts.
- Зафиксировать актуальный MVP/architecture status.
- Явно отделить current baseline от historical stage artifacts.

### Phase 3 — Roadmap Readability Cleanup

- Облегчить roadmap.
- Оставить status, governance, activation rules, quality gates и carryover.
- Вынести подробности в baseline/stage docs через ссылки.
- Не превращать roadmap в task tracker.

### Phase 4 — Product Docs Cleanup

- Отделить active MVP baseline от historical artifacts.
- Привести связующий текст к русскому стилю.
- Сохранить audit trail и superseded context.
- Не возвращать flights, combined itinerary, booking или payment в MVP.

### Phase 5 — Architecture Docs Cleanup

- Усилить architecture baseline entry point.
- Отделить conceptual architecture от future implementation.
- Сохранить provider facts / LLM boundary.
- Не создавать OpenAPI/API contracts, DB schema/storage model или provider-specific adapter design.

### Phase 6 — Decisions / ADR Cleanup

- Разделить accepted ADRs, drafts, candidates и non-ADR decision inventory.
- Уточнить wording `ADR Candidate` и `Confirmed` так, чтобы candidates не выглядели accepted ADR.
- Не принимать новые ADR без отдельной задачи.

### Phase 7 — Development Docs and Prompts Cleanup

- Улучшить future/reference framing development docs.
- Синхронизировать prompts, GitHub templates, AGENTS.md и style guide.
- Убедиться, что development docs не выглядят как активный implementation backlog.

## 7. Recommended Before Stage 6

Минимальный набор до Stage 6:

- style guide;
- compact baseline layer proposal или первые baseline docs;
- roadmap readability cleanup;
- product baseline entry point;
- architecture baseline entry point;
- navigation cleanup;
- decisions/ADR terminology cleanup;
- prompts/AGENTS.md alignment.

Не нужно делать все за один шаг. Безопасный путь - 3-5 маленьких controlled documentation tasks, каждая с явными allowed files и forbidden changes.

## 8. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Потеря смысла при сокращении текста. | Выполнять refactoring маленькими задачами; не менять requirements/decisions; сохранять links to source artifacts. |
| Случайное изменение MVP scope. | Держать hotel-only MVP boundary в primary roadmap и baseline docs; явно запрещать flights/combined/booking/payment в MVP. |
| Потеря audit trail. | Не удалять historical stage artifacts и reviews; маркировать их role labels вместо удаления. |
| Future docs превращаются в backlog. | В development docs использовать future/reference framing и ссылки на primary roadmap activation. |
| Чрезмерное сокращение guardrails. | Централизовать canonical guardrails и оставлять короткие локальные reminders. |
| Сломанные ссылки. | После каждого navigation cleanup запускать проверку `rg`/ручную проверку ссылок по измененным files. |
| Stage 6 started accidentally. | Не создавать Stage 6 deliverables; не менять Stage 6 status; все next steps оставлять recommendations, not execution. |

## 9. Suggested Execution Order

### Task 1 — Add Navigation Links for Documentation Governance

Goal:
Добавить discoverability для style guide и refactoring plan.

Allowed files:

- `README.md`
- `docs/roadmap/roadmap.md`
- relevant index docs only if needed

Forbidden changes:

- менять roadmap status;
- менять product/architecture content;
- выполнять refactoring;
- начинать Stage 6.

Expected output:
Минимальные ссылки на `docs/guides/documentation-style-guide.md` и `docs/reviews/documentation-refactoring-plan.md`.

### Task 2 — Create Product Baseline

Goal:
Создать compact current product/UX baseline.

Allowed files:

- proposed `docs/product/product-baseline.md`
- `docs/product/README.md`
- `README.md`, только если нужны navigation changes

Forbidden changes:

- переписывать stage docs;
- менять MVP scope;
- возвращать flights/combined/booking/payment в MVP;
- удалять historical context.

Expected output:
Короткий current baseline для hotel-only MVP v1 с links to stage artifacts.

### Task 3 — Create Architecture Baseline

Goal:
Создать compact current architecture baseline.

Allowed files:

- proposed `docs/architecture/architecture-baseline.md`
- `docs/architecture/README.md`
- `README.md`, только если нужны navigation changes

Forbidden changes:

- менять architecture decisions;
- создавать API/OpenAPI contracts;
- создавать DB schema/storage model;
- превращать provider abstraction в API contract;
- начинать implementation planning.

Expected output:
Короткий conceptual architecture baseline с links to Stage 5 artifacts.

### Task 4 — Roadmap Readability Cleanup

Goal:
Облегчить primary roadmap без изменения смысла.

Allowed files:

- `docs/roadmap/roadmap.md`
- `docs/ROADMAP.md` only if needed for navigation consistency

Forbidden changes:

- менять stage statuses;
- менять roadmap order;
- добавлять активный backlog;
- удалять governance;
- начинать Stage 6.

Expected output:
Более читаемый roadmap: current status, stage map, activation rules, links to baseline/stage docs.

### Task 5 — Decisions / ADR Terminology Cleanup

Goal:
Уточнить разделение accepted ADRs, drafts, candidates и non-ADR inventory.

Allowed files:

- `docs/decisions/README.md`
- `docs/architecture/stage-5/architecture-decisions-draft.md`

Forbidden changes:

- создавать accepted ADR без отдельной задачи;
- менять architecture decisions;
- превращать candidates в accepted decisions.

Expected output:
Ясная terminology и role labels для decision materials.

### Task 6 — Development Docs and Prompts Alignment

Goal:
Синхронизировать development docs, prompts и AGENTS.md со style guide.

Allowed files:

- `docs/development/roadmap.md`
- `docs/development/milestones.md`
- `docs/development/implementation-strategy.md`
- `docs/prompts/*`
- `.github/*`
- `AGENTS.md`

Forbidden changes:

- создавать implementation backlog;
- активировать Stage 6;
- менять product/architecture scope;
- добавлять code/tooling.

Expected output:
Development docs оформлены как справочные материалы для будущей реализации; prompts следуют primary roadmap и style guide.

## 10. Final Recommendation

Controlled documentation refactoring нужен before Stage 6, но его нельзя выполнять одним большим шагом.

Минимальный safe path:

1. Зафиксировать style guide и refactoring plan.
2. Добавить navigation links.
3. Создать compact product и architecture baseline entry points.
4. После этого облегчить roadmap и синхронизировать decisions/development/prompts.

Нельзя начинать с массового переписывания stage docs, перемещения файлов или сокращения guardrails во всех местах сразу. Такой подход повышает риск потерять traceability, изменить scope или случайно начать Stage 6.
